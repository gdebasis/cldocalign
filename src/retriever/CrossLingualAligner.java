/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import indexer.TextDocIndexer;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dganguly
 */

class TermStats implements Comparable<TermStats> {
    String term;
    int tf;
    float ntf;
    float idf;
    float wt;
    
    TermStats(String term, int tf, IndexReader reader) throws Exception {
        this.term = term;
        this.tf = tf;
        idf = (float)(reader.docFreq(new Term(TextDocIndexer.FIELD_ANALYZED_CONTENT, term)));
    }
    
    void computeWeight(int docLen, float lambda) {
        ntf = tf/(float)docLen;
        wt = lambda*ntf + (1-lambda)*idf;
    }

    @Override
    public int compareTo(TermStats that) {
        return -1*Float.compare(this.wt, that.wt); // descending
    }
    
}

class DocStats {
    List<TermStats> wordvec;
    int docId;
    Terms tfvector;
    IndexReader reader;
    int numTopTerms;
    float qSelLambda;
    
    DocStats(CrossLingualAligner aligner, int docId) {
        this.docId = docId;
        this.reader = aligner.enIndexReader;
        wordvec = new ArrayList<>();
        
        numTopTerms = Integer.parseInt(aligner.prop.getProperty("querysel.ntopterms", "20"));
        qSelLambda = Float.parseFloat(aligner.prop.getProperty("querysel.lambda", "0.4"));
    }
    
    public List<TermStats> build() throws Exception {
        String termText;
        BytesRef term;
        Terms tfvector;
        TermsEnum termsEnum;
        int docLen = 0;
        int tf;
        
        tfvector = reader.getTermVector(docId, TextDocIndexer.FIELD_ANALYZED_CONTENT);
        if (tfvector == null || tfvector.size() == 0)
            return null;

        // Construct the normalized tf vector
        termsEnum = tfvector.iterator(); // access the terms for this field
        
    	while ((term = termsEnum.next()) != null) { // explore the terms for this field            
            tf = (int)termsEnum.totalTermFreq();
            termText = term.utf8ToString();
            
            wordvec.add(new TermStats(termText, tf, reader));
            docLen += tf;
        }
        
        for (TermStats ts : wordvec) {
            ts.computeWeight(docLen, qSelLambda);
        }
        
        Collections.sort(wordvec);
        return wordvec.subList(0, Math.min(numTopTerms, wordvec.size()));        
    }
    
}

public class CrossLingualAligner {
    Properties prop;

    IndexReader enIndexReader;
    IndexReader frIndexReader;
    IndexSearcher enFrSearcher;
    FileWriter fw;
            
    public CrossLingualAligner(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
                
        String enIndexPath = prop.getProperty("index");
        String frIndexPath = prop.getProperty("translated.index");
        
        enIndexReader = DirectoryReader.open(FSDirectory.open(new File(enIndexPath).toPath()));
        frIndexReader = DirectoryReader.open(FSDirectory.open(new File(frIndexPath).toPath()));
        enFrSearcher = new IndexSearcher(frIndexReader);
        enFrSearcher.setSimilarity(new BM25PayloadSimilarity(1.2f, 0.75f));
    }
    
    public void alignAll() throws Exception {
        
        fw = new FileWriter(prop.getProperty("out.align.file"));
        final int numDocs = enIndexReader.numDocs();
        
        for (int i = 0; i < numDocs; i++) {
            Document doc = enIndexReader.document(i);
            
            String docId = doc.get(TextDocIndexer.FIELD_ID);            
            String alignedDocId = align(i);
            
            String line = docId + "\t" + alignedDocId;
            System.out.println(line);
            fw.write(line + "\n");
        }
        
        fw.close();
        
        enIndexReader.close();
        frIndexReader.close();
    }

    Query constructQuery(int docId) throws Exception {
        BooleanQuery q = new BooleanQuery();
        TermQuery tq;

        DocStats docStats = new DocStats(this, docId);
        List<TermStats> termsStatsList = docStats.build();
        
        for (TermStats ts : termsStatsList) {        
            Term queryTerm = new Term(TextDocIndexer.FIELD_ANALYZED_CONTENT, ts.term);
            tq = new TermQuery(queryTerm);
            tq.setBoost(ts.ntf);
            q.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));            
        }
        
        return q;        
    }
    
    // Returns the doc-id of the aligned doc
    String align(int docId) throws Exception {
        
        TopScoreDocCollector collector;
        TopDocs topDocs;
        
        Query q = constructQuery(docId);
        
        System.out.println("Querying with: " + q);
        
        collector = TopScoreDocCollector.create(1);
        this.enFrSearcher.search(q, collector);
        
        topDocs = collector.topDocs();
        ScoreDoc[] hits = topDocs.scoreDocs;
        
        Document alignedDoc = frIndexReader.document(hits[0].doc);
        return alignedDoc.get(TextDocIndexer.FIELD_ID);
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java TrecDocIndexer <prop-file>");
            args[0] = "init.properties";
        }
        
        try {
            CrossLingualAligner aligner = new CrossLingualAligner(args[0]);
            aligner.alignAll();            
        }
        catch (Exception ex) { ex.printStackTrace(); }
        
    }
}
