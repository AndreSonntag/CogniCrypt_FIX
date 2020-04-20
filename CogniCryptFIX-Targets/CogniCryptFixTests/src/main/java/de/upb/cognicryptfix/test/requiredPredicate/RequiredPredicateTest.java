package de.upb.cognicryptfix.test.requiredPredicate;

import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import de.upb.cognicryptfix.test.utils.Utils;

public class RequiredPredicateTest {

//	/**
//	 * salt => add SecureRandom
//	 */
//	@SuppressWarnings("unused")
//	private void test01() {
//		Utils utils = new Utils();
//		byte[] salt = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
//		
//		char[] password = utils.readFileContent("passwordPath");
//		PBEKeySpec pbeKeySpec = new PBEKeySpec(password, salt, 10000, 128);
//		pbeKeySpec.clearPassword();
//	}
//	
//	/**
//	 * salt => add SecureRandom
//	 */
//	@SuppressWarnings("unused")
//	private void test02() {
//		byte[] iv = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
//		GCMParameterSpec gcmParamSpec = new GCMParameterSpec(128, iv);
//	}
//
	/**
	 * keyMaterial => SecretKey.getEncoded()
	 */
	@SuppressWarnings("unused")
	private void test04() {
		byte[] keyMaterial = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
		SecretKeySpec spec = new SecretKeySpec(keyMaterial, "AES");
	}

	
	/*-
	 *  Exception in thread "main" java.lang.StackOverflowError
	 * 	at wpds.impl.WeightedPAutomaton.addWeightForTransition(WeightedPAutomaton.java:261)
	 */
//	private void _test05() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {		
//		byte[] keyMaterial = { 15, -12, 94, 0, 12, 3, -65, 73, -1, -84, -35 };
//		SecretKeySpec spec = new SecretKeySpec(keyMaterial, "AES");
//		Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
//		c.init(Cipher.ENCRYPT_MODE, spec);
//		byte[] message = "HelloWorld".getBytes();
//		byte[] cipherText = c.doFinal(message);
//	}
	

	
	
	

}
