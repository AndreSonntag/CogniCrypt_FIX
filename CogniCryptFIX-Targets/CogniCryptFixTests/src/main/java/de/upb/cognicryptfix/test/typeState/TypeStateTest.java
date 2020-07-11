package de.upb.cognicryptfix.test.typeState;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class TypeStateTest {
	
	/**
	 * Unexpected call to method doFinal on object of type javax.crypto.Cipher. Expect a call to one of the following methods init
	 * copy all lines after doFinal and insert everything abote doFinal
	 * @throws Exception
	 */
	private void test01() throws Exception{
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		byte[] cipherText = c.doFinal(new byte[16]);			//errorUnit
		int keySize = 128;
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(keySize);
		int mode = 1;
		SecretKey key = keyGen.generateKey();
		SecretKey omega = key;
		c.init(mode, omega);
	}
	
	/**
	 * ErrorMessage: Unexpected call to method init on object of type javax.crypto.KeyGenerator.
	 * delete keyGen.init(128)
	 * @throws Exception
	 */
	private void test02() throws NoSuchAlgorithmException  {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.generateKey();
		System.out.println(keyGen.getAlgorithm());
		keyGen.init(128);
	}

	private void test03() throws NoSuchAlgorithmException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		keyGen.init(128);
		keyGen.generateKey();
		keyGen.init(128);	//<-- error without call purposes
	}
	
	private void test04() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		SecretKey key = keyGen.generateKey();		
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] cipherText = c.doFinal();	
		c.init(Cipher.ENCRYPT_MODE, key);
	}
	
	private void test05() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
		SecretKey key = keyGen.generateKey();		
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		c.init(Cipher.ENCRYPT_MODE, key);
		byte[] plainText = new byte[16];
		byte[] cipherText = c.doFinal();
		cipherText = c.doFinal(plainText);	

	}
	
	
}
