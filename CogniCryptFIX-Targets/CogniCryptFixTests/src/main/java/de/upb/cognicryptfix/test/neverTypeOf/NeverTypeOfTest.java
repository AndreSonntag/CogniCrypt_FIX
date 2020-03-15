package de.upb.cognicryptfix.test.neverTypeOf;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;

public class NeverTypeOfTest {

	private void generatePBEKeySpec() throws Exception{
		byte[] salt = new byte[100];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		
		String password = "2d#qP";				
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
		spec.clearPassword();
	}
}
