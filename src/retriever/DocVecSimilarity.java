/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import indexer.TextDocIndexer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.WeightedTerm;
import org.apache.lucene.util.BytesRef;
import wvec.DocVec;
import wvec.WordVec;
import wvec.WordVecs;

/**
 * Use the vector based representation of documents and queries to
 * compute similarity values.
 * Reranks the list of top docs by adding the contributions from the
 * vector based similarities. Currently, we do a linear combination after
 * similarity score normalization (of the text based scores)
 * 
 * @author Debasis
 */

class CentroidLinkageSim {
    
    static public float computeSim(DocVec a, DocVec b) throws Exception {
        List<WordVec> alist = a.getWordVecs();
        List<WordVec> blist = b.getWordVecs();

        float avgSim = 0;
        float thisSim;
        float normalizationFactor = 0;
        float wt = 1;
        
        for (WordVec avec : alist) {            
            for (WordVec bvec : blist) {
                thisSim = WordVecs.getSim(avec, bvec);
                avgSim += thisSim*wt;
                normalizationFactor = normalizationFactor + wt;
            }            
        }
        return avgSim/(float)(normalizationFactor);
    }    
}

public class DocVecSimilarity {
    TopDocs topDocs;
    int numDocs;
    IndexReader reader;
    DocVec queryVec;
    Properties prop;
    
    boolean compressedIndex, vocabCluster;
    byte allDocWords;
    float textSimWt;

    static final byte WORDS_AS_CLUSTERS = 0;
    static final byte ALL_WORDS_AS_SEPARATE_CLUSTERS = 1;
    static final byte ALL_WORDS_AS_ONE_CLUSTER = 2;
    
    public DocVecSimilarity(Properties prop, IndexReader reader, TopDocs topDocs, WeightedTerm[] queryTerms, float textSimWt) throws Exception {
        this.reader = reader;
        this.prop = prop;
        this.topDocs = topDocs;
        numDocs = topDocs.scoreDocs.length;
        
        // The vector representation for the query
        int numClusters = Integer.parseInt(prop.getProperty("wvecs.numclusters", "5"));
        queryVec = new DocVec(queryTerms, numClusters);
        
        this.textSimWt = textSimWt;
        
        compressedIndex = Boolean.parseBoolean(prop.getProperty("index.compressed", "true"));
        // possible values cluster/one/all
        String docSetFormation = prop.getProperty("doc.setformation", "cluster");
        allDocWords = docSetFormation.equals("cluster")? WORDS_AS_CLUSTERS:
                docSetFormation.equals("one")?
                ALL_WORDS_AS_ONE_CLUSTER : ALL_WORDS_AS_SEPARATE_CLUSTERS;
        
        int numVocabClusters = Integer.parseInt(prop.getProperty("retrieve.vocabcluster.numclusters", "0"));
        this.vocabCluster = numVocabClusters > 0;
    }

    HashMap<String, WordVec> loadWordClusterInfo(int docId, byte allDocWords) throws Exception {
        String termText;
        BytesRef term;
        Terms tfvector;
        TermsEnum termsEnum;
        HashMap<Integer, List<WordVec>> clusterMap = new HashMap<>();
        
        tfvector = reader.getTermVector(docId, TextDocIndexer.FIELD_ANALYZED_CONTENT);
        
        // Construct the normalized tf vector
        termsEnum = tfvector.iterator(); // access the terms for this field
        
        int wordId = 0;
        //System.out.println("Getting cluster ids for document " + docId);
    	while ((term = termsEnum.next()) != null) { // explore the terms for this field
            wordId++;
            termText = term.utf8ToString();
            int clusterId =
                    allDocWords==ALL_WORDS_AS_SEPARATE_CLUSTERS? wordId : // each word a new cluster id
                    allDocWords==WORDS_AS_CLUSTERS? WordVecs.getClusterId(termText): // cluster ids from vocab
                    0; // each word the same cluster id
            if (clusterId < 0)
                continue;
            
            // Get the term and its cluster id.. Store in a hashmap for
            // computing group-wise centroids
            WordVec wv = WordVecs.getVecCached(termText);
            if (wv == null)
                continue;
            List<WordVec> veclist = clusterMap.get(clusterId);
            if (veclist == null) {
                veclist = new ArrayList<>();
                clusterMap.put(clusterId, veclist);
            }
            veclist.add(wv);
        }
        //System.out.println("Got cluster ids for doc " + docId);

        // Return a list of centroids computed by grouping together the cluster ids
        HashMap<String, WordVec> centroids = new HashMap<>();
        
        //System.out.println("#clusters in doc " + docId + ": " + clusterMap.size());
        for (Map.Entry<Integer, List<WordVec>> e : clusterMap.entrySet()) {
            List<WordVec> veclist = e.getValue();
            WordVec centroid = WordVecs.getCentroid(veclist);
            centroids.put("Cluster: " + e.getKey(), centroid);
        }
        
        return centroids;
    }
    
    ScoreDoc[] computeSims() throws Exception {
        
        float simValue;
        ScoreDoc[] scoreDocs = new ScoreDoc[numDocs];
        
        int i = 0;
        DocVec docvec;
        for (ScoreDoc sd : topDocs.scoreDocs) {
            Document doc = reader.document(sd.doc);
            
            // Two control flows depending on the type of the word clusters
            // we are interested in... i.e. global or per document...
            // the former is stored in an auxiliary index, latter stored
            // as auxiliary field of the retrievable document index...
            if (vocabCluster) {
                HashMap<String, WordVec> wvmap = loadWordClusterInfo(sd.doc, allDocWords);
                docvec = new DocVec(wvmap);
            }
            else {
                docvec = new DocVec(doc, compressedIndex);
            }
            
            simValue = CentroidLinkageSim.computeSim(queryVec, docvec);
            scoreDocs[i++] = new ScoreDoc(sd.doc, simValue);
        }
        
        return scoreDocs;
    }       
}
