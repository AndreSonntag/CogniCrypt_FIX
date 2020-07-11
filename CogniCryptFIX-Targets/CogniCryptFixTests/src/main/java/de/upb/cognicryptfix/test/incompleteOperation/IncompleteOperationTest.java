package de.upb.cognicryptfix.test.incompleteOperation;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.AlgorithmParameters;
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
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class IncompleteOperationTest {

//	/* problem -> analysis doesn't detect any error ?? */
//	public void test15() throws Exception {
//		SSLContext context = SSLContext.getInstance("TLSv1.2");
//		SSLEngine engine = context.createSSLEngine();
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

	
	/** 
	 *  ADD:
	 *	<java.security.AlgorithmParameters: void init(java.security.spec.AlgorithmParameterSpec)>
	 * @throws Exception
	 */
	public void test00() throws Exception {
		AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
	}	

	/** 
	 *  ADD:
	 * 	<javax.crypto.Cipher: void init(int,java.security.Key)>
	 *	<javax.crypto.Cipher: byte[] doFinal(byte[])>
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public void test01() throws NoSuchAlgorithmException, NoSuchPaddingException{
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	}
	
	/** 
	 *  ADD:
	 *  <java.security.DigestInputStream: int read()>
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public void test02() throws FileNotFoundException, NoSuchAlgorithmException {
		InputStream fileInputStream = new FileInputStream("DigestInput.txt");
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		DigestInputStream digestStream = new DigestInputStream(fileInputStream, md);
		md.update((byte)1);
		md.digest();
	}
	
	/** 
	 *  ADD:
	 *	<java.security.DigestOutputStream: void write(int)>
	 *	<java.io.OutputStream: void close()>
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchPaddingException
	 */
	public void test03() throws FileNotFoundException, NoSuchAlgorithmException {
		OutputStream fileOutputStream = new FileOutputStream("DigestInput.txt");
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		DigestOutputStream digestStream = new DigestOutputStream(fileOutputStream, md);
		md.update((byte)1);
		md.digest();
	}	
	

	/** 
	 *  ADD:
	 *	<javax.crypto.KeyGenerator: javax.crypto.SecretKey generateKey()>
	 * @throws NoSuchAlgorithmException
	 */
	public void test04() throws NoSuchAlgorithmException {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
	}	
	
	/** 
	 *  ADD:
	 *	<javax.net.ssl.KeyManagerFactory: void init(java.security.KeyStore,char[])>
	 *  <javax.net.ssl.KeyManagerFactory: javax.net.ssl.KeyManager[] getKeyManagers()>
	 * @throws NoSuchAlgorithmException
	 */
	public void test05() throws NoSuchAlgorithmException {
		KeyManagerFactory factory = KeyManagerFactory.getInstance("PKIX");
	}

	/** 
	 *  ADD:
	 *	<java.security.KeyPairGenerator: void initialize(int)>
	 *  <java.security.KeyPairGenerator: java.security.KeyPair generateKeyPair()>
	 * @throws NoSuchAlgorithmException
	 */
	public void test06() throws NoSuchAlgorithmException{
		KeyPairGenerator pairGen = KeyPairGenerator.getInstance("DSA");
	}

	/** 
	 *  ADD:
	 *	<java.security.KeyStore: void load(java.io.InputStream,char[])>
	 * @throws KeyStoreException
	 */
	public void test7() throws KeyStoreException{
		KeyStore store = KeyStore.getInstance("pkcs12");
	}

	/** 
	 *  ADD:
	 *	<javax.crypto.Mac: void init(java.security.Key)>
	 *	<javax.crypto.Mac: byte[] doFinal()>
	 * @throws NoSuchAlgorithmException
	 */
	public void test8() throws NoSuchAlgorithmException{
		Mac mac = Mac.getInstance("HmacMD5");
	}

	/** 
	 *  ADD:
	 *	<java.security.MessageDigest: byte[] digest(byte[])>
	 * @throws NoSuchAlgorithmException
	 */
	public void test9() throws NoSuchAlgorithmException{
		MessageDigest messDig = MessageDigest.getInstance("SHA-256");
	}
	
	/** 
	 *  ADD:
	 *	<javax.crypto.spec.PBEKeySpec: void clearPassword()>
	 * @throws NoSuchAlgorithmException
	 */
	public void test10() throws NoSuchAlgorithmException{
		byte[] salt = new byte[100];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		char[] password = { '2', 'd', '#', 'q', 'P' };
		PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 128);
	}

	/** 
	 *  ADD:
	 *	<javax.net.ssl.SSLContext: void init(javax.net.ssl.KeyManager[],javax.net.ssl.TrustManager[],java.security.SecureRandom)>
	 * @throws NoSuchAlgorithmException
	 */
	public void test11() throws NoSuchAlgorithmException{
		SSLContext context = SSLContext.getInstance("TLSv1.2");
	}	
	
	/** 
	 *  ADD:
	 *	<javax.net.ssl.SSLParameters: void setProtocols(java.lang.String[])>
	 *	<javax.net.ssl.SSLParameters: void setCipherSuites(java.lang.String[])>
	 */
	public void test12(){
		SSLParameters paramters = new SSLParameters();
	}
		
	/** 
	 *  ADD:
	 *	<java.security.Signature: void initVerify(java.security.PublicKey)>
	 *	<java.security.Signature: boolean verify(byte[])>
	 * @throws NoSuchAlgorithmException
	 */
	public void test13() throws NoSuchAlgorithmException{
	Signature sig = Signature.getInstance("SHA256withECDSA");
	}

	/** 
	*  ADD:
	*	<javax.net.ssl.TrustManagerFactory: void init(java.security.KeyStore)>
	*	<javax.net.ssl.TrustManagerFactory: javax.net.ssl.TrustManager[] getTrustManagers()>//	
	* @throws NoSuchAlgorithmException
	*/
	public void test014() throws NoSuchAlgorithmException{
		TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
	}
	
	/** 
	*  ADD:
	*	<javax.crypto.SecretKeyFactory: javax.crypto.SecretKey generateSecret(java.security.spec.KeySpec)>
	* @throws NoSuchAlgorithmException
	*/
	public void test15() throws Exception {
		SecretKeyFactory factory = SecretKeyFactory.getInstance("AES");
	}
	
	/** 
	*  ADD:
	*	<java.security.KeyFactory: java.security.PublicKey generatePublic(java.security.spec.KeySpec)>
	* @throws NoSuchAlgorithmException
	*/
	public void test16() throws Exception {
		KeyFactory factory = KeyFactory.getInstance("RSA");
	}
	
}
