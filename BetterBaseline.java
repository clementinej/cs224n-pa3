package cs224n.corefsystems;

import java.lang.System;
import java.util.*;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.util.Pair;
import cs224n.coref.Mention;

public class BetterBaseline implements CoreferenceSystem {

    @Override
    public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
        // Implement a head matching system
        // accumulate statistics on the head parses of the mentions
        // handle pronouns separately
        // model on distance, etc.

    }

    @Override
    public List<ClusteredMention> runCoreference(Document doc) {
        List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
        Map<String, Entity> clusters = new HashMap<String, Entity>();
        // for each mention in the document
        for (Mention m : doc.getMentions()) {
            // if the mention is already in the cluster
            if (clusters.containsKey(m.headWord())) {
                // mark it as coreferential
                mentions.add(m.markCoreferent(clusters.get(m.headWord())));
            } else {
                // otherwise create a new singleton cluster
                ClusteredMention newCluster = m.markSingleton();
                mentions.add(newCluster);
                System.out.println(m.headWord());
                clusters.put(m.headWord(), newCluster.entity);
            }
        }
        // return the mentions
        return mentions;
    }
}
