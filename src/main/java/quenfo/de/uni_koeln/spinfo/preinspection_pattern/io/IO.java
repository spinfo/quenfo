package quenfo.de.uni_koeln.spinfo.preinspection_pattern.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import org.supercsv.io.CsvListWriter;
import org.supercsv.io.ICsvListWriter;
import org.supercsv.prefs.CsvPreference;

/**
 * @author ChristineSchaefer
 * 
 * Includes all necessary Read-/Write- and DB-methods.
 */
public class IO {

    /**
     * Reads the extractions (competence, no competence) from .txt file.
     * 
     * @param path
     * @return extractions
     * @throws IOException
     */
    public static List<String> readCompetences(String path) throws IOException {

        BufferedReader br = new BufferedReader(new FileReader(path));

        String line;
        List<String> extractions = new ArrayList<String>();
        while ((line = br.readLine()) != null) {
            extractions.add(line);
        }
        br.close();

        return extractions;
    }

    /**
     * Creates/connects to the given database.
     * 
     * @param dbFilePath
     * @return Connection
     * @throws SQLException
     * @throws ClassNotFoundException
     * 
     * @author geduldia
     * @see IE_DBConnector.java
     */
    public static Connection connect(String dbFilePath) throws SQLException, ClassNotFoundException {
        Connection connection;
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
        return connection;
    }

    /**
     * Reads the column "patternString" from given database. Does not save
     * duplicates.
     * 
     * @param connection
     * @return used-Pattern
     * @throws SQLException
     */
    public static Set<String> readPatternToSet(Connection connection) throws SQLException {

        Set<String> toReturn = new TreeSet<String>();
        connection.setAutoCommit(false);
        String sql = "SELECT patternString FROM JOINTABLE";

        Statement stmt = connection.createStatement();
        ResultSet result = stmt.executeQuery(sql);
        while (result.next()) {
            String comp = result.getString(1);
            toReturn.add(comp);
        }
        stmt.close();
        connection.commit();
        return toReturn;
    }

    /**
     * Reads the column "patternString" from given database. Save duplicates.
     * 
     * @param connection
     * @return used-Pattern
     * @throws SQLException
     */
    public static List<String> readPatternToList(Connection connection) throws SQLException {

        List<String> toReturn = new ArrayList<String>();
        connection.setAutoCommit(false);
        String sql = "SELECT patternString FROM JOINTABLE";

        Statement stmt = connection.createStatement();
        ResultSet result = stmt.executeQuery(sql);
        while (result.next()) {
            String comp = result.getString(1);
            toReturn.add(comp);
        }
        stmt.close();
        connection.commit();
        return toReturn;
    }

    /**
     * Reads the column "lemmaExpression" from given database. Does not save
     * duplicates.
     * 
     * @param connection
     * @return extractions
     * @throws SQLException
     */
    public static Set<String> readExtractionToSet(Connection connection) throws SQLException {

        Set<String> toReturn = new TreeSet<String>();
        connection.setAutoCommit(false);
        String sql = "SELECT lemmaExpression FROM JOINTABLE";

        Statement stmt = connection.createStatement();
        ResultSet result = stmt.executeQuery(sql);
        while (result.next()) {
            String comp = result.getString(1);
            toReturn.add(comp);
        }
        stmt.close();
        connection.commit();
        return toReturn;
    }

    /**
     * @implNote Writes a map <String, Double> in a .csv file using the library
     *           "super-csv". Source: https://github.com/super-csv/super-csv
     * 
     * @see super-csv
     * 
     * @throws Exception
     */
    public static void writeConfidenceMapToCsv(String pathOutput, Map<String, Double> map) throws Exception {
        try (ICsvListWriter listWriter = new CsvListWriter(new FileWriter(pathOutput),
                CsvPreference.STANDARD_PREFERENCE)) {
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                listWriter.write(entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * @implNote Writes a map <String, Integer> in a .csv file using the library
     *           "super-csv". Source: https://github.com/super-csv/super-csv
     * 
     * @see super-csv
     * 
     * @throws Exception
     */
    public static void writeQuantityMapToCsv(String pathOutput, Map<String, Integer> map) throws Exception {
        try (ICsvListWriter listWriter = new CsvListWriter(new FileWriter(pathOutput),
                CsvPreference.STANDARD_PREFERENCE)) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                listWriter.write(entry.getKey(), entry.getValue());
            }
        }

    }

    public static void writeMapToCsv(String pathOutput, Map<String, Integer> map) throws Exception {
        try (ICsvListWriter listWriter = new CsvListWriter(new FileWriter(pathOutput),
                CsvPreference.STANDARD_PREFERENCE)) {
            for (Map.Entry<String, Integer> entry : map.entrySet()) {
                listWriter.write(entry.getKey(), entry.getValue());
            }
        }

    }

    /**
     * Writes a map <String, Integer> in a .txt file.
     * 
     * @param pathOutput
     * @param map
     * @throws IOException
     */
    public static void writeQuantityToTxt(String pathOutput, Map<String, Integer> map) throws IOException {
        FileWriter fstream = new FileWriter(pathOutput);
        BufferedWriter out = new BufferedWriter(fstream);

        Iterator<Entry<String, Integer>> it = map.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Integer> pairs = it.next();
            out.write("NAME:\t " + pairs.getKey() + " - " + pairs.getValue() + "\n");
        }
        out.close();
    }

    /**
     * Writes a map <String, Double> in a .txt file.
     * 
     * @param pathOutput
     * @param map
     * @throws IOException
     */

    public static void writeConfToTxt(String pathOutput, Map<String, Double> map) throws IOException {
        FileWriter fstream = new FileWriter(pathOutput);
        BufferedWriter out = new BufferedWriter(fstream);

        Iterator<Entry<String, Double>> it = map.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<String, Double> pairs = it.next();
            out.write("NAME:\t" + pairs.getKey().replace("[", "").replace("]", "") + "\nCONF:\t" + pairs.getValue()
                    + "\n\n");
        }
        out.close();
    }
}
