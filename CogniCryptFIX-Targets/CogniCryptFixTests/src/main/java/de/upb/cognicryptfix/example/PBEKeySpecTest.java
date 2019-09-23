package de.upb.cognicryptfix.example;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.PBEKeySpec;

public class PBEKeySpecTest {
	//static salt
	byte[] salt = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };

	private void generateKey(String password) throws NoSuchAlgorithmException, GeneralSecurityException {
		//too low iterations
		int iteration = 9999;
		PBEKeySpec pBEKeySpec = new PBEKeySpec(password.toCharArray(), salt, iteration, 256);
	}

}
