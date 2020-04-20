package de.upb.cognicryptfix.test.forbiddenMethod;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.spec.PBEKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import de.upb.cognicryptfix.test.utils.Utils;

public class ForbiddenMethodTest {

//	/**
//	 * new PBEKeySpec(char[]) => PBEKeySpec(char[],byte[],int,int)
//	 * @throws IOException
//	 */
//	@SuppressWarnings("unused")
//	private void test01() throws IOException {
//		Utils utils = new Utils();
//
//		String filePath = "xxx";
//		char[] content = utils.readFileContent(filePath);
//		PBEKeySpec spec = new PBEKeySpec(content);
//		spec.clearPassword();
//	}
//
//	/**
//	 * getDefault() => getInstance(Sting)
//	 * @throws Exception
//	 */
//	@SuppressWarnings("unused")
//	private void test02() throws Exception {
//		Utils utils = new Utils();
//		
//		KeyStore store = utils.createKeyStore();
//		KeyManager[] km = utils.getKeyManagers(store);
//		TrustManager[] tm = utils.getTrustManager(store);
//		SecureRandom random = SecureRandom.getInstanceStrong();
//		SSLContext context = SSLContext.getDefault();
//		context.init(km, tm, random);
//		context.createSSLEngine();
//	}
//
//	
//	/**
//	 * on(boolean) => X
//	 * @throws Exception
//	 */
	@SuppressWarnings("unused")
	private void test03() throws NoSuchAlgorithmException, IOException {
		InputStream fileInputStream = new FileInputStream("DigestInput.txt");
		MessageDigest md = MessageDigest.getInstance("SHA-512");		
		DigestInputStream digestStream = new DigestInputStream(fileInputStream, md);
		digestStream.read();
		digestStream.on(true);
		digestStream.close();
		byte[] digest = md.digest("Hello".getBytes());

	}
}
