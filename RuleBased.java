package cs224n.corefsystems;

import java.util.Collection;
import java.util.List;
import java.util.*;


import cs224n.coref.ClusteredMention;
import cs224n.coref.Pronoun;
import cs224n.coref.Document;
import cs224n.coref.Entity;
import cs224n.util.Pair;
import cs224n.coref.Mention;
import cs224n.coref.*;
import cs224n.ling.Tree;

public class RuleBased implements CoreferenceSystem {
    
    HashMap<Pair<String, String>, Double> statistics = 
    	new HashMap<Pair<String, String>, Double>();
    @Override
    public void train(Collection<Pair<Document, List<Entity>>> trainingData) {
	for(Pair<Document, List<Entity>> pair : trainingData){
	    //--Get Variables
	    Document doc = pair.getFirst();
	    List<Entity> clusters = pair.getSecond();
	    List<Mention> mentions = doc.getMentions();

	    //--Iterate Over Coreferent Mention Pairs
	    for(Entity e : clusters){
		for(Pair<Mention, Mention> mentionPair : e.orderedMentionPairs()){
		    // if (!mentionPair.getFirst().headToken().isNoun() ||
		    // 	!mentionPair.getSecond().headToken().isNoun())
		    // 	continue;			
		    if (mentionPair.getFirst().headToken().isPronoun() ||
		    	mentionPair.getSecond().headToken().isPronoun())
		    	continue;			

		    String m = mentionPair.getFirst().headWord();
		    String n = mentionPair.getSecond().headWord();
		    Pair<String, String> candidate = null;
		    if (m.compareToIgnoreCase(n) != 0){
			if (m.compareToIgnoreCase(n) > 0)
			    candidate = new Pair<String, String>(m.toLowerCase(),n.toLowerCase());
			else
			    candidate = new Pair<String, String>(n.toLowerCase(),m.toLowerCase());
		    }
		    if (candidate == null) continue;
		    if (!statistics.containsKey(candidate))
			statistics.put(candidate, 1.0);
		    else
			statistics.put(candidate, statistics.get(candidate)+1);
		}
	    }
	}
	statisticsUpdate();
	
    }
    
    private void statisticsUpdate(){
	double sum = 0;
	for (Double count : statistics.values())
	    sum += count.doubleValue();
	System.out.println(sum);
	for (Pair<String, String> candidate : statistics.keySet())
	    statistics.put(candidate, statistics.get(candidate)/sum);
	// for (Pair<String, String> c : statistics.keySet())
	//     System.out.println(c.getFirst()+ " " + c.getSecond() + " " + statistics.get(c));
    }

    @Override
    public List<ClusteredMention> runCoreference(Document doc) {
	List<ClusteredMention> mentions = new ArrayList<ClusteredMention>();
	Map<String,Entity> clusters = new HashMap<String,Entity>();
	Set<Set<Mention>> mentionClusters = new HashSet<Set<Mention>>();

	// for(Mention m : doc.getMentions()){
	//     for(String s : m.sentence.nerTags) 
	// 	System.out.println(s);
	//     break;
	// }

	// Create singleton mentionClusters
	for(Mention m : doc.getMentions()){
	    Set<Mention> temp = new HashSet<Mention>();
	    temp.add(m);
	    mentionClusters.add(temp);
	}
	// First Pass
	statisticsBased(mentionClusters);	
	exactMatch(mentionClusters);
	// matchAppositives(doc, mentionClusters);
	// matchAcronym(mentionClusters);
	// predicateNominative(doc, mentionClusters);
	// matchStrictHead(mentionClusters);
	exactHeadMatch(mentionClusters);
	relaxedHeadMatch(mentionClusters);
	matchSpeaker(mentionClusters);
	pronounProcessing(doc, mentionClusters);
	

	createMentions(mentionClusters, mentions);
	return mentions;
    }

		



    private void createMentions(Set<Set<Mention>> mentionClusters, List<ClusteredMention> mentions){
	for(Set<Mention> a : mentionClusters){
	    ClusteredMention temp = null;
	    for(Mention m : a){
		try{
		    mentions.add(m.markCoreferent(temp));
		}catch(Exception E){
		    temp = m.markSingleton();
		    mentions.add(temp);
		}
	    }
	}
    }
	    
    
    private void merge(Set<Mention> a, Set<Mention> b){
	    a.addAll(b);
	    b.removeAll(b);
    }
    

