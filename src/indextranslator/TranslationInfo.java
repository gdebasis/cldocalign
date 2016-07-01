/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indextranslator;

/**
 *
 * @author dganguly
 */
public class TranslationInfo implements Comparable<TranslationInfo> {
    public String word;
    public float weight;

    public TranslationInfo(String line) {
        String[] tokens = line.split("\\s+");
        this.word = tokens[1];
        this.weight = Float.parseFloat(tokens[2]);
    }

    @Override
    public int compareTo(TranslationInfo that) {
        return -1*Float.compare(weight, that.weight); // descending
    }
    
}

