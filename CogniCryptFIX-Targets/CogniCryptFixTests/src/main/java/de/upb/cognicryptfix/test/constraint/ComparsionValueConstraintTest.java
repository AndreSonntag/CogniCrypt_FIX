package de.upb.cognicryptfix.test.constraint;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.PBEKeySpec;

public class ComparsionValueConstraintTest {

	public void test01() throws Exception{
		byte[] salt = new byte[100];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		char[] password = { '2', 'd', '#', 'q', 'P' };	
		int x = 10;

		int iterations = x;
		int keyLength = 20;
		PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
		spec.clearPassword();
		
		keyLength = 10;
		int y = keyLength;
		
		
		
	}
	
	public void test02() throws Exception{
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		Cipher c = Cipher.getInstance("AES");
		c.init(Cipher.ENCRYPT_MODE, keyGen.generateKey());
		c.doFinal(new byte[16]);
	}
}
