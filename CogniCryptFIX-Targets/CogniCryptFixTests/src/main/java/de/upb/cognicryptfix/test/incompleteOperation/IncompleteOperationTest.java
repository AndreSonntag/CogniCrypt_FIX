package de.upb.cognicryptfix.test.incompleteOperation;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class IncompleteOperationTest {

	/*- Rules with just one necessary call
	 * - CertPathTrustManagerParameters
	 * - DHGenParameterSpec
	 * - DHParameterSpec
	 * - DSAGenParameterSpec
	 * - DSAParameterSpec
	 * - GCMParameterSpec
	 * - HMACParameterSpec
	 * - IvParameterSpec
	 * - Key (interface)
	 * - KeyPair
	 * - KeyStoreBuilderParameters
	 * - PBEParameterSpec
	 * - PKIXBuilderParameters
	 * - PKIXParameters
	 * - RSAKeyGenParameterSpec
	 * - SecretKey
	 * - SecretKeySpec
	 * - TrustAnchor
	 */
		
	/* problem -> contains predicate loops! */
//	public void test00() throws Exception {
//		AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
//	}	
	
	/* problem -> analysis doesn't detect any error ?? */
//	public void test15() throws Exception {
//		SSLContext context = SSLContext.getInstance("TLSv1.2");
//		SSLEngine engine = context.createSSLEngine();
//	}

	/* problem --> rule borken ??*/
//	public void test08() throws Exception {
//		KeyFactory factory = KeyFactory.getInstance("RSA");
//	}
	
	/* problem -> CryptoAnalysis - java.lang.NullPointerException */
//	public void test17() throws Exception {
//		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
//	}
	
//	public void test02() throws Exception {
//		InputStream fileInputStream = new FileInputStream("CipherInput.txt");
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//		SecretKey sK = keyGen.generateKey();
//		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
//		c.init(Cipher.DECRYPT_MODE, sK);
//		c.doFinal();
//		CipherInputStream cipherStream = new CipherInputStream(fileInputStream, c);
//	}

	/* Detected call to forbidden method void init(int,java.security.Key) of class javax.crypto.Cipher. Instead, call method <javax.crypto.Cipher: void init(int,java.security.Key)>.*/
//	public void test03() throws Exception {
//		OutputStream fileOutputStream = new FileOutputStream("CipherInput.txt");
//		KeyGenerator keyGen = KeyGenerator.getInstance("AES");
//		SecretKey sK = keyGen.generateKey();
//		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
//		c.init(Cipher.ENCRYPT_MODE, sK);
//		c.doFinal();
//		CipherOutputStream cipherStream = new CipherOutputStream(fileOutputStream, c);
//		cipherStream.close();
//	}

	/* works */
	public void test01() throws Exception {
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
	}

	/* works */
	public void test04() throws Exception {
		InputStream fileInputStream = new FileInputStream("DigestInput.txt");
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		md.update((byte)1);
		DigestInputStream digestStream = new DigestInputStream(fileInputStream, md);
	}

	/* works */
	public void test05() throws Exception {
		OutputStream fileOutputStream = new FileOutputStream("DigestInput.txt");
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		DigestOutputStream digestStream = new DigestOutputStream(fileOutputStream, md);
	}

	/* works */
	public void test06() throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance("RSA");
	}

	/* works */
	public void test07() throws Exception {
		KeyManagerFactory factory = KeyManagerFactory.getInstance("PKIX");
	}

	/* works */
	public void test09() throws Exception {
		KeyPairGenerator pairGen = KeyPairGenerator.getInstance("DSA");
	}

	/* works */
	public void test10() throws Exception {
		KeyStore store = KeyStore.getInstance("pkcs12");
	}

	/* works */
	public void test11() throws Exception {
		Mac mac = Mac.getInstance("HmacMD5");
	}

	/* works */
	public void test12() throws Exception {
		MessageDigest messDig = MessageDigest.getInstance("SHA-256");
	}

	/* works */
	public void test13() throws Exception {
		byte[] salt = new byte[100];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		char[] password = { '2', 'd', '#', 'q', 'P' };
		PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 128);
	}

	/* works */
	public void test14() throws Exception {
		SSLContext context = SSLContext.getInstance("TLSv1.2");
	}

	/* works */
	public void test16() throws Exception {
		SSLParameters paramters = new SSLParameters();
	}

	/* works */
	public void test18() throws Exception {
	Signature sig = Signature.getInstance("SHA256withECDSA");
	}

	/* works */
	public void test019() throws Exception {
		TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
	}
}
