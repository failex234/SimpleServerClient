package com.blogspot.debukkitsblog.Util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Crypter {

	/**
	 * <p>Encrypts an object in a quite bad and easy-to-crack way<br>
	 * into an CryptedObject using passwword.</p>
	 * @param o The object to encrypt
	 * @param password The password to use for encryption (and for decryption later)
	 * @return the encrypted CryptedObject
	 */
	public static CryptedObject encrypt(Object o, String password) {
		byte[] bytes = serialize(o);
		int[] ints = new int[bytes.length];
		password = lengthenPassword(password, bytes.length);
		
		for(int i = 0; i < bytes.length; i++) {
			int intRep = Integer.parseInt(new Byte(bytes[i]).toString());
			int intEnc = intRep + password.charAt(i);
			ints[i] = intEnc;
		}
		
		return new CryptedObject(ints);
	}
	
	/**
	 * <p>Decrypts an CryptedObject in a quite bad and easy-to-crack way<br>
	 * into an object using password.</p>
	 * @param co The CryptedObject to decrypt
	 * @param password The password to use for decryption
	 * @return the decrypted object or null if password is incorrect
	 */
	public static Object decrypt(CryptedObject co, String password){
		int[] ints = co.getIntegers();
		byte[] bytes = new byte[ints.length];
		
		try{
			password = lengthenPassword(password, bytes.length);
			
			for(int i = 0; i < ints.length; i++) {
				int intDec = ints[i] - password.charAt(i);
				bytes[i] = Byte.parseByte(String.valueOf(intDec));
			}
		} catch(Exception e){ }
		
		Object o = deserialize(bytes);
		if(o == null){
			System.err.println("Decryption failed. Password probably incorrect.");
		}
		return o;
	}
	
	private static String lengthenPassword(String password, int length){
		while(password.length() < length){
			password += password;
		}
		while(password.length() > length){
			password = password.substring(0, password.length() - 1);
		}
		return password;
	}
	
	private static byte[] serialize(Object obj) {
	    try{
	    	ByteArrayOutputStream out = new ByteArrayOutputStream();
	    	ObjectOutputStream os = new ObjectOutputStream(out);
	    	os.writeObject(obj);
	    	return out.toByteArray();
	    } catch(Exception e){
	    	e.printStackTrace();
	    }
	    return null;
	}
	
	private static Object deserialize(byte[] data) {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(data);
			ObjectInputStream is = new ObjectInputStream(in);
			return is.readObject();
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return null;
	}
	
}
