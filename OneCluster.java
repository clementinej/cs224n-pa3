package cs224n.corefsystems;

import java.util.*;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.coref.Mention;
import cs224n.util.Pair;

public class OneCluster implements CoreferenceSystem {

    // Assign every mention to a single entity.
    @Override
    public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<ClusteredMention> runCoreference(Document doc) {
        //(variables)
        List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
        ClusteredMention cluster = null;
        //(for each mention...)
        for (Mention m : doc.getMentions()) {
            if (cluster == null) {
                cluster = m.markSingleton();
            }
            mentions.add(m.markCoreferent(cluster));
        }
        //(return the mentions)
        return mentions;
    }
}