    private void statisticsBased(Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.equals(n)) continue;
			Pair<String, String> candidate = null;
			if (m.headWord().compareToIgnoreCase(n.headWord()) != 0){
			    if (m.headWord().compareToIgnoreCase(n.headWord()) > 0)
				candidate = new Pair<String, String>(m.headWord().toLowerCase(),n.headWord().toLowerCase());
			    else
				candidate = new Pair<String, String>(n.headWord().toLowerCase(),m.headWord().toLowerCase());
			}
			if (candidate == null) continue;
			if (statistics.containsKey(candidate)){
			    merge(a,b);
			    break loop;
			}
		    }
		}
	    }
	}
    }

    private void exactMatch(Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.equals(n)) continue;
			if (m.headToken().isPronoun() ||
			    n.headToken().isPronoun())
			    continue;
			if(m.gloss().equalsIgnoreCase(n.gloss())){
			    merge(a,b);
			    break loop;
			}
		    }
		}
	    }
	}
    }

    private void exactHeadMatch(Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			//if(m.gloss().contains(n.gloss()) || n.gloss().contains(m.gloss())) continue;
			if(m.headWord().equalsIgnoreCase(n.headWord())){
			    merge(a,b);
			    break loop;
			}
		    }
		}
	    }
	}
    }

    private void relaxedHeadMatch(Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.gloss().contains(n.gloss()) ||
			   n.gloss().contains(m.gloss())) 
			    continue;
			for(String s : n.text()){
			    if(m.headWord().equalsIgnoreCase(s) &&
			       !m.headToken().nerTag().equals("O") &&
			       m.headToken().nerTag().equals(n.headToken().nerTag())){
				merge(a,b);
				break loop;
			    }
			}
		    }
		}
	    }
	}
    }

    private void matchAcronym(Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.equals(n)) continue;
			if(!m.parse.getLabel().equals("NNP") ||
			   !n.parse.getLabel().equals("NNP"))  continue;
			if(m.gloss().equalsIgnoreCase(getAcronym(n.text(), 1)) ||
			   n.gloss().equalsIgnoreCase(getAcronym(m.text(), 1))){
			    System.out.println("Reached here");
			    merge(a,b);
			    break loop;
			}

			if(m.gloss().equalsIgnoreCase(getAcronym(n.text(), 0)) ||
			   n.gloss().equalsIgnoreCase(getAcronym(m.text(), 0))){
			    System.out.println("Reached here");
			    merge(a,b);
			    break loop;
			}
		    }
		}
	    }
	}
    }

    private String getAcronym(List<String> words, int type){
	String acro = "";
	for(String word : words){
	    if(type == 0)
		acro += word.charAt(0);
	    else
		acro += ("."+word.charAt(0));
	}
	System.out.println(acro);
	return acro;
    }


    private void predicateNominative(Document doc, Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.equals(n)) continue;
			if(doc.indexOfSentence(m.sentence) !=
			   doc.indexOfSentence(n.sentence))
			    continue;
			if(m.sentence.gloss().contains(m.gloss()+" is "+n.gloss())){ 
			    merge(a,b);
			    break loop;
			}
		    }
		}
	    }
	}
    }

    private Tree<String> searchSubTree(Tree<String> parent, Tree<String> child){
	for (Tree<String> tree : parent.getChildren()){
	    if (tree.equals(child))
		return parent;
	    Tree<String> temp = searchSubTree(tree, child);
	    if (temp != null) return temp;
	}
	return null;
    }

    private void matchAppositives(Document doc, Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.equals(n)) continue;
			if(!m.parse.getLabel().equals("NP") ||
			   !n.parse.getLabel().equals("NP"))  continue;
			if(doc.indexOfSentence(m.sentence) !=
			   doc.indexOfSentence(n.sentence))
			    continue;
			Tree<String> subTree =
			    searchSubTree(m.sentence.parse, m.parse);
			if (subTree == null ||
			    !subTree.getLabel().equals("NP") ||
			    subTree.getChildren().size() < 3) continue;
			if (!subTree.getChildren().get(0).equals(m.parse)
			    ||
			    !subTree.getChildren().get(2).equals(n.parse)) continue;
			// // if(doc.indexOfMention(m)-doc.indexOfMention(n) ==
			// //    1){ //&&
			// //    //doc.getMentions().get(doc.indexOfMention(n) + 1).headToken().posTag().equals(",")){
			if(m.sentence.gloss().contains(m.gloss()+" , "+n.gloss())){
			    // Wrong implementation. Make sure parent is
			    // NN too, and there are no CC's in the expansion
			    System.out.println(m.sentence.gloss());
			    merge(a,b);
			    break loop;
			}
		    }
		}
	    }
	}
    }

    private void matchSpeaker(Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.equals(n)) continue;
			Pronoun p = Pronoun.valueOrNull(m.gloss().toUpperCase().replaceAll(" ", "_"));
			if(p == null) continue;
			if(!m.headToken().isQuoted()) continue;	
			if(m.headToken().speaker().equals(n.gloss()) &&
			   (p.speaker == Pronoun.Speaker.FIRST_PERSON)){
			    merge(a,b);
			    break loop;
			}
		    }
		}
	    }
	}
    }
    
    private void matchStrictHead(Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.equals(n)) continue;
			if(m.headWord().equalsIgnoreCase(n.headWord())){
			    if(modifierTest(m,n) && withinTest(m,n)){
				merge(a,b);
				break loop;
			    }
			}
		    }
		}
	    }
	}
    }

    private boolean modifierTest(Mention m, Mention n){
	Set<String> modifiers = new HashSet<String>();
	for(int i = m.beginIndexInclusive; i < m.endIndexExclusive; i++) {
	    if(m.sentence.posTags.get(i).contains("NN") ||
	       m.sentence.posTags.get(i).contains("JJ"))
		modifiers.add(m.sentence.words.get(i));
	}
	for(int i = n.beginIndexInclusive; i < n.endIndexExclusive; i++) {
	    if(n.sentence.posTags.get(i).contains("NN") ||
	       n.sentence.posTags.get(i).contains("JJ"))
		modifiers.remove(n.sentence.words.get(i));
	}
	
	return modifiers.size()==0 ? true : false;
    }

    private boolean withinTest(Mention m, Mention n){
	if((m.gloss().contains(n.gloss()) &&
	    n.parse.getLabel().equals("NP")) ||
	   (n.gloss().contains(m.gloss()) && 
	    m.parse.getLabel().equals("NP"))) 
	    return false;

	return true;
    }
		
    private void pronounProcessing(Document doc, Set<Set<Mention>> mentionClusters){
	for(Set<Mention> a : mentionClusters){
	    for(Set<Mention> b : mentionClusters){
		if(a.equals(b)) continue;
		loop: for(Mention m : a){
		    for(Mention n : b){
			if(m.equals(n)) continue;
			if(!Pronoun.isSomePronoun(m.gloss())) continue;
			if(!n.headToken().isNoun() && !Pronoun.isSomePronoun(n.gloss())) continue;
			int distance = doc.indexOfSentence(m.sentence) - doc.indexOfSentence(n.sentence);
			if(distance != 0) continue;
			if(m.beginIndexInclusive < n.endIndexExclusive) continue;
			Pronoun mp = Pronoun.valueOrNull(m.gloss().toUpperCase().replaceAll(" ", "_"));
			Pronoun np = Pronoun.valueOrNull(n.gloss().toUpperCase().replaceAll(" ", "_"));
			// Gender test
			Pair<Boolean, Boolean> gender =
			    Util.haveGenderAndAreSameGender(m, n);
			if(gender.getFirst() && !gender.getSecond()) continue;
			// Number test
			Pair<Boolean, Boolean> number =
			    Util.haveNumberAndAreSameNumber(m, n);
			if(number.getFirst() && !number.getSecond())
			    continue;	
			// System.out.println(n.gloss());
			// System.out.println(Pronoun.isSomePronoun(n.gloss()));
			// System.out.println(mp.speaker);
			// System.out.println(Pronoun.valueOrNull(n.gloss()));
			if (mp != null && np != null && mp.speaker !=
			    np.speaker && !n.headToken().isQuoted() &&
			    !m.headToken().isQuoted())
			    continue;			
			if(//m.headToken().nerTag().equals("O") ||
			   !m.headToken().nerTag().equals(n.headToken().nerTag()))
			    continue;
			    
			merge(a,b);
			break loop;
		    }
		}
	    }
	}
    }




	
}

