package de.upb.cognicryptfix.test.hardcoded;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;

import de.upb.cognicryptfix.test.utils.Utils;

public class HardCodedTest {

	/**
	 * password = { 'a', 'b', 'c' } => read from file
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
	@SuppressWarnings("unused")
	private void test01() throws IOException, NoSuchAlgorithmException {
		char[] password = { 'a', 'b', 'c' };
		byte[] salt = new byte[16];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		PBEKeySpec spec = new PBEKeySpec(password, salt, 10000, 126);
		spec.clearPassword();
	}
	
	/**
	 * password = { 'x', 'y', 'z' } => read from file
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 */
	@SuppressWarnings("unused")
	private void test02() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyStore ks = KeyStore.getInstance("jceks");
		char[] password = { 'x', 'y', 'z' };
		ks.load(null, password);
	}
	
	
	/**
	 * keyPassword = { 'x', 'y', 'z' } => read from file
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws CertificateException 
	 * @throws UnrecoverableKeyException 
	 */
	@SuppressWarnings("unused")
	private void test03() throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException {
		Utils utils = new Utils();
		KeyStore ks = KeyStore.getInstance("jceks");
		char[] ksPassword = utils.readFileContent("pathToKeyStorePassword");
		ks.load(null, ksPassword);
		
		char[] keyPassword = { 'x', 'y', 'z' };
		KeyManagerFactory factory = KeyManagerFactory.getInstance("PKIX");
		factory.init(ks, keyPassword);
		KeyManager[] manager = factory.getKeyManagers();	
	}
	
	/**
	 * ksPassword = { 'a', 'b', 'c' } => read from file
	 * keyPassword = { 'x', 'y', 'z' } => read from file
	 * @throws IOException
	 * @throws NoSuchAlgorithmException 
	 * @throws KeyStoreException 
	 * @throws CertificateException 
	 * @throws UnrecoverableKeyException 
	 */
	@SuppressWarnings("unused")
	private void test04() throws IOException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException {
		KeyStore ks = KeyStore.getInstance("jceks");
		char[] ksPassword = { 'a', 'b', 'c' };
		ks.load(null, ksPassword);
		
		char[] keyPassword = { 'x', 'y', 'z' };
		KeyManagerFactory factory = KeyManagerFactory.getInstance("PKIX");
		factory.init(ks, keyPassword);
		KeyManager[] manager = factory.getKeyManagers();	
	}
	
	private void test05() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyStore ks = KeyStore.getInstance("jceks");
		ks.load(null, getPassword());
	}	
	private char[] getPassword() {
		return new char[]{ 'x', 'y', 'z' };
	}

	
	
	
}
