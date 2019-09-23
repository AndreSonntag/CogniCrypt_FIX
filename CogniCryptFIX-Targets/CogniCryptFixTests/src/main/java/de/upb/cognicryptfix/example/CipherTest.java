package de.upb.cognicryptfix.example;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class CipherTest {
	
	public void encryption(SecretKey key, IvParameterSpec iv) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
		String plaintext = "HelloWorld";
		Cipher ecb = Cipher.getInstance("AES/CBC");
		ecb.init(Cipher.ENCRYPT_MODE, key, iv);
		ecb.doFinal(plaintext.getBytes());
	}

}
