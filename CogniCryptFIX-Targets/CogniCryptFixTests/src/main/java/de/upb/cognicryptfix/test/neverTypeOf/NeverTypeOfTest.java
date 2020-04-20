package de.upb.cognicryptfix.test.neverTypeOf;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;

public class NeverTypeOfTest {

//	/**
//	 * 1. password = "2d#qP" => password = { '2', 'd', '#', 'q', 'P' }
//	 * 2. password = { '2', 'd', '#', 'q', 'P' } => read from file
//	 * @throws Exception
//	 */
//	@SuppressWarnings("unused")
//	private void test01() throws Exception{
//		byte[] salt = new byte[16];
//		SecureRandom.getInstanceStrong().nextBytes(salt);
//		String password = "2d#qP";				
//		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
//		spec.clearPassword();
//	}
//	
//	/**
//	 * 1. password = "abc" => password = { 'a', 'b', 'c'}
//	 * 2. password = { 'a', 'b', 'c'} => read from file
//	 * @throws Exception
//	 */
//	@SuppressWarnings("unused")
//	private void test02() throws Exception{
//		KeyStore ks = KeyStore.getInstance("jceks");
//		String password = "abc";
//		ks.load(null, password.toCharArray());
//	}
	
}
