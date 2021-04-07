package quenfo.de.uni_koeln.spinfo.preinspection_pattern.featureWeight;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import quenfo.de.uni_koeln.spinfo.preinspection_pattern.io.IO;

public class Evaluation {
    // Pfade zu Dateien mit validierten Extraktionen (Goldstandard)
    public static String competencesPath = "input\\competences.txt";
    public static String noCompetencesPath = "input\\noCompetences.txt";

    // Pfad zur genutzten Datenbank mit Extraktionen
    public static String dbPath = "input\\text_kernel_schaefer_ex.db";

    // Liste mit extrahierten Strings
    public static List<String> competences = new ArrayList<String>();
    public static List<String> noCompetences = new ArrayList<String>();

    // Set mit allen genutzten Mustern/ermittelten Extraktionen
    public static Set<String> usedPattern = new TreeSet<String>();
    public static Set<String> extractions = new TreeSet<String>();

    // Map mit key=pattern/seed und value=conf
    public static Map<String, Double> confPattern = new HashMap<String, Double>();
    public static Map<String, Double> confSeed = new HashMap<String, Double>();

    public static Map<String, Integer> tpPattern = new HashMap<String, Integer>();
    public static Map<String, Integer> fpPattern = new HashMap<String, Integer>();

    public static void main(String[] args) throws Exception {

        competences = IO.readCompetences(competencesPath);
        noCompetences = IO.readCompetences(noCompetencesPath);

        // System.out.println(competences.size());

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

        usedPattern = IO.readPatternToSet(inputConnection);
        System.out.println(usedPattern);

        confPattern = evaluatePattern(usedPattern, competences, noCompetences, inputConnection);
        System.out.println(confPattern);

        IO.writeMapToCsv("output\\tpPattern.csv", tpPattern);
        IO.writeMapToCsv("output\\fpPattern.csv", fpPattern);

        extractions = IO.readExtractionToSet(inputConnection);
        System.out.println(extractions);

        // Muster mit Confidence in .csv Datei
        IO.writeConfidenceMapToCsv("output\\patternConfidence.csv", confPattern);

        // Muster mit Confidence in .txt Datei
        IO.writeConfToTxt("output\\patternConfidence.txt", confPattern);

        confSeed = evaluateSeed(extractions, inputConnection, confPattern);
        System.out.println(confSeed);

        IO.writeConfToTxt("output\\seedConfidence.txt", confSeed);

    }

    /**
     * Computes the confidence of the used patterns: Conf(P) = P.pos / (P.pos +
     * P.neg)
     * 
     * @param usedPattern
     * @param competences
     * @param connection
     * 
     * @return pattern-confidence
     * @throws Exception
     */
    public static Map<String, Double> evaluatePattern(Set<String> usedPattern, List<String> competences,
            List<String> noCompetences, Connection connection) throws Exception {
        // Was ist, wenn Extraktion noch nicht in der Liste vorhanden, aber trotzdem
        // richtig extrahiert ist?
        // Die Liste kann ja kaum vollst�ndig sein. In diesem Fall: manuelle
        // Berarbeitung?

        Map<String, Double> confP = new HashMap<String, Double>();

        // Verbindung zur Datenbank wird genutzt um Anfrage zu stellen
        connection.setAutoCommit(false);
        Statement stmt = connection.createStatement();

        // Iteration über jedes genutzte Muster, das in der Datenbank aufgelistet ist
        for (String p : usedPattern) {

            int tp = 0;
            int fp = 0;

            int falseExtractions = 0;

            // Liste mit den Extraktionen des aktuellen Musters
            List<String> extractions = new ArrayList<String>();

            // Abfrage der Extraktionen
            String query = "SELECT lemmaExpression FROM JOINTABLE WHERE (SELECT patternString = '" + p + "')";

            // Anfrage läuft über DB
            ResultSet result = stmt.executeQuery(query);

            // Extraktionen werden der Liste hinzugefügt
            while (result.next()) {
                String comp = result.getString(1);
                extractions.add(comp);
            }

            // Vergleich der Extraktionen mit den validierten Kompetenzen
            for (String e : extractions) {
                if ((noCompetences.contains(e))) {
                    falseExtractions++;
                }
                if ((competences.contains(e))) {
                    tp++; // wenn Strings nicht übereinstimmen, wird fp hochgezählt
                } else {
                    fp++; // ansonsten tp
                }
            }

            // Hinzufügen des Musters mit ermittelten Confidence-Wert
            confP.put(p, getPatternConfidence(tp, fp));

            tpPattern.put(p, tp);
            fpPattern.put(p, falseExtractions);

            // TP und FP für neues Pattern wieder null setzen
            tp = 0;
            fp = 0;

            // Verbindung schließen
            stmt.close();
            connection.commit();
        }
        return confP;

        // TODO
        // Hinzufügen des Conf in bestehende .txt Datei: String-Matching des Namen des
        // Musters? Aber wie dann Conf ändern? txt Format ist nicht so ideal/nicht
        // sicher, wie ich die bereits existierenden Strukturen von Geduldig nutzen kann
    }

    public static Map<String, Double> evaluateSeed(Set<String> extractions, Connection connection,
            Map<String, Double> confP) throws SQLException {
        // Conf(seed) = 1 - <Produkt>(1-Conf(P))

        Map<String, Double> confS = new HashMap<String, Double>();

        // Verbindung zur Datenbank wird genutzt um Anfrage zu stellen
        connection.setAutoCommit(false);
        Statement stmt = connection.createStatement();

        System.out.println("Verbindung hergestellt.");

        for (String e : extractions) {

            System.err.println(e);
            Set<String> usedPattern = new TreeSet<String>();

            // Abfragen
            String query = "SELECT patternString FROM JOINTABLE WHERE (SELECT lemmaExpression = '" + e + "')";

            System.out.println("Anfrage gestellt.");
            // Anfrage läuft über DB
            ResultSet result = stmt.executeQuery(query);

            System.out.println("Anfrage hat Datenbank erreicht.");
            while (result.next()) {
                String comp = result.getString(1);
                usedPattern.add(comp);

                System.out.println("Genutzte Muster hinzugefügt.");
            }
            System.out.println(usedPattern);

            confS.put(e, getSeedsConfidence(usedPattern, confP));
            System.out.println("Map gefüllt.");

            // Verbindung schließen
            stmt.close();
            connection.commit();
        }
        System.out.println("Fertig.");
        return confS;
    }

    public static double getPatternConfidence(int tp, int fp) {
        double conf = ((double) tp / (tp + fp));
        return conf;
    }

    public static double getSeedsConfidence(Set<String> usedPattern, Map<String, Double> patternConf) {
        double conf = 0d;
        double product = 0d;

        List<Double> confValue = new ArrayList<Double>();

        for (String p : usedPattern) {
            System.out.println(p);
            if (patternConf.containsKey(p)) {
                confValue.add(1 - patternConf.get(p)); // (1 - Conf(P)) = Wahrscheinlichkeit f�r die Fehlerhaftigkeit
            }
        }
        System.out.println(confValue);

        for (int i = 1; i <= confValue.size(); i++) {
            // Wie berechne ich das Produkt aller Elemente der Liste confValue?
            if (product == 0d) {
                product = confValue.get(i - 1);
            } else {
                product = product * confValue.get(i - 1);
            }
        }
        conf = 1 - product;

        return conf;
    }
}
