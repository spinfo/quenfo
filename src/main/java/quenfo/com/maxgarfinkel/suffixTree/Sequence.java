package quenfo.com.maxgarfinkel.suffixTree;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

/**
 * Represents a sequence of items. This plays the part of the string in a non
 * generic suffix tree implementation. This object automatically appends a
 * terminating item to the end of the instance which is included in all
 * operations.
 * 
 * @author Max Garfinkel
 * 
 * @param <I,S>
 */
public class Sequence<I, S extends Iterable<I>> implements Iterable<Object> {

	private List<Object> masterSequence = new ArrayList<Object>();
	Logger logger = Logger.getLogger(getClass());

	Sequence(){
	}
	
	/**
	 * Initialize the sequence.
	 * 
	 * @param sequence
	 */
	Sequence(S sequence, UUID id) {
		for(Object item : sequence)
			masterSequence.add(item);
		SequenceTerminal<S> sequenceTerminal = new SequenceTerminal<S>(sequence, id);
		masterSequence.add(sequenceTerminal);
	}

	/**
	 * Retrieve the item at the position specified by index.
	 * 
	 * @param index
	 * @return
	 */
	Object getItem(int index) {
		return masterSequence.get(index);
	}

	/**
	 * Adds a Sequence to the suffix tree. 
	 * @param sequence
	 */
	void add(S sequence, UUID id){
		for(I item : sequence){
			masterSequence.add(item);
		}
		SequenceTerminal<S> terminal = new SequenceTerminal<S>(sequence, id);
		masterSequence.add(terminal);
	}
	
	/**
	 * Retrieves an iterator for the sequence.
	 */
	public Iterator<Object> iterator() {
		return new Iterator<Object>() {

			int currentPosition = 0;

			public boolean hasNext() {
				return masterSequence.size() > currentPosition;
			}

			public Object next() {
				if (currentPosition <= masterSequence.size())
					return masterSequence.get(currentPosition++);
				else {
					return null;
				}
			}

			public void remove() {
				throw new UnsupportedOperationException(
						"Remove is not supported.");

			}

		};
	}
	
	int getLength(){
		return masterSequence.size();
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder("Sequence = [");
		for(Object i : masterSequence){
			sb.append(i).append(", ");
		}
		sb.append("]");
		return sb.toString();
	}
	
}
