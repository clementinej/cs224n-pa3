package cs224n.corefsystems;

import java.util.Collection;
import java.util.*;

import cs224n.coref.ClusteredMention;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.util.Pair;
import cs224n.coref.Mention;

public class AllSingleton implements CoreferenceSystem {

	@Override
	public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<ClusteredMention> runCoreference(Document doc) {
	   //(variables)
        List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
        //(for each mention...)
        for (Mention m : doc.getMentions()) {
            ClusteredMention cluster = m.markSingleton();
            mentions.add(cluster);
        }
        //(return the mentions)
        return mentions;
	}
}
