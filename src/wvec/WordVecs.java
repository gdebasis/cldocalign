/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wvec;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Debasis
 */

/**
 * A collection of WordVec instances for each unique term in
 * the collection.
 * @author Debasis
 */
public class WordVecs {

    static Properties prop;
    static IndexReader reader;
    static IndexSearcher searcher;
    static IndexReader clusterInfoReader;
    
    static HashMap<String, Integer> clusterMap = new HashMap();
    static HashMap<String, WordVec> wvecMap = new HashMap();
    
    static public void init(String propFile) throws Exception {        
        int numDocs;
        prop = new Properties();
        prop.load(new FileReader(propFile));        
        String loadFrom = prop.getProperty("wvecs.index");
        File indexDir = new File(loadFrom);
        
        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        searcher = new IndexSearcher(reader);
        
        int numVocabClusters = Integer.parseInt(prop.getProperty("retrieve.vocabcluster.numclusters", "0"));
        if (numVocabClusters > 0) {
            String clusterInfoIndexPath = prop.getProperty("wvecs.clusterids.basedir") + "/" + numVocabClusters;
            clusterInfoReader = DirectoryReader.open(FSDirectory.open((new File(clusterInfoIndexPath)).toPath()));
            
            System.out.println("Loading cluster ids in memory...");
            numDocs = clusterInfoReader.numDocs();
            for (int i=0; i<numDocs; i++) {
                Document d = clusterInfoReader.document(i);
                String wordName = d.get(WordVecsIndexer.FIELD_WORD_NAME);
                int clusterId = Integer.parseInt(d.get(WordVecsIndexer.FIELD_WORD_VEC));
                clusterMap.put(wordName, clusterId);
            }        
        }
        
        System.out.println("Loading wvecs in memory...");
        numDocs = reader.numDocs();
        for (int i=0; i<numDocs; i++) {
            Document d = reader.document(i);
            String wordName = d.get(WordVecsIndexer.FIELD_WORD_NAME);
            String line = d.get(WordVecsIndexer.FIELD_WORD_VEC);
            WordVec wv = new WordVec(line);
            wv.normalize();
            wvecMap.put(wordName, wv);
        }
        
    }
    
    static public void close() throws Exception {
        reader.close();
    }

    static Query getLuceneQueryObject(String word) {
        BooleanQuery q = new BooleanQuery();
        TermQuery tq = new TermQuery(new Term(WordVecsIndexer.FIELD_WORD_NAME, word));
        q.add(tq, BooleanClause.Occur.MUST);
        return q;
    }
    
    static public WordVec getVecCached(String word) throws Exception {
        return wvecMap.get(word);
    }
    
    static public WordVec getVec(String word) throws Exception {
        TopScoreDocCollector collector;
        TopDocs topDocs;
        collector = TopScoreDocCollector.create(1);
        searcher.search(getLuceneQueryObject(word), collector);
        topDocs = collector.topDocs();
        
        if (topDocs.scoreDocs == null || topDocs.scoreDocs.length == 0) {
            //System.err.println("vec for word: " + word + " not found");
            return null;
        }
        
        int wordId = topDocs.scoreDocs[0].doc;
        Document matchedWordVec = reader.document(wordId);
        String line = matchedWordVec.get(WordVecsIndexer.FIELD_WORD_VEC);
        WordVec wv = new WordVec(line);
        return wv;
    }

    static public float getSim(String u, String v) throws Exception {
        WordVec uVec = getVec(u);
        WordVec vVec = getVec(v);
        return uVec.cosineSim(vVec);
    }
    
    static public float getSim(WordVec u, WordVec v) throws Exception {
        return u.cosineSim(v);
    }
    
    static public WordVec getCentroid(List<WordVec> wvecs) {
        int numVecs = wvecs.size(), j, dimension = wvecs.get(0).getDimension();
        WordVec centroid = new WordVec(dimension);
        for (WordVec wv : wvecs) {
            for (j = 0; j < dimension; j++) {
                centroid.vec[j] += wv.vec[j];
            }
        }
        for (j = 0; j < dimension; j++) {
            centroid.vec[j] /= (double)numVecs;
        }
        
        return centroid;
    }
    
    static public int getClusterId(String word) throws Exception {
        if (!clusterMap.containsKey(word))
            return -1;
        return clusterMap.get(word);
    }    
    
    public static void main(String[] args) {
        try {
            WordVecs.init("init.properties");
            System.out.println(WordVecs.getVec("govern"));
            WordVecs.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

