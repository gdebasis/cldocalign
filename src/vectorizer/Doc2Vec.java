/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vectorizer;

import indextranslator.Dictionary;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.deeplearning4j.datasets.iterator.BaseDatasetIterator;
import org.deeplearning4j.datasets.iterator.DataSetIterator;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.RBM;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.IterationListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.lossfunctions.LossFunctions;

/**
 *
 * @author Debasis
 */

public class Doc2Vec {

    Properties prop;
    LuceneDocFetcher luceneDocFetcher;
    Directory srcdir;
    Directory tgtdir;
    int nTopTerms;
    Dictionary dict;
    
    public Doc2Vec(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        srcdir = FSDirectory.open(Paths.get(prop.getProperty("translated.index")));
        tgtdir = FSDirectory.open(Paths.get(prop.getProperty("index"))); // have to translate this with dict
        nTopTerms = Integer.parseInt(prop.getProperty("srbm.topterms", "1000"));        
        
        dict = new Dictionary(
                Integer.parseInt(prop.getProperty("numtranslated_words", "3")),
                Float.parseFloat(prop.getProperty("translation.threshold_weight", "0.01"))
        );
        System.out.println("Loading dict for query translation...");
        dict.load(prop.getProperty("dict"));                    
    }
    
    Properties getProperties() { return prop; }
    
    String getOutputVecFileName() {
        String dirName = prop.getProperty("srbm.vecfile.dir");
        return dirName + "/" + prop.getProperty("srbm.outfile.prefix") + ".txt";
    }
    
    void storeOutputLayer(
            DataSetIterator iter,
            MultiLayerNetwork model) throws Exception {
        iter.reset();
        
        String fileName = getOutputVecFileName();
        System.out.println("saving o/p layers in file: " + fileName);
        FileWriter fw = new FileWriter(fileName);
        StringBuffer buff;
    
        int docid = 0;
        
        while (iter.hasNext()) {
            String docName = luceneDocFetcher.getDocName(docid);
            
            DataSet v = iter.next();
            INDArray st = model.output(v.getFeatures());
            buff = new StringBuffer();
            buff.append(docName)
                .append('\t')
                .append(st.toString())
                .append("\n");
            
            fw.write(buff.toString());
            docid++;
        }

        fw.close();
    }
    
    void saveModel(String outputFilename, MultiLayerNetwork model) throws IOException {        
        ModelSerializer.writeModel(model, new File(outputFilename + ".srbm"), true);        
    }
    
    MultiLayerNetwork loadModel(String outputFilename) throws IOException {
        return ModelSerializer.restoreMultiLayerNetwork(new File(outputFilename + ".srbm"));
    }

    MultiLayerNetwork buildAutoEncoder(int vocabSize, int iterations) {
        int[] numInputs = { vocabSize, 100, 25 };
        int seed = 100;

        /*
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .momentum(0.1)
                .momentumAfter(Collections.singletonMap(10, 0.01))
                .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT)
                .list(4)
                .layer(0, new RBM.Builder().nIn(vocabSize).nOut(numInputs[0])
                        .lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                .layer(1, new RBM.Builder().nIn(numInputs[0]).nOut(numInputs[1])
                        .lossFunction(LossFunctions.LossFunction.RMSE_XENT).build())
                .layer(2, new RBM.Builder().lossFunction(LossFunctions.LossFunction.RMSE_XENT)
                        .nIn(numInputs[1]).nOut(numInputs[2]).build())
                .layer(3, new RBM.Builder().lossFunction(LossFunctions.LossFunction.RMSE_XENT)
                        .nIn(numInputs[2]).nOut(numInputs[3]).build())
                .pretrain(false)
                .build();
        */
    
        /*
    MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .gradientNormalization(GradientNormalization.ClipElementWiseAbsoluteValue)
            .gradientNormalizationThreshold(1.0)
            .iterations(iterations)
            .momentum(0.5)
            .momentumAfter(Collections.singletonMap(3, 0.9))
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .list(3)
            .layer(0, new AutoEncoder.Builder().nIn(numInputs[0]).nOut(numInputs[1])
                    .weightInit(WeightInit.XAVIER).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
                    .corruptionLevel(0.3)
                    .build())
            .layer(1, new AutoEncoder.Builder().nIn(numInputs[1]).nOut(numInputs[2])
                    .weightInit(WeightInit.XAVIER).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
                    .corruptionLevel(0.3)
                    .build())
            //.layer(2, new AutoEncoder.Builder().nIn(numInputs[2]).nOut(numInputs[3])
            //        .weightInit(WeightInit.XAVIER).lossFunction(LossFunctions.LossFunction.RMSE_XENT)
            //        .corruptionLevel(0.3)
            //        .build())
            //.layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD).activation("softmax")
            //        .nIn(numInputs[2]).nOut(numInputs[3]).build())
            .layer(2, new OutputLayer.Builder(LossFunctions.LossFunction.RMSE_XENT)
                    .nIn(numInputs[2]).nOut(numInputs[3]).build())
            .pretrain(true).backprop(false)
            .build();        
        */
        
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()		
             .seed(seed) // Locks in weight initialization for tuning		
             .iterations(iterations) // # training iterations predict/classify & backprop		
             .learningRate(1e-6f) // Optimization step size		
             .optimizationAlgo(OptimizationAlgorithm.CONJUGATE_GRADIENT) // Backprop to calculate gradients		
             .l1(1e-1).regularization(true).l2(2e-4)		
             .useDropConnect(true)		
             .list(2) // # NN layers (doesn't count input layer)		
             .layer(0, new RBM.Builder(RBM.HiddenUnit.RECTIFIED, RBM.VisibleUnit.GAUSSIAN)		
                             .nIn(numInputs[0]) // # input nodes		
                             .nOut(numInputs[1]) // # fully connected hidden layer nodes. Add list if multiple layers.		
                             .weightInit(WeightInit.XAVIER) // Weight initialization		
                             .k(1) // # contrastive divergence iterations		
                             .activation("relu") // Activation function type		
                             .lossFunction(LossFunctions.LossFunction.RMSE_XENT) // Loss function type		
                             .updater(Updater.ADAGRAD)		
                             .dropOut(0.5)		
                             .build()		
             ) // NN layer type		
             .layer(1, new RBM.Builder(RBM.HiddenUnit.RECTIFIED, RBM.VisibleUnit.GAUSSIAN)		
                             .nIn(numInputs[1]) // # input nodes		
                             .nOut(numInputs[2]) // # fully connected hidden layer nodes. Add list if multiple layers.		
                             .weightInit(WeightInit.XAVIER) // Weight initialization		
                             .k(1) // # contrastive divergence iterations		
                             .activation("relu") // Activation function type		
                             .lossFunction(LossFunctions.LossFunction.RMSE_XENT) // Loss function type		
                             .updater(Updater.ADAGRAD)		
                             .dropOut(0.5)		
                             .build()		
             ) // NN layer type		
             .build();
        
        MultiLayerNetwork model = new MultiLayerNetwork(conf);
        model.init();
        return model;
    }

