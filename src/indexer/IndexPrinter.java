/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author dganguly
 */
public class IndexPrinter {
    Properties prop;
    IndexReader reader;
    FileWriter fw;
    
    IndexPrinter(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        String indexPath = prop.getProperty("index"); // query index
        reader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));        
        String outFilePath = prop.getProperty("index.dumpfile", "indexdump.txt"); // query index
        fw = new FileWriter(outFilePath);
    }
    
    String getAnalyzedContent(String content) throws IOException {
        StringBuffer tokenizedContentBuff = new StringBuffer();
        Analyzer analyzer = new StandardAnalyzer();
        TokenStream stream = analyzer.tokenStream(TextDocIndexer.FIELD_ANALYZED_CONTENT, new StringReader(content));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokenizedContentBuff.append(term).append(" ");
        }
        tokenizedContentBuff.append("\n");
        return tokenizedContentBuff.toString();
    }
    
    String getContent(String docId) throws Exception {
        
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(1);
        TermQuery tq = new TermQuery(new Term(TextDocIndexer.FIELD_ID, docId));
        searcher.search(tq, collector);
        
        int retrievedDocId = collector.topDocs().scoreDocs[0].doc;
        Document retrievedDoc = reader.document(retrievedDocId);
        return retrievedDoc.get(TextDocIndexer.FIELD_ANALYZED_CONTENT);        
    }

    String getContent(int docId) throws Exception {        
        Document retrievedDoc = reader.document(docId);
        return getAnalyzedContent(retrievedDoc.get(TextDocIndexer.FIELD_ANALYZED_CONTENT));
    }
    
    void processAll() throws Exception {
        int numDocs = reader.numDocs();
        for (int i=0; i<numDocs; i++) {
            fw.write(getContent(i));
        }
    }
    
    void close() throws Exception {
        reader.close();
        fw.close();
    }
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java IndexPrinter <prop-file>");
            args[0] = "init.properties";
        }

        try {
            IndexPrinter p = new IndexPrinter(args[0]);
            p.processAll();
            p.close();            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }    
}

