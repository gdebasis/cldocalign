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

class TranslationInfo {
    String word;
    float weight;

    public TranslationInfo(String line) {
        String[] tokens = line.split("\\s+");
        this.word = tokens[1];
        this.weight = Float.parseFloat(tokens[2]);
    }
}

class Translations {
    String source;
    List<TranslationInfo> tlist;
    
    public Translations(String source) {
        this.source = source;
        tlist = new ArrayList<>();
    }
    
    public void addRecord(String line, float cutoff) {
        TranslationInfo tinfo = new TranslationInfo(line);
        if (tinfo.weight > cutoff)
            tlist.add(new TranslationInfo(line));
    }    
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        //buff.append(source).append("\t");
        for (TranslationInfo tinfo : tlist) {
            buff.append(tinfo.word).append(PayloadAnalyzer.delim).append(tinfo.weight).append(" ");
        }
        return buff.toString();
    }    
}

public class Dictionary {
    float cutoff;
    Map<String, Translations> tmap;

    public Dictionary(float cutoff) {        
        tmap = new HashMap<>();        
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
    
    public String getTranslations(String sourceWord) {
        Translations tlist = tmap.get(sourceWord);
        if (tlist == null)
            return "";
        
        return tlist.toString();
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("usage: java Dictionary <dict file>");
            args = new String[1];
            args[0] = "dict/lex.e2d";
        }
        
        try {
            Dictionary dict = new Dictionary(0.01f);
            dict.load(args[0]);
            System.out.println(dict);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        } 
    }
}
