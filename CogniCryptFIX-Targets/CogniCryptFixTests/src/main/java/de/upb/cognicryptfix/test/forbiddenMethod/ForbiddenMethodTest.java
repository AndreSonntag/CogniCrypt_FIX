package de.upb.cognicryptfix.test.forbiddenMethod;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.SSLContext;

public class ForbiddenMethodTest {

	/* works */
	private void test01() {
		char[] password = { '2', 'd', '#', 'q', 'P' };
		PBEKeySpec spec = new PBEKeySpec(password);
		spec.clearPassword();
	}

	/* works */
	private void test02() throws NoSuchAlgorithmException {
		SSLContext context = SSLContext.getDefault();
	}

	/* works */
	private void test03() throws NoSuchAlgorithmException, IOException {
		InputStream fileInputStream = new FileInputStream("DigestInput.txt");
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		md.update(new byte[16]);
		md.digest();
		DigestInputStream digestStream = new DigestInputStream(fileInputStream, md);
		digestStream.read();
		digestStream.on(true);
		digestStream.close();
	}
}
