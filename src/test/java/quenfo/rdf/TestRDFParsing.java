package quenfo.rdf;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Selector;
import org.apache.jena.rdf.model.SimpleSelector;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.junit.Test;

public class TestRDFParsing {

	@Test
	public void test() {
		Model model = ModelFactory.createDefaultModel();
		model = model.read("esco_v1.0.3.ttl");
		
		Property prop = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Resource res = model.createResource("http://data.europa.eu/esco/skill/238343b1-7b51-42b3-a9ed-cf24d3a236e7");
		Selector selector = new SimpleSelector(null, prop, (RDFNode)null);
		StmtIterator iter = model.listStatements(selector);
		
		Set<RDFNode> objects = new HashSet<RDFNode>();
		
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object
		    objects.add(object);
//		    if(!object.isURIResource()) {
		    	
//		    	String value = object.toString();
////		    	if(value.contains("@de")) {
//		    		System.out.print(subject.toString());
//				    System.out.print(" " + predicate.toString() + " ");
//				    if (object instanceof Resource) {
//				       System.out.print(object.toString());
//				    } else {
//				        // object is a literal
//				        System.out.print(" \"" + object.toString() + "\"");
//				    }
//
//				    System.out.println(" .");
//		    	}
		    	
		    	
//		    }   
		} 
		for (RDFNode o : objects) {
			System.out.println(o.toString());
		}
	}

}
