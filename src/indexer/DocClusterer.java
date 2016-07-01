/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import wvec.DocVec;
import wvec.WordVec;
import wvec.WordVecs;

/**
 *
 * @author Debasis
 */
public class DocClusterer {
    int numClusters;
    DocVec dvec;

    public DocClusterer(String docId, String docText, int numClusters) throws Exception {
        this.numClusters = numClusters;
        dvec = new DocVec(docId, docText);
    }
    
    public String getClusterVecs() throws Exception {
        StringBuffer buff = new StringBuffer();
        List<CentroidCluster<WordVec>> clusters = clusterWords(dvec.getWordMap(), numClusters);
        if (clusters == null)
            return "";
        int i = 0;
        
        for (CentroidCluster<WordVec> c : clusters) {
            //List<WordVec> thisClusterPoints = c.getPoints();
            //WordVec clusterCenter = WordVecs.getCentroid(thisClusterPoints);
            Clusterable clusterCenter = c.getCenter();
            WordVec clusterWordVec = new WordVec("Cluster_" + i, clusterCenter.getPoint());
            //clusterCenter.setWord("Cluster_" + numClusters);
            buff.append(clusterWordVec.toString()).append(":");
            i++;
        }
        
        return buff.toString();
    }
    
    public List<CentroidCluster<WordVec>> clusterWords(
                HashMap<String, WordVec> wvecMap,
                int numClusters
            ) throws Exception {
        System.out.println("Clustering document: " + dvec.getDocId());
        List<WordVec> wordList = new ArrayList<>(wvecMap.size());
        for (Map.Entry<String, WordVec> e : wvecMap.entrySet()) {
            wordList.add(e.getValue());
        }
        
        KMeansPlusPlusClusterer<WordVec> clusterer =
                new KMeansPlusPlusClusterer<>(Math.min(numClusters, wordList.size()));
        if (wordList.size() == 0)
            return null;
        List<CentroidCluster<WordVec>> clusters = clusterer.cluster(wordList);        
        return clusters;
    }    
}
