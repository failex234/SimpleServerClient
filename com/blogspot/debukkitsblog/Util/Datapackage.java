package com.blogspot.debukkitsblog.Util;

import java.io.Serializable;
import java.util.ArrayList;

public class Datapackage implements Serializable {
	
	private static final long serialVersionUID = 3194796995460883469L;
	
	ArrayList<Object> memory;
	
	/**
	 * Creats a serializable Datapackage consiting of an identifier for identification (head)<br>
	 * and lots of objects in its body.<br>
	 * Alle the data including the identifier is stores in an ArrayList of Objects,<br>
	 * where index=0 is the identifier and index>=1 is the data.
	 * @param id The identifier for later identification and separation
	 * @param o The contents of the package (lots of Objects)
	 */
	public Datapackage(String id, Object... o){		
		memory = new ArrayList<Object>();
		memory.add(id);
		for(Object current : o){
			memory.add(current);
		}
	}
	
	/**
	 * @return the identifier of the datapackage (index=0 of the corresponding ArrayList of Objects)
	 * @throws <b>IllegalArgumentException</b> if identifier is not a String
	 */
	public String id(){
		if(!(memory.get(0) instanceof String)){
			throw new IllegalArgumentException("Identifier of Datapackage is not a String");
		}		
		return (String) memory.get(0);
	}
	
	/**
	 * @param i index of the element of the corresponding ArrayList of objects to be returned
	 * @return The element of the corresponding ArrayList of objects with the given index <i>i</i>
	 */
	public Object get(int i){
		return memory.get(i);
	}
	
	/**
	 * @return the whole corresponding ArrayList of objects laying behind this class, including the identifier (index=0)
	 */
	public ArrayList<Object> open(){
		return memory;
	}
	
	/**
	 * returns the String-representation of the corresponding ArrayList of objects laying behind this class: [id, data1, data2, ...]
	 */
	@Override
	public String toString() {
		return memory.toString();
	}

}
