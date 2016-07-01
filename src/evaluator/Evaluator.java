/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package evaluator;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author dganguly
 */

class Alignment {
    String src;
    String tgt;

    public Alignment(String line) {
        String[] tokens = line.split("\\s+");
        src = tokens[0];
        tgt = tokens[1];
    }        
    
    public Alignment(String src, String tgt) {
        this.src = src;
        this.tgt = tgt;
    }            
}

public class Evaluator {
    String ofile;
    String refFile;
    Properties prop;
    
    HashMap<String, Alignment> alignMap;
    ArrayList<Alignment> outAligns;

    public Evaluator(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile)); 
        
        ofile = prop.getProperty("out.align.file");
        refFile = prop.getProperty("ref.align.file");
        
        alignMap = new HashMap<>();
        outAligns = new ArrayList<>();
                    
        load();
    }
    
    void load() throws Exception {
        FileReader fr = new FileReader(refFile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        
        while ((line = br.readLine()) != null) {
            Alignment a = new Alignment(line);
            alignMap.put(a.src, a);
        }
        
        br.close();
        fr.close();
        
        fr = new FileReader(ofile);
        br = new BufferedReader(fr);
        
        while ((line = br.readLine()) != null) {
            Alignment a = new Alignment(line);
            outAligns.add(a);
        }
        
        br.close();
        fr.close();        
    }
    
    public void evaluate() {
        int correct = 0;
        for (Alignment a : outAligns) {
            Alignment ref = alignMap.get(a.src);
            if (ref == null)
                continue;
            if (ref.tgt.equals(a.tgt)) {
                //System.out.println(ref.src + "\t" + ref.tgt);
                correct++;
            }
        }
        System.out.println("correct = " + correct + ", Prec: " + correct/(float)outAligns.size() + ", Recall: " + correct/(float)alignMap.size());
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java TextDocIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            Evaluator eval = new Evaluator(args[0]);
            eval.evaluate();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
