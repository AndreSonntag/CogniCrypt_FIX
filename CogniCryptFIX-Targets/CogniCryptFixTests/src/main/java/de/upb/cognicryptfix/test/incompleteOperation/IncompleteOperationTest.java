package de.upb.cognicryptfix.test.incompleteOperation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class IncompleteOperationTest {
	
//	/* problem -> contains predicate loops! */
//	public void test00() throws Exception {
//		AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
//	}	
//	
	/* problem -> analysis doesn't detect any error ?? */
//	public void test15() throws Exception {
//		SSLContext context = SSLContext.getInstance("TLSv1.2");
//		SSLEngine engine = context.createSSLEngine();
//	}

//	public void test17() throws Exception {
//		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
//	}
//	
//	public void test00() throws Exception {
//		KeyFactory factory = KeyFactory.getInstance("RSA");
//	}
	
	
//	public void test02() throws Exception {
//		InputStream fileInputStream = new FileInputStream("CipherInput.txt");
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//		SecretKey sK = keyGen.generateKey();
//		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
//		c.init(Cipher.DECRYPT_MODE, sK);
//		byte[] plainText = c.doFinal("HelloWorld".getBytes());
//		CipherInputStream cipherStream = new CipherInputStream(fileInputStream, c);
//	}
//
//	public void test03() throws Exception {
//		OutputStream fileOutputStream = new FileOutputStream("CipherInput.txt");
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//		SecretKey sK = keyGen.generateKey();
//		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
//		c.init(Cipher.ENCRYPT_MODE, sK);
//		CipherOutputStream cipherStream = new CipherOutputStream(fileOutputStream, c);
//		c.doFinal(new byte[16]);
//
//	}

	/* works */
//	public void test01() throws NoSuchAlgorithmException, NoSuchPaddingException{
//		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
//	}
//	
//	/* works */
//	public void test04() throws FileNotFoundException, NoSuchAlgorithmException {
//		InputStream fileInputStream = new FileInputStream("DigestInput.txt");
//		MessageDigest md = MessageDigest.getInstance("SHA-512");
//		md.update((byte)1);
//		md.digest();
//		DigestInputStream digestStream = new DigestInputStream(fileInputStream, md);
//	}
//
//	/* works */
//	public void test05() throws FileNotFoundException, NoSuchAlgorithmException {
//		OutputStream fileOutputStream = new FileOutputStream("DigestInput.txt");
//		MessageDigest md = MessageDigest.getInstance("SHA-512");
//		md.update((byte)1);
//		md.digest();
//		DigestOutputStream digestStream = new DigestOutputStream(fileOutputStream, md);
//	}
//
//	/* works */
//	public void test06() throws NoSuchAlgorithmException {
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//	}
//
//	/* works */
//	public void test07() throws NoSuchAlgorithmException {
//		KeyManagerFactory factory = KeyManagerFactory.getInstance("PKIX");
//	}
//
//	/* works */
//	public void test09() throws NoSuchAlgorithmException{
//		KeyPairGenerator pairGen = KeyPairGenerator.getInstance("DSA");
//	}
//
//	/* works */
//	public void test10() throws KeyStoreException{
//		KeyStore store = KeyStore.getInstance("pkcs12");
//	}
//
//	/* works */
//	public void test11() throws NoSuchAlgorithmException{
//		Mac mac = Mac.getInstance("HmacMD5");
//	}
//
//	/* works */
//	public void test12() throws NoSuchAlgorithmException{
//		MessageDigest messDig = MessageDigest.getInstance("SHA-256");
//	}
//
//	/* works */
//	public void test13() throws NoSuchAlgorithmException{
//		byte[] salt = new byte[100];
//		SecureRandom.getInstanceStrong().nextBytes(salt);
//		char[] password = { '2', 'd', '#', 'q', 'P' };
//		PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 128);
//	}
//
//	/* works */
//	public void test14() throws NoSuchAlgorithmException{
//		SSLContext context = SSLContext.getInstance("TLSv1.2");
//	}
//
//	/* works */
//	public void test16(){
//		SSLParameters paramters = new SSLParameters();
//	}
//
//	/* works */
//	public void test18() throws NoSuchAlgorithmException{
//	Signature sig = Signature.getInstance("SHA256withECDSA");
//	}
//
//	/* works */
//	public void test019() throws NoSuchAlgorithmException{
//		TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
//	}
}
