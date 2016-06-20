/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indextranslator;

import indexer.TextDocIndexer;
import java.util.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import retriever.PayloadAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;


/**
 * Input: A mono-lingual index
 * Output: A translated index using word translation probabilities
 * 
 * @author dganguly
 */
public class BOWTranslator {
    Properties prop;
    String inIndexPath;
    String outIndexPath;
    IndexWriter writer;
    IndexReader reader;
    Dictionary dict;
    
    public BOWTranslator(String propfile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propfile));
        
        inIndexPath = prop.getProperty("index");
        outIndexPath = prop.getProperty("translated.index");
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(new PayloadAnalyzer());
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(FSDirectory.open(new File(outIndexPath).toPath()), iwcfg);                
        reader = DirectoryReader.open(FSDirectory.open(new File(inIndexPath).toPath()));
        
        // Load the dict in memory
        dict = new Dictionary(
                Integer.parseInt(prop.getProperty("numtranslated_words", "3")),                
                Float.parseFloat(prop.getProperty("translation.threshold_weight", "0.01"))
        );
        dict.load(prop.getProperty("dict"));
    }
    
    public void translateAll() throws Exception {
        final int numDocs = reader.numDocs();
        String docId;
        
        int startDocId = Integer.parseInt(prop.getProperty("source.startdocid", "0"));
        int endDocId = Integer.parseInt(prop.getProperty("source.enddocid", String.valueOf(numDocs)));
        
        for (int i = startDocId; i < endDocId; i++) {
            Document doc = reader.document(i);
            docId = doc.get(TextDocIndexer.FIELD_ID);
            System.out.println("Translating doc: " + docId);
            translate(docId, i);            
        }
        
        writer.close();
        reader.close();
    }
    
    public void translate(String docIdStr, int docId) throws Exception {
        String termText;
        BytesRef term;
        Terms tfvector;
        TermsEnum termsEnum;
        int tf;
                
        tfvector = reader.getTermVector(docId, TextDocIndexer.FIELD_ANALYZED_CONTENT);
        if (tfvector == null || tfvector.size() == 0)
            return;

        // Construct the normalized tf vector
        termsEnum = tfvector.iterator(); // access the terms for this field
        StringBuffer buff = new StringBuffer();
        
    	while ((term = termsEnum.next()) != null) { // explore the terms for this field
            tf = (int)termsEnum.totalTermFreq();            
            termText = term.utf8ToString();                        
            buff.append(dict.getTranslations(termText, tf)).append("\n");
        }
        
        Document doc = constructDoc(docIdStr, buff.toString());
        writer.addDocument(doc);
    }
    
    Document constructDoc(String id, String line) throws Exception {        
        Document doc = new Document();
        doc.add(new Field(TextDocIndexer.FIELD_ID, id, Field.Store.YES, Field.Index.NOT_ANALYZED));        
        doc.add(new Field(TextDocIndexer.FIELD_ANALYZED_CONTENT, line,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
        return doc;        
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java BOWTranslator <prop-file>");
            args[0] = "init.properties";
        }
        
        BOWTranslator bowt;
        try {
            bowt = new BOWTranslator(args[0]);
            bowt.translateAll();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
}
