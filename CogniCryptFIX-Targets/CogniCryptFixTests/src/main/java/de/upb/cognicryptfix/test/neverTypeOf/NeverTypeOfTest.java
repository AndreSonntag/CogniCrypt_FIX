package de.upb.cognicryptfix.test.neverTypeOf;

import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManagerFactory;

public class NeverTypeOfTest {

	private void test01() throws Exception{
		byte[] salt = new byte[100];
		SecureRandom.getInstanceStrong().nextBytes(salt);
		String password = "2d#qP";				
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 10000, 256);
		spec.clearPassword();
	}
	
	private void test02() throws Exception{	
		KeyManagerFactory factory = KeyManagerFactory.getInstance("PKIX");
		factory.init(null, "HelloWorld".toCharArray());
	}
	
	// The analysis doesn't detect byte array types
	private void test03() throws Exception{
		String materialString = "TestTest";
		byte[] materialByteArray = materialString.getBytes();
		SecretKeySpec spec = new SecretKeySpec(materialByteArray, "AES"); 
	}
	
	private void test04() throws Exception{
		String materialString = "TestTest";
		SecretKeySpec spec = new SecretKeySpec(materialString.getBytes(), "AES"); 
	}
	
	private void test05() throws Exception{
		SecretKeySpec spec = new SecretKeySpec("TestTest".getBytes(), "AES"); 
	}
}