    private void saveInputVectors(DataSetIterator iter, LuceneDocFetcher fetcher) throws Exception {
        int count = 0;
        String dirName = prop.getProperty("srbm.vecfile.dir");
        FileWriter fw = new FileWriter(dirName + "/inputvecs.txt");
        
        List<TermInfo> topTerms = fetcher.getTopTerms();        
        StringBuffer buff = new StringBuffer();
        for (TermInfo tinfo : topTerms) {
            buff.append(tinfo.term).append(" ");
        }
        buff.append("\n");
        fw.write(buff.toString());
        
        while (iter.hasNext()) {
            DataSet v = iter.next();            
            fw.write(fetcher.getDocName(count) + ": " + v.getFeatures() + "\n");
            count++;
        }
        
        fw.close();
    }
    
    private DataSetIterator getDataSetIterator(LuceneDocFetcher fetcher) {
        int numData = fetcher.totalExamples();
        DataSetIterator iter = new BaseDatasetIterator(1, numData, fetcher);
        return iter;
    }
    
    public DataSetIterator loadDocVecs() throws Exception {
        
        // Load documents to build entire vocabulary
        luceneDocFetcher = new LuceneDocFetcher();
        luceneDocFetcher.loadDcuments(srcdir);
        luceneDocFetcher.loadDcuments(tgtdir, dict);
        
        // Chop off vvocabulary to manageable size
        luceneDocFetcher.pruneVocabulary(nTopTerms);
        luceneDocFetcher.buildVecs();
        
        DataSetIterator datasetIter = getDataSetIterator(luceneDocFetcher);
        return datasetIter;        
    }
    
    public void vectorizeDocs() throws Exception {

        DataSetIterator iter = loadDocVecs();
        saveInputVectors(iter, luceneDocFetcher);
        
        final int vocabSize = luceneDocFetcher.getDimension();
        
        final int numIters = Integer.parseInt(prop.getProperty("srbm.numiters", "3"));
        int listenerFreq = numIters/2;
        
        System.out.println("Initializing stacked RBMs");

        MultiLayerNetwork model = buildAutoEncoder(vocabSize, numIters);
        model.setListeners(Arrays.asList((IterationListener) new ScoreIterationListener(listenerFreq)));
        
        System.out.println("Training stacked RBMs");
        iter.reset();
        model.fit(iter);

        System.out.println("Saving output of the model");
        storeOutputLayer(iter, model);
        
        System.out.println("saving model in file " + prop.getProperty("srbm.ser"));
        saveModel(prop.getProperty("srbm.ser"), model);        
        
        srcdir.close();
        tgtdir.close();
    }

    // A small unit test for testing the vectorization
    public static void main(String[] args) {
        
        if (args.length < 1) {
            args = new String[1];
            args[0] = "init.properties";
        }

        try {
            Doc2Vec dva = new Doc2Vec(args[0]);            
            // Train the stacked RBM jointly on the source
            // and the translated target documents.
            dva.vectorizeDocs();            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
