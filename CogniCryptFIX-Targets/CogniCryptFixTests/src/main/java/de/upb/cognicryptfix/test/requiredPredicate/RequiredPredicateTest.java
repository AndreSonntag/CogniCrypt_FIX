package de.upb.cognicryptfix.test.requiredPredicate;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class RequiredPredicateTest {

	/* works */
	private void test01() {
		char[] password = { '2', 'd', '#', 'q', 'P' };
		byte[] salt = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 10000, 128);
		pbeKeySpec.clearPassword();
	}

	/* works */
	private void test02() {
		byte[] iv = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
	}

	/* works */
	private void test03() {
		byte[] iv = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		IvParameterSpec ivParamSpec = new IvParameterSpec(iv, 128, 129);
	}

	/* works */
	private void test04() {
		byte[] keyMaterial = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		SecretKeySpec spec = new SecretKeySpec(keyMaterial, "AES");
	}

	private void test05() throws NoSuchAlgorithmException, InvalidKeySpecException {
		
		char[] password = { '2', 'd', '#', 'q', 'P' };
		byte[] salt = new byte[16];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 10000, 128);
		//pbeKeySpec.clearPassword();	missing
		KeyFactory factory = KeyFactory.getInstance("RSA");
		PrivateKey privKey = factory.generatePrivate(pbeKeySpec);
	}
	
	
	/* problem --> no error */
//	private void _test06() throws NoSuchAlgorithmException, InvalidKeySpecException {
//		SecureRandom sr = SecureRandom.getInstance("insecure");
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//		keyGen.init(sr);
//		keyGen.generateKey();
//	}
	
	/*-
	 *  Exception in thread "main" java.lang.StackOverflowError
	 * 	at wpds.impl.WeightedPAutomaton.addWeightForTransition(WeightedPAutomaton.java:261)
	 */
//	private void _test05() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {		
//		byte[] keyMaterial = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
//		SecretKeySpec spec = new SecretKeySpec(keyMaterial, "AES");
//		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
//		c.init(Cipher.ENCRYPT_MODE, spec);
//		byte[] message = "HelloWorld".getBytes();
//		byte[] cipherText = c.doFinal(message);
//	}
	

	
	
	

}
