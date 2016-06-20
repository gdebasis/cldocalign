/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import evaluator.Evaluator;
import indexer.TextDocIndexer;
import indextranslator.Dictionary;
import indextranslator.TranslationInfo;
import indextranslator.Translations;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopDocsCollector;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
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
    
    TermStats(String term, int tf, float wt) {
        this.term = term;
        this.tf = tf;
        this.wt = wt;        
    }
    
    TermStats(String term, int tf, IndexReader reader) throws Exception {
        this.term = term;
        this.tf = tf;
        idf = (float)(
                Math.log(reader.numDocs()/
                (float)(reader.docFreq(new Term(TextDocIndexer.FIELD_ANALYZED_CONTENT, term)))));
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
    
    boolean isNumerical(String str) {
        for (char c : str.toCharArray()) {
            if (Character.isDigit(c))
                return true;
        }
        return false;
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
            if (isNumerical(termText))
                continue;
            
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
    IndexSearcher frIndexSearcher;
    FileWriter fw;
    
    boolean queryTranslation;
    Dictionary dict;
    int shift;
    String prefix;
            
    public CrossLingualAligner(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        
        queryTranslation = Boolean.parseBoolean(prop.getProperty("qry.translation", "true"));
                
        String enIndexPath = prop.getProperty("index"); // query index
        String frIndexPath = prop.getProperty("translated.index"); // search index
        
        enIndexReader = DirectoryReader.open(FSDirectory.open(new File(enIndexPath).toPath()));
        frIndexReader = DirectoryReader.open(FSDirectory.open(new File(frIndexPath).toPath()));
        frIndexSearcher = new IndexSearcher(frIndexReader);

        // Get the prefix
        prefix = frIndexReader.document(0)
                            .get(TextDocIndexer.FIELD_ID)
                            .substring(0, 3);
        
        if (queryTranslation) {            
            dict = new Dictionary(
                    Integer.parseInt(prop.getProperty("numtranslated_words", "3")),
                    Float.parseFloat(prop.getProperty("translation.threshold_weight", "0.01"))
            );
            System.out.println("Loading dict for query tranalation...");
            dict.load(prop.getProperty("dict"));            
        }
        shift = Integer.parseInt(prop.getProperty("retrieve.temporal_search_window", "10"));
    }
    
    IndexSearcher buildTemporalIndexSearcher(IndexReader reader) throws Exception {
        
        IndexSearcher searcher = new IndexSearcher(reader);
        
        if (queryTranslation)
            //searcher.setSimilarity(new BM25Similarity());
            //searcher.setSimilarity(new LMDirichletSimilarity());
            searcher.setSimilarity(new LMJelinekMercerSimilarity(0.4f));            
        else
            searcher.setSimilarity(new BM25PayloadSimilarity(1.2f, 0.75f));
        
        return searcher;        
    }
    
    public void alignAll() throws Exception {
        
        fw = new FileWriter(prop.getProperty("out.align.file"));
        final int numDocs = enIndexReader.numDocs();
        
        int startDocId = Integer.parseInt(prop.getProperty("source.startdocid", "0"));
        int endDocId = Integer.parseInt(prop.getProperty("source.enddocid", String.valueOf(numDocs)));
        
        for (int i = startDocId; i < endDocId; i++) {
            Document doc = enIndexReader.document(i);
            
            String docId = doc.get(TextDocIndexer.FIELD_ID);            
            String alignedDocId = align(i);
            if (alignedDocId == null)
                continue;
            
            String line = docId + "\t" + alignedDocId;
            System.out.println(line);
            fw.write(line + "\n");
            fw.flush();
        }
        
        fw.close();
        
        enIndexReader.close();
        frIndexReader.close();
    }

    List<TermStats> getTranslatedQueryTerms(TermStats termStats) throws Exception {
        List<TermStats> qterms = new ArrayList<>();
        
        Translations translatedTerms = dict.getTranslationTerms(termStats.term);
        List<TranslationInfo> tlist = translatedTerms.getTranslationInfo();
        
        for (TranslationInfo tinfo : tlist) {
            TermStats ts = new TermStats(tinfo.word, termStats.tf, tinfo.weight);
            qterms.add(ts);
        }
                
        return qterms;
    }
    
    Query constructQuery(int docId) throws Exception {
        BooleanQuery q = new BooleanQuery();

        DocStats docStats = new DocStats(this, docId);
        List<TermStats> termsStatsList = docStats.build();
        
        if (termsStatsList == null)
            return null;
        
        for (TermStats ts : termsStatsList) {        
            
            Term queryTerm = new Term(TextDocIndexer.FIELD_ANALYZED_CONTENT, ts.term);
            TermQuery tq = new TermQuery(queryTerm);
            tq.setBoost(ts.ntf);
            q.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));            
        }
        
        return q;        
    }
    
    
    Query constructTranslatedQuery(int docId) throws Exception {
        HashMap<String, TermStats> qmap = new HashMap<>();
        BooleanQuery q = new BooleanQuery();

        DocStats docStats = new DocStats(this, docId);
        List<TermStats> termsStatsList = docStats.build();
        
        if (termsStatsList == null)
            return null;
        
        for (TermStats ts : termsStatsList) {                    
            List<TermStats> qterms = getTranslatedQueryTerms(ts);
            
            for (TermStats translatedTermStats : qterms) {
                TermStats seen = qmap.get(translatedTermStats.term);
                if (seen == null) {
                    seen = new TermStats(translatedTermStats.term, translatedTermStats.tf, translatedTermStats.wt);
                    qmap.put(translatedTermStats.term, seen);
                }
                seen.tf += translatedTermStats.tf;                
            }
        }
        
        for (Map.Entry<String, TermStats> e : qmap.entrySet()) {            
            TermStats ts = e.getValue();
            Term queryTerm = new Term(TextDocIndexer.FIELD_ANALYZED_CONTENT, ts.term);
            TermQuery tq = new TermQuery(queryTerm);
            tq.setBoost(ts.tf * ts.wt);
            q.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
        }
        
        return q;        
    }
    
    String extractDate(String docName) {
        int startDatePos = docName.indexOf('.');
        int endDatePos = docName.indexOf('.', startDatePos+1);
        String date = docName.substring(startDatePos+1, endDatePos);        
        return date;
    }
    
    String getShiftedDate(String refDate, int shiftDays) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        Calendar c = Calendar.getInstance();        
        c.setTime(sdf.parse(refDate));                
        c.add(Calendar.DATE, shiftDays);  // number of days to add
        String dt = sdf.format(c.getTime());  // dt is now the new date
        return dt;        
    }
    
    BooleanQuery constructDateQuery(String refDate) throws Exception {
        BooleanQuery q = new BooleanQuery();
        
        // Form a boolean OR query with all the dates in range...
        if (shift == 0) {
            PrefixQuery tq = new PrefixQuery(
                    new Term(
                        TextDocIndexer.FIELD_ID,
                        prefix + refDate)
            );
            q.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));            
        }
        
        else {
            for (int i = -shift; i <= shift; i++) {
                PrefixQuery tq = new PrefixQuery(
                        new Term(
                            TextDocIndexer.FIELD_ID,
                            prefix + getShiftedDate(refDate, i)));
                q.add(new BooleanClause(tq, BooleanClause.Occur.SHOULD));
            }
        }
        
        return q;
    }
    
    void getSubsetToSearch(int refDocId, IndexWriter writer) throws Exception {
        String srcDocName = enIndexReader.document(refDocId).get(TextDocIndexer.FIELD_ID);
        String refDate = extractDate(srcDocName);
        int perDay = Integer.parseInt(prop.getProperty("retrieve.max_per_day", "50"));
        
        // construct a range query
        BooleanQuery dateWildCardQuery = constructDateQuery(refDate);
        System.out.println(dateWildCardQuery);

        int numDatesInRange = ((shift<<1)+1)*perDay;
        TopDocsCollector collector = TopScoreDocCollector.create(numDatesInRange);
        
        frIndexSearcher.search(dateWildCardQuery, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;
        for (ScoreDoc hit : hits) {
            writer.addDocument(frIndexReader.document(hit.doc));
        }        
    }
    
    // Build temporal in-mem index to restrict search
    Directory buildTemporalIndex(int refDocId) throws Exception {
        Directory ramdir = new RAMDirectory();
        IndexWriterConfig iwcfg = new IndexWriterConfig(new StandardAnalyzer());
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);
        
        // Get all documents from the current month of the year
        getSubsetToSearch(refDocId, writer);
        
        writer.commit();
        writer.close();
        return writer.getDirectory();        
    }
    
    // Returns the doc-id of the aligned doc
    String align(int docId) throws Exception {
        
        TopScoreDocCollector collector;
        TopDocs topDocs;
        
        Query q = queryTranslation? constructTranslatedQuery(docId) : constructQuery(docId);
        if (q == null)
            return null;
        
        System.out.println("Querying with: " + q);
        
        Directory inMemTemporalIndex = buildTemporalIndex(docId);
        IndexReader ramDirReader = DirectoryReader.open(inMemTemporalIndex);
        IndexSearcher searcher = buildTemporalIndexSearcher(ramDirReader);
        
        collector = TopScoreDocCollector.create(1);
        searcher.search(q, collector);
        
        topDocs = collector.topDocs();
        ScoreDoc[] hits = topDocs.scoreDocs;
        
        if (hits.length == 0) {
            ramDirReader.close();
            inMemTemporalIndex.close();
            return null;
        }
        
        Document alignedDoc = ramDirReader.document(hits[0].doc);
        String alignedDocId = alignedDoc.get(TextDocIndexer.FIELD_ID);
        
        ramDirReader.close();
        inMemTemporalIndex.close();
                
        return alignedDocId;
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
            Evaluator eval = new Evaluator(args[0]);
            eval.evaluate();
        }
        catch (Exception ex) { ex.printStackTrace(); }
        
    }
}
