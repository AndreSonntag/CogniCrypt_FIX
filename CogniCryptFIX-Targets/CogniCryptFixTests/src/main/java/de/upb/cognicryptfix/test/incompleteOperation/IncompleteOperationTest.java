package de.upb.cognicryptfix.test.incompleteOperation;

import java.security.AlgorithmParameters;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;

public class IncompleteOperationTest {

	public void test01() throws Exception {		
		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");	
	}
	
//	public void test02() throws Exception {		
//		AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
//	}
	
	public void test03() throws Exception {		
		KeyGenerator keyGen = KeyGenerator.getInstance("RSA");
	}
	
	public void test04() throws Exception {		
		KeyPairGenerator pairGen = KeyPairGenerator.getInstance("DSA");
	}
	
	public void test05() throws Exception {		
		 Mac mac = Mac.getInstance("HmacMD5");
	}
	
	public void test06() throws Exception {		
		MessageDigest messDig = MessageDigest.getInstance("SHA-256");
	}
	
	public void test07() throws Exception {		
		Signature sig = Signature.getInstance("SHA256withECDSA");
	}

}
