/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vectorizer;

import indexer.TextDocIndexer;
import indextranslator.Dictionary;
import indextranslator.TranslationInfo;
import indextranslator.Translations;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.deeplearning4j.datasets.fetchers.BaseDataFetcher;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

/**
 *
 * @author Debasis
 */

class TermInfo implements Comparable<TermInfo> {
    String term;
    int id;
    int tf;
    float idf;
    
    public TermInfo(String term, int tf, int id) {
        this.tf = tf;
        this.term = term;
        this.id = id;
    }
    
    public TermInfo(String term, int tf, int id, float idf) {
        this.tf = tf;
        this.term = term;
        this.id = id;
        this.idf = idf;
    }
    
    @Override
    public int compareTo(TermInfo that) {  
        return -1 * Float.compare(this.tf, that.tf); // descending
    }
}

class DocVector {
    String docName;
    ArrayList<TermInfo> vec;
    float docLen;
    
    public DocVector(String docName) {
        this.docName = docName;
        this.vec = new ArrayList<>();
    }
    
    void addTermInfo(TermInfo tinfo) {
        vec.add(tinfo);
    }
    
    float getDocLen() {
        if (docLen > 0)
            return docLen;
        
        for (TermInfo ti : vec) {
            docLen += ti.tf;
        }
        return docLen;
    }
}

// Fetch the document text from an existing Lucene index
public class LuceneDocFetcher extends BaseDataFetcher {
    DataSet dataSet;
    int globalTermId;
    Map<String, Integer> termSeen;
    Map<String, TermInfo> collFreq;    
    ArrayList<DocVector> docWordMaps;
    List<TermInfo> topTerms;
    
    public static final String ID_FIELD_NAME = TextDocIndexer.FIELD_ID;
    public static final String CONTENT_FIELD_NAME = TextDocIndexer.FIELD_ANALYZED_CONTENT;
        
    public LuceneDocFetcher() throws Exception {
        globalTermId = 0;
        termSeen = new HashMap<>();
        collFreq = new HashMap<>();        
        docWordMaps = new ArrayList<>();
    }
    
    protected int getTermId(String term) {
        if (termSeen.containsKey(term))
            return termSeen.get(term);
        int termId = globalTermId++;
        termSeen.put(term, termId);
        return termId;
    }

    public DataSet getDataSet() { return dataSet; }
    
    String getDocName(int docId) throws Exception {
        return docWordMaps.get(docId).docName;        
    }

    private DocVector buildTerms(IndexReader reader, int docId, int numDocs, Dictionary dict) throws Exception {
        DocVector wmap = new DocVector(reader.document(docId).get(ID_FIELD_NAME));
        Terms tfvector;
        TermsEnum termsEnum;
        String termText;
        BytesRef term;
        int tf;
        float idf;
        
        tfvector = reader.getTermVector(docId, CONTENT_FIELD_NAME);
        
        if (tfvector == null)
            return null;
        
        // Construct the normalized tf vector
        termsEnum = tfvector.iterator(); // access the terms for this field
        
    	while ((term = termsEnum.next()) != null) { // explore the terms for this field
            tf = (int)termsEnum.totalTermFreq();
            termText = term.utf8ToString();
            
            float df = reader.docFreq(new Term(CONTENT_FIELD_NAME, termText));
            idf = (float)Math.log(1+numDocs/df);
            
            TermInfo termInfo = new TermInfo(termText, tf, getTermId(termText), idf);
            if (dict != null) {
                Translations translations = dict.getTranslationTerms(termText);
                for (TranslationInfo tinfo : translations.getTranslationInfo()) {
                    termInfo.tf *= tinfo.weight;
                }
            }

            // Update global stats
            TermInfo seenTermInfo = collFreq.get(termText);
            if (seenTermInfo == null) {
                seenTermInfo = new TermInfo(termInfo.term, termInfo.tf, termInfo.id, termInfo.idf);
                collFreq.put(termText, seenTermInfo);
            }
            else {
                seenTermInfo.tf += termInfo.tf; // coll freq
            }
            
            wmap.addTermInfo(termInfo);            
        }
        
        return wmap;
    }
    
