package quenfo.rdf;

import java.util.HashSet;
import java.util.Set;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFormatter;
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
//		model = model.read("information_extraction/data/competences/esco/transversal_skills_collection.ttl");
		model = model.read("information_extraction/data/competences/esco/esco_v1.0.3.ttl");
		
//		String queryString = "Select ?subjects Where { ?subjects	<http://www.w3.org/2004/02/skos/core#prefLabel>	\"http://www.w3.org/2004/02/skos/core#Concept\" }";
		String queryString = "Select ?subjects ?objects Where { ?subjects	<http://www.w3.org/2004/02/skos/core#prefLabel>	?objects }";
		
		Query query = QueryFactory.create(queryString);
		QueryExecution qe = QueryExecutionFactory.create(query, model);
		ResultSet results = qe.execSelect();
		
		ResultSetFormatter.out(System.out, results, query);
		qe.close();
		System.exit(0);
		
		Property prop = model.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");
		Property prefLabel = model.createProperty("http://www.w3.org/2004/02/skos/core#prefLabel");
		Resource res = model.createResource("http://www.w3.org/2004/02/skos/core#Concept");
		Selector selector = new SimpleSelector(null, prop, (RDFNode)res);
		StmtIterator iter = model.listStatements(selector);
		
		
		Set<RDFNode> objects = new HashSet<RDFNode>();
		Set<Resource> subjects = new HashSet<Resource>();
		
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object
		    objects.add(object);
		    subjects.add(subject);

		    Statement labels = subject.getProperty(prefLabel);
		    String obj = labels.getObject().toString();
		    if(obj.contains("@de"))
		    	System.out.println(obj);
  
		}
		
//		for(Resource subject : subjects) {
//			System.out.println(subject.toString());
//		}
//		for (RDFNode o : objects) {
//			System.out.println(o.toString());
//		}
	}

}
