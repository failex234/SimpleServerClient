package com.blogspot.debukkitsblog.Util;

import java.io.Serializable;

/**
 * <p>An encrypted Object consisting of nothing but an array of integers.<br>
 * Use the Crypter class and the right password for decryption.</p>
 * <p>created on April 1, 2016 in Horstmar, NRW, Germany</p>
 */
public class CryptedObject implements Serializable {
	
	private int[] ints;
	
	/**
	 * Constructs a CryptedObject consisting of
	 * @param lots of Integers in an array
	 */
	public CryptedObject(int[] ints) {
		this.ints = ints;
	}
	
	/**
	 * return The array of Integers stores in this CryptedObject
	 */
	public int[] getIntegers(){
		return ints;
	}
	
	/**
	 * Decrypts this object using a password
	 * @param password The password to use for decryption
	 * @return the decrypted object or null if password is wrong
	 */
	public Object decrypt(String password){
		return Crypter.decrypt(this, password);
	}

}