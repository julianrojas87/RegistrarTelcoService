package org.telcomp.events;

import java.util.HashMap;
import java.util.Random;
import java.io.Serializable;

public final class EndRegistrarTelcoServiceEvent implements Serializable {

	private final long id;
	private static final long serialVersionUID = 1L;
	private String userID;
	private String operation;

	public EndRegistrarTelcoServiceEvent(HashMap<String, ?> hashMap) {
		id = new Random().nextLong() ^ System.currentTimeMillis();
		this.userID = (String) hashMap.get("userID");
		this.operation = (String) hashMap.get("operation");
	}

	public boolean equals(Object o) {
		if (o == this) return true;
		if (o == null) return false;
		return (o instanceof EndRegistrarTelcoServiceEvent) && ((EndRegistrarTelcoServiceEvent)o).id == id;
	}
	
	public int hashCode() {
		return (int) id;
	}
	
	public String getUserID(){
		return this.userID;
	}
	
	public String getOperation(){
		return this.operation;
	}
	
	public String toString() {
		return "endRegistrarEvent[" + hashCode() + "]";
	}
}
