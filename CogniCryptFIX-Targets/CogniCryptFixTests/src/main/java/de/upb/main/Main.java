package de.upb.main;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;

public class Main {

	public static void main(String[] args) throws NoSuchAlgorithmException, DestroyFailedException {

		
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		SecretKey key = keyGen.generateKey();
		
		
		key.destroy();
	}

}
