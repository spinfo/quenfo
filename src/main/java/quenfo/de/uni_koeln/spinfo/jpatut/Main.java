package quenfo.de.uni_koeln.spinfo.jpatut;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import quenfo.de.uni_koeln.spinfo.classification.core.data.ClassifyUnit;

public class Main {
	
	
	public static void main(String[] args) throws ClassNotFoundException, SQLException {
		
		
		DatabaseTransformer dt = new DatabaseTransformer();
		
		List<JobAd> classifyUnits = readClassifyUnits();
		System.out.println("Read " + classifyUnits.size() + " ClassifyUnits");
//		dt.transformIntoDerby(classifyUnits);
		
		dt.readDerby();
		
		
		
		
	}
	

	
	
	
	public static List<JobAd> readClassifyUnits() throws SQLException, ClassNotFoundException {
		Connection connection;
		Class.forName("org.sqlite.JDBC");
		connection = DriverManager.getConnection("jdbc:sqlite:C:/sqlite/classification/CorrectableParagraphs_textkernel.db");

		connection.setAutoCommit(false);
		
		String query = "SELECT * FROM ClassifiedParagraphs";
		ResultSet result;
		PreparedStatement prepStmt = connection.prepareStatement(query);
		
		result = prepStmt.executeQuery();
		List<JobAd> classifyUnits = new ArrayList<>();
		ClassifyUnit classifyUnit;
		JobAd jobAd;
		
		while (result.next()) {
			
			int class1 = result.getInt("ClassOne");
			int class2 = result.getInt("ClassTwo");
			int class3 = result.getInt("ClassThree");
			int class4 = result.getInt("ClassFour");
			
			boolean[] classIDs = new boolean[4];
			classIDs[0] = (class1 == 1);
			classIDs[1] = (class2 == 1);
			classIDs[2] = (class3 == 1);
			classIDs[3] = (class4 == 1);
			
			
			//TODO JB: multiclass
			
//			classifyUnit = new JASCClassifyUnit(result.getString("Text"), result.getInt("Jahrgang"),
//					result.getInt("ZEILENNR"));
//			((JASCClassifyUnit) classifyUnit).setTableID(result.getInt("ID"));
//			((ZoneClassifyUnit) classifyUnit).setClassIDs(classIDs);
			
			jobAd = new JobAd(result.getString("Text"), classIDs);
			classifyUnits.add(jobAd);
		}
		result.close();
		prepStmt.close();
		connection.commit();
		
		
		return classifyUnits;
	}
	

}
