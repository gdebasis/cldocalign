/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wvec;

import indexer.CompressionUtils;
import indexer.TextDocIndexer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DoublePoint;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.highlight.WeightedTerm;

/**
 * Represent each document as a set of cluster points
 * @author Debasis
 */
public class DocVec {
    String id;
    String text;  // Ensure that you pass a preprocessed list of tokens delimited by white spaces.
    HashMap<String, WordVec> wvecMap;
    KMeansPlusPlusClusterer<WordVec> clusterer;
    
    static final float EPSILON = .001f;
    static final int MIN_PTS = 1;
    
    public DocVec(String id, String text) throws Exception {
        this.id = id;
        this.text = text;
        wvecMap = new HashMap<>();
        buildTerms();
    }

    public String getDocId() { return this.id; }
    public HashMap<String, WordVec> getWordMap() { return wvecMap; }
    
    // This constructor is to be called during retrieval when we want
    // to construct a vector representation of the query terms
    public DocVec(WeightedTerm[] queryTerms, int numClusters) throws Exception {
        wvecMap = new HashMap<>();
        
        for (WeightedTerm term: queryTerms) {
            WordVec qwv = WordVecs.getVec(term.getTerm());
            if (qwv != null) {
                qwv.normalize();
                wvecMap.put(qwv.getWord(), qwv);
            }
        }
        
        List<WordVec> wordList = new ArrayList<>(wvecMap.size());
        for (Map.Entry<String, WordVec> e : wvecMap.entrySet()) {
            wordList.add(e.getValue());
        }
        
        if (wordList.size() == 0)
            return;
        
        // Cluster the query word vecs
        clusterer = new KMeansPlusPlusClusterer<>(Math.min(numClusters, wordList.size()));        
        List<CentroidCluster<WordVec>> clusters = clusterer.cluster(wordList);

        wvecMap.clear();
        int i = 0;
        for (CentroidCluster<WordVec> c : clusters) {
            Clusterable clusterCenter = c.getCenter();
            WordVec clusterWordVec = new WordVec("Cluster_" + i, clusterCenter.getPoint());
            wvecMap.put(clusterWordVec.getWord(), clusterWordVec);
            i++;
        }        
    }
    
    // This constructor is going to be invoked during similarity
    // computation in the retrieval phase, i.e. we would have to
    // load the clustered representation (stored in the index)
    // to build up the wordmap (we won't need the words nor would
    // we have them, because these would be cluster centre points rather
    // than individual word points.
    public DocVec(Document doc, boolean compressedIndex) {
        int numClusterCentres;
        wvecMap = new HashMap<>();
        String clusterCentres;
        
        // If we are going to use per-doc clusters
        if (!compressedIndex)
            clusterCentres = doc.get(TextDocIndexer.FIELD_WORDVEC_CLUSTER_CENTRES);
        else
            clusterCentres = CompressionUtils.decompress(doc.getBinaryValue(TextDocIndexer.FIELD_WORDVEC_CLUSTER_CENTRES).bytes);

        String[] clusterCentreVecStrings = clusterCentres.split(":");
        numClusterCentres = clusterCentreVecStrings.length;
        for (int i = 0; i < numClusterCentres; i++) {
            WordVec wv = new WordVec(clusterCentreVecStrings[i]);
            wvecMap.put(wv.word, wv);
        }
    }
    
    // Directly pass a map set from else where
    public DocVec(HashMap<String, WordVec> wvecMap) {
        this.wvecMap = wvecMap;
    }
    
    void buildTerms() throws Exception {
        String[] tokens = text.split("\\s+");
        for (String token: tokens) {
            WordVec wv = wvecMap.get(token);
            if (wv == null) {
                wv = WordVecs.getVec(token);
                if (wv != null)
                    wvecMap.put(token, wv);
            }
        }
    }
    
    public List<WordVec> getWordVecs() {
        List<WordVec> wvList = new ArrayList<>();
        for (Entry<String, WordVec> e : wvecMap.entrySet()) {
            wvList.add(e.getValue());
        }
        return wvList;
    }

    // For unit testing
    public static void main(String[] args) {
        List<DoublePoint> plist = new ArrayList<>();
        int[][] points = {{1, 1}, {0, 0}, {1, 0}, {0, 1},
                     {10, 10}, {9, 9}, {10, 9}, {9, 10}};
        
        for (int[] point: points) {
            DoublePoint dp = new DoublePoint(point);
            plist.add(dp);
            //System.out.println(dp);
        }
    }
}
