package de.upb.cognicryptfix.test.constraint;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.RSAKeyGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.PBEParameterSpec;

public class ComparsionValueConstraintTest {

//	/**
//	 * Cipher "AES" -> "AES/CBC/PKCS5Padding"
//	 * @throws Exception
//	 */
//	@SuppressWarnings("unused")
//	public void test01() throws Exception{
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//		Cipher c = Cipher.getInstance("AES");
//		c.init(Cipher.ENCRYPT_MODE, keyGen.generateKey());
//		byte[] cipherText = c.doFinal(new byte[16]);
//	}
//	
//	/**
//	 * 20 -> 128
//	 * @throws NoSuchAlgorithmException
//	 */
//	@SuppressWarnings("unused")
//	public void test02() throws NoSuchAlgorithmException {
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//		keyGen.init(20);
//		SecretKey sk = keyGen.generateKey();
//	}
//	
//	/**
//	 * 128 -> 256
//	 * @throws NoSuchAlgorithmException
//	 */
//	@SuppressWarnings("unused")
//	public void test03() throws NoSuchAlgorithmException {
//		KeyPairGenerator pairGenerator = KeyPairGenerator.getInstance("EC");
//		pairGenerator.initialize(128);
//		KeyPair kp = pairGenerator.generateKeyPair();
//	}
//	
//	/**
//	 * "SHA-128" -> "SHA-256"
//	 * @throws NoSuchAlgorithmException
//	 */
//	@SuppressWarnings("unused")
//	public void test04() throws NoSuchAlgorithmException {
//		MGF1ParameterSpec mgf1 = new MGF1ParameterSpec("SHA-128");
//	}
//	
//	/**
//	 * 10 -> 100000
//	 * @throws NoSuchAlgorithmException
//	 */
//	@SuppressWarnings("unused")
//	public void test05() throws NoSuchAlgorithmException{
//		byte[] salt = new byte[16];
//		SecureRandom.getInstanceStrong().nextBytes(salt);
//		PBEParameterSpec pbeSpec = new PBEParameterSpec(salt, 10); 
//	}
//	
//	/**
//	 * 10 -> 1024
//	 * "10" -> "65537" | not working
//	 * @throws NoSuchAlgorithmException
//	 */
//	@SuppressWarnings("unused")
//	public void test06() {
//		RSAKeyGenParameterSpec rsaSpc = new RSAKeyGenParameterSpec(10, new BigInteger("10"));
//	}
}
