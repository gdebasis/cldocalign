/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indextranslator;

import java.util.ArrayList;
import java.util.List;
import retriever.PayloadAnalyzer;

/**
 *
 * @author dganguly
 */
public class Translations {
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
    
    public List<TranslationInfo> getTranslationInfo() { return tlist; }
    
    public String toString(int tf) {
        StringBuffer buff = new StringBuffer();
        //buff.append(source).append("\t");
        for (TranslationInfo tinfo : tlist) {
            buff.append(tinfo.word).append(PayloadAnalyzer.delim).append(tinfo.weight*tf).append(" ");
        }
        return buff.toString();
    }    
}

