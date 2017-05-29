package de.uni_koeln.spinfo.classification.core.featureEngineering.featureSelection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.maxgarfinkel.suffixTree.Edge;
import com.maxgarfinkel.suffixTree.Node;
import com.maxgarfinkel.suffixTree.SuffixTree;

import de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;
import de.uni_koeln.spinfo.classification.core.featureEngineering.FeatureUnitTokenizer;

public class SuffixTreeFeatureGenerator {

	
	FeatureUnitTokenizer tokenizer = new FeatureUnitTokenizer();
	
	
	public List<ClassifyUnit> getSuffixTreeFreatures(List<ClassifyUnit> cus){
		//buid Suffixtree
		SuffixTree<String,List<String>> tree = new SuffixTree<String, List<String>>();
		for (ClassifyUnit cu : cus) {
			tree.add(cu.getFeatureUnits(), cu.getID());			
		}		
		//get interesting Edges (edges with nodes with more than one visitor)
		List<Edge<String,List<String>>> interestingEdges = getInterestingEdges(tree.getRoot());
		
		
		//get visitors of edge(-strings)
		Map<String, Set<UUID>> visitors = new HashMap<String, Set<UUID>>();
		for (Edge<String, List<String>> edge : interestingEdges) {
			Set<UUID> ids = getAllVisitors(edge.getTerminal());
			if(ids.size()>1){
				visitors.put(edge.getStringFromRoot(), ids);
			}
			
		}		
		//set featureUnits (= visited strings in suffixtree that appear in cu's content )
		for (ClassifyUnit cu : cus) {	
			cu.setFeatureUnits(getFeatureUnits(cu, visitors.keySet()));	
		}	
		return cus;	
	}

	
	private List<String> getFeatureUnits(ClassifyUnit cu, Set<String> suffixes){
		StringBuffer fullContent = new StringBuffer();
		fullContent.append(" ");
		for (String fu : cu.getFeatureUnits()) {
			fullContent.append(fu+" ");
		}
		fullContent.append(" ");
		Set<String> fus = new HashSet<String>(cu.getFeatureUnits());
		for (String string : suffixes) {
			if(string.length() <1){
				continue;
			}
			String currentContent = new String(fullContent.toString());	
			while(currentContent.contains(" "+string+" ")){
				fus.add(string);
				currentContent = currentContent.replaceFirst(string+" ", "");
			}
		}
		List<String> toReturn = new ArrayList<String>(fus);
		return toReturn;
	}





	private Set<UUID> getAllVisitors(Node<String, List<String>> node) {
		Set<UUID> ids = new HashSet<UUID>();
		Collection<Edge<String, List<String>>> edges = node.getEdges();
		List<Edge<String, List<String>>> toDo = new ArrayList<Edge<String, List<String>>>();
		for (Edge<String, List<String>> edge : edges) {
			String s = edge.toString();
			int start = s.indexOf("$");
			if (start >= 0) {
				ids.add(  UUID.fromString(   s.substring(start+1).replace(",", "").trim()     )   );
			} else {
				toDo.add(edge);
			}
		}
		if (toDo.size() > 0) {
			for (Edge<String, List<String>> edge : toDo) {
				getAllVistors(edge.getTerminal(), ids);
			}
		}
		return ids;
	}


	private Set<UUID> getAllVistors(Node<String, List<String>> node,
			Set<UUID> ids) {
		Collection<Edge<String, List<String>>> edges = node.getEdges();
		List<Edge<String, List<String>>> toDo = new ArrayList<Edge<String, List<String>>>();
		for (Edge<String, List<String>> edge : edges) {
			String s = edge.toString();
			int start = s.indexOf("$");
			if (start >= 0) {
				ids.add(  UUID.fromString(   s.substring(start+1).replace(",", "").trim()     )   );
			} else {
				toDo.add(edge);
			}
		}
		if (toDo.size() > 0) {
			for (Edge<String, List<String>> edge : toDo) {
				getAllVistors(edge.getTerminal(), ids);
			}
		}
		return ids;
		
	}


	private List<Edge<String, List<String>>> getInterestingEdges(
			Node<String, List<String>> root) {
		List<Edge<String, List<String>>> interestingEdges = new ArrayList<Edge<String, List<String>>>();
		Collection<Edge<String, List<String>>> edges = root.getEdges();
		for (Edge<String, List<String>> edge : edges) {
			if (edge.isTerminating()) {
				interestingEdges.add(edge);
				getInterestingEdges(edge.getTerminal(), interestingEdges);
			}
		}
		return interestingEdges;
	}


	private void getInterestingEdges(Node<String, List<String>> node,
			List<Edge<String, List<String>>> interestingEdges) {
		Collection<Edge<String, List<String>>> edges = node.getEdges();

		for (Edge<String, List<String>> edge : edges) {
			if (edge.isTerminating()) {
				interestingEdges.add(edge);
				getInterestingEdges(edge.getTerminal(), interestingEdges);
			}

		}
	}
}
