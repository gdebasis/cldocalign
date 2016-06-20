/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indextranslator;
import java.io.*;
import java.util.*;
import retriever.PayloadAnalyzer;

/**
 *
 * @author dganguly
 */

public class Dictionary {
    float cutoff;
    int numTranslations;
    Map<String, Translations> tmap;

    public Dictionary(int numTranslations, float cutoff) {        
        tmap = new HashMap<>();      
        this.cutoff = cutoff;
        this.numTranslations = numTranslations;
    }
    
    // Trim the dictionary so that each word contains a max
    // number of translation alternatives
    protected void trim() {
        for (Map.Entry<String, Translations> e : tmap.entrySet()) {
            Translations t = e.getValue();
            Collections.sort(t.tlist);
            t.tlist = t.tlist.subList(0, Math.min(numTranslations, t.tlist.size()));
        }
    }
    
    public void load(String inputFile) throws Exception {
        FileReader fr = new FileReader(inputFile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        String srcword;
        
        while ((line = br.readLine()) != null) {
            String[] tokens = line.split("\\s+");
            srcword = tokens[0];
            Translations tranlations = tmap.get(srcword);
            if (tranlations == null) {
                tranlations = new Translations(srcword);
                tmap.put(srcword, tranlations);
            }
            tranlations.addRecord(line, cutoff);
        }
        
        br.close();
        fr.close();
        
        trim();        
    }
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Map.Entry<String, Translations> e : tmap.entrySet()) {
            Translations t = e.getValue();
            buff.append(t.toString()).append("\n");
        }
        return buff.toString();
    }
    
    public String getTranslations(String sourceWord, int tf) {
        Translations tlist = tmap.get(sourceWord);
        if (tlist == null)
            return sourceWord;
        
        return tlist.toString(tf);
    }
    
    public Translations getTranslationTerms(String sourceWord) {
        Translations t = tmap.get(sourceWord);
        if (t != null)
            return t;
        t = new Translations(sourceWord);
        t.addRecord(sourceWord + "\t" + sourceWord + " 1.0", cutoff); // hack to put the src word back onto itself (named entities e.g.)
        return t;
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: java Dictionary <dict file>");
            args = new String[1];
            args[0] = "dict/lex.e2d";
        }
        
        try {
            Dictionary dict = new Dictionary(3, 0.01f);
            dict.load(args[0]);
            System.out.println(dict);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        } 
    }
}
