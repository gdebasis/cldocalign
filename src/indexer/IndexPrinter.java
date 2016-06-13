/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
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
    IndexReader reader;
    
    IndexPrinter(String indexDir) throws Exception {
        reader = DirectoryReader.open(FSDirectory.open(new File(indexDir).toPath()));        
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
    
    void close() throws Exception { reader.close(); }
    
    public static void main(String[] args) throws Exception {
        IndexPrinter p = new IndexPrinter("/mnt/sdb2/research/DocumentAligner/index");
        System.out.println(p.getContent("fr.20141017.31.txt"));        
        p.close();
    }    
}
