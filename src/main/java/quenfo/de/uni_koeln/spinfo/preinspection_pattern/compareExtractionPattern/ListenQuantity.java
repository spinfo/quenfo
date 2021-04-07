package quenfo.de.uni_koeln.spinfo.preinspection_pattern.compareExtractionPattern;

import java.io.File;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import quenfo.de.uni_koeln.spinfo.preinspection_pattern.io.IO;

public class ListenQuantity {
    /**
     * Lists the number of extractions of a pattern in a map and exports it to a
     * .txt file.
     * 
     * @author christine schaefer
     *
     */

    // Erstellen einer Map: String = Pattern, Integer = Anzahl, wie häufig Pattern
    // vorkommt
    private static Map<String, Integer> patternQuantity = new HashMap<String, Integer>();

    // Pfad zur genutzten Datenbank mit Extraktionen
    private static String dbPath = "input\\text_kernel_schaefer_ex.db";

    private static String outputPath = "output\\patternQuantity.txt";

    // Liste mit allen genutzten Mustern (auch Dopplungen)
    private static List<String> usedPattern = new ArrayList<String>();

    public static void main(String[] args) throws Exception {

        // Verbindung zur Input-DB
        Connection inputConnection = null;
        if (!new File(dbPath).exists()) {
            System.out
                    .println("Input-DB '" + dbPath + "' does not exist\nPlease change configuration and start again.");
            System.exit(0);
        } else {
            inputConnection = IO.connect(dbPath);
            System.out.println("Connection to Database is ready.");
        }

        usedPattern = IO.readPatternToList(inputConnection);

        // objects for sorting
        ValueComparator bvc = new ValueComparator(patternQuantity);
        TreeMap<String, Integer> sorted_map = new TreeMap<String, Integer>(bvc);

        fillMap(usedPattern);
        sorted_map.putAll(patternQuantity);
        System.out.println(sorted_map);

        // export of the sorted map to a .txt file
        IO.writeQuantityToTxt(outputPath, sorted_map);

        // export of the map patternQuantity to a .csv file
        IO.writeQuantityMapToCsv("output\\patternQuantity.csv", patternQuantity);

    }

    /**
     * Fills the map with the used patterns. If a pattern occurs several times, the
     * value is incremented.
     * 
     * @param usedPattern
     */
    private static void fillMap(List<String> usedPattern) {
        for (String pattern : usedPattern) {
            if (patternQuantity.containsKey(pattern)) {
                patternQuantity.put(pattern, patternQuantity.get(pattern) + 1);
            } else {
                patternQuantity.put(pattern, 1);
            }
        }
    }
}
