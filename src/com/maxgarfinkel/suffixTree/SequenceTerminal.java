package com.maxgarfinkel.suffixTree;

import java.util.UUID;

/**
 * Represents the terminating item of a sequence.
 * 
 * @author maxgarfinkel
 * 
 */
class SequenceTerminal<S> {
	
	UUID id;

	private final S sequence;
	
	SequenceTerminal(S sequence, UUID id){
		this.sequence = sequence;
		this.id = id;
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object o) {
		if(o == null || o.getClass() != this.getClass())
			return false;
		return ((SequenceTerminal<S>)o).sequence.equals(this.sequence);
	}
	
	public int hashCode(){
		return sequence.hashCode();	
	}

	@Override
	public String toString() {
		//return "$"+sequence.toString()+"$";
		return "$"+id.toString();
	}
	
	public S getSequence(){
		return sequence;
	}
	
	public UUID getId(){
		return id;
	}
	


}