    private DataSet constructTermVector(ArrayList<TermInfo> docWordMap, float doclen, byte relLabel) {
        INDArray onehotDocVec = Nd4j.create(1, globalTermId);
        INDArray labels = Nd4j.create(1, 1);
        labels.putScalar(0, relLabel);
        
        for (TermInfo termInfo : docWordMap) {
            onehotDocVec.putScalar(termInfo.id-1, (termInfo.tf/doclen)*termInfo.idf); // set present terms as 1, rest all 0s
        }
        
        return new DataSet(onehotDocVec, labels);
    }

    public void loadDcuments(Directory dir, Dictionary dict) throws Exception {
        IndexReader reader = DirectoryReader.open(dir);        
        //int numDocs = Math.min(reader.numDocs(), 1000);
        int numDocs = reader.numDocs();
        
        // build the per-doc word maps
        for (int i = 0; i < numDocs; i++) {             
            System.out.println("Loading term vector of document: " + i);
            DocVector dvector = buildTerms(reader, i, numDocs, dict);
            if (dvector != null)
                docWordMaps.add(dvector);
        }
        reader.close();
    }
    
    public void loadDcuments(Directory dir) throws Exception {
        loadDcuments(dir, null);
    }
    
    public void buildVecs() throws Exception {
        // iterate through the word maps and build the one-hot vectors
        List<DataSet> allDocVecs = new ArrayList<>(docWordMaps.size());
        for (DocVector docwordMap : docWordMaps) {
            if (docwordMap == null) {
                continue;
            }
            allDocVecs.add(constructTermVector(docwordMap.vec, docwordMap.getDocLen(), (byte)0));
        }
        
        // Merge all doc vecs into one dataset
        this.totalExamples = allDocVecs.size();
        this.dataSet = DataSet.merge(allDocVecs);        
    }
    
    public void pruneVocabulary(int ntopTerms) {
        System.out.println("Pruning vocab to " + ntopTerms);
        docWordMaps = trimDocs(ntopTerms);
    }
    
    private ArrayList<DocVector> trimDocs(int nTopTerms) {
        
        ArrayList<DocVector> trimmedDocs = new ArrayList<DocVector>(docWordMaps.size());
        
        // Collect the terms from collFreq table and sort.
        List<TermInfo> allTerms = new ArrayList<>(globalTermId);
        for (Map.Entry<String, TermInfo> e : collFreq.entrySet()) {
            TermInfo tinfo = e.getValue();
            allTerms.add(tinfo);
        }
        
        Collections.sort(allTerms);  // sort by descending tf*idf values
        topTerms = allTerms.subList(0, nTopTerms);
        
        HashMap<String, TermInfo> topTermsMap = new HashMap<>();
        int i = 1;
        for (TermInfo topTerm : topTerms) {
            topTermsMap.put(topTerm.term, new TermInfo(topTerm.term, topTerm.tf, i++, topTerm.idf));
        }
        
        // we have now obtained a trimmed vocabulary... readjust
        // the info of each doc iterating through the docWordMaps list
        for (DocVector docwordMap : docWordMaps) {  // each doc in pool
            if (docwordMap == null) {
                continue;
            }
                
            DocVector trimmedDocWordMap = new DocVector(docwordMap.docName);
            
            for (TermInfo docTermInfo : docwordMap.vec) { // each term in doc                                                    
                // retain in this doc only if a top term
                TermInfo refTermInfo = topTermsMap.get(docTermInfo.term);
                if (refTermInfo != null) {
                    trimmedDocWordMap.addTermInfo(
                            new TermInfo(docTermInfo.term, docTermInfo.tf,
                                    refTermInfo.id, docTermInfo.idf));
                }
            }
            
            trimmedDocs.add(trimmedDocWordMap);
        }
        
        globalTermId = nTopTerms;
        return trimmedDocs;        
    }
    
    public int getDimension() {
        return globalTermId;
    }
    
    public List<TermInfo> getTopTerms() {
        return topTerms;
    }
    
    @Override
    public void fetch(int numExamples) {
        List<DataSet> newData = new ArrayList<>();
        for (int grabbed = 0; grabbed < numExamples && cursor < dataSet.numExamples(); cursor++,grabbed++) {
            DataSet ds = dataSet.get(cursor);
            if (ds != null)
                newData.add(ds);
        }
        if (newData.size() > 0)
            this.curr = DataSet.merge(newData);
    }    
}

