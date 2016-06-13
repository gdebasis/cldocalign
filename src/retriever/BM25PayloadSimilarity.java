/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package retriever;

import org.apache.lucene.analysis.payloads.PayloadHelper;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.util.BytesRef;

/**
 *
 * @author dganguly
 */
public class BM25PayloadSimilarity extends BM25Similarity {

    public BM25PayloadSimilarity(float k, float b) {
        super(k, b);
    }
    
    @Override
    protected float scorePayload(int doc, int start, int end, BytesRef payload) {
        if (payload == null) return 1.0f;
        float wt = PayloadHelper.decodeFloat(payload.bytes, payload.offset);
        return wt;
    }    
}
