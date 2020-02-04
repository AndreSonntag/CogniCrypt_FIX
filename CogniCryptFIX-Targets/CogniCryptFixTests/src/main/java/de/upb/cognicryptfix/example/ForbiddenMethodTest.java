package de.upb.cognicryptfix.example;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;

public class ForbiddenMethodTest {

	
	private void generateKey() throws NoSuchAlgorithmException, GeneralSecurityException {
		char[] passwordCharArry = {'a','b','c'};
		PBEKeySpec pBEKeySpec = new PBEKeySpec(passwordCharArry);
		
		
		
		byte[] salt = new byte[16];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		PBEKeySpec secureConstructor = new PBEKeySpec(passwordCharArry, salt, 10000, 128);
	}

}
