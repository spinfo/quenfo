package quenfo.de.uni_koeln.spinfo.preinspection_pattern.compareExtractionPattern;

import java.util.Comparator;
import java.util.Map;

/**
 * 
 * @author christine schaefer
 * 
 * @implNote Compare the values of a map and sorts the map according to these
 *           values. Source:
 *           https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values
 *
 */

public class ValueComparator implements Comparator<String> {
    Map<String, Integer> base;

    public ValueComparator(Map<String, Integer> base) {
        this.base = base;
    }

    /**
     * @see interface Comparator<T>
     * 
     * @param a, b
     * @return int
     * 
     * @implNote This comparator imposes orderings that are inconsistent with
     *           equals.
     */

    public int compare(String a, String b) {
        if (base.get(a) >= base.get(b)) {
            return -1;
        } else {
            return 1;
        } // returning 0 would merge keys
    }
}
