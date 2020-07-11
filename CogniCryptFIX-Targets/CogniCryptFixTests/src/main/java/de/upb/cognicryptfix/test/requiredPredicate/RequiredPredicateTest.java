package de.upb.cognicryptfix.test.requiredPredicate;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AlgorithmParameters;
import java.security.DigestInputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import de.upb.cognicryptfix.test.utils.Utils;

public class RequiredPredicateTest {

	/**
	 * salt => add SecureRandom
	 */
	@SuppressWarnings("unused")
	private void test01() {
		Utils utils = new Utils();
		byte[] salt = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		
		char[] password = utils.readFileContent("passwordPath");
		PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 10000, 128);
		pbeKeySpec.clearPassword();
	}
	
	/**
	 * salt => add SecureRandom
	 */
	@SuppressWarnings("unused")
	private void test02() {
		byte[] iv = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
	}

	/**
	 * keyMaterial => SecretKey.getEncoded()
	 */
	@SuppressWarnings("unused")
	private void test04() {
		byte[] keyMaterial = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		SecretKeySpec spec = new SecretKeySpec(keyMaterial, "AES");
	}

	/**
	 * null => 	MessageDigest md = MessageDigest.getInstance("SHA-512");	
	 *			byte[] digest = md.digest("".getBytes());
	 *
	 *! last call of MessageDigest needs to be after new DigestInputStream(_,_);
	 *
	 * @throws IOException 
	 */
	@SuppressWarnings("unused")
	private void test05() throws IOException {
		InputStream fileInputStream = new FileInputStream("DigestInput.txt");
		DigestInputStream digestStream = new DigestInputStream(fileInputStream, null);
		digestStream.read();
		digestStream.close();
	}
	
	/** 
	 *	generate preparedDH
	 * @throws Exception
	 */
	public void test06() throws Exception {
		byte[] next = new byte[16];
		SecureRandom.getInstanceStrong().nextBytes(next);
		IvParameterSpec iv = new IvParameterSpec(next);
		AlgorithmParameters parameters = AlgorithmParameters.getInstance("DiffieHellman");
		parameters.init(iv);
	}	
	
	/**
	 * generate preparedKeyMaterial
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 * @throws InvalidKeyException
	 * @throws IllegalBlockSizeException
	 * @throws BadPaddingException
	 */
	private void test07() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {		
		byte[] keyMaterial = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		SecretKeySpec spec = new SecretKeySpec(keyMaterial, "AES");
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.ENCRYPT_MODE, spec);
		byte[] message = "HelloWorld".getBytes();
		byte[] cipherText = c.doFinal(message);
	}
}
