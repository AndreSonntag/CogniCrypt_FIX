package de.upb.cognicryptfix.test.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

public class Utils {
	
	public Utils() {
		
	}
	
	public char[] readFileContent(String path){
		char[] content = new char[100];

		try {
			BufferedReader br = new BufferedReader(new FileReader(path));
			for(int i = 0, charNum = br.read(); charNum != -1; i++) {
				content[i] = (char) charNum;
				charNum = br.read();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	public KeyStore createKeyStore() throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyStore ks = KeyStore.getInstance("jceks");
		ks.load(null, readFileContent("pathToPassword"));
		return ks;
	}
	
	public KeyManager[] getKeyManagers() throws UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
		KeyManager[] manager = null;
		KeyManagerFactory factory = KeyManagerFactory.getInstance("PKIX");
		factory.init(createKeyStore(), readFileContent("pathToPasswordFile"));
		manager = factory.getKeyManagers();
		return manager;
	}
	
	public TrustManager[] getTrustManager() throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
		TrustManager[] managers = null;
		TrustManagerFactory factory = TrustManagerFactory.getInstance("PKIX");
		factory.init(createKeyStore());
		managers = factory.getTrustManagers();
		return managers;
	}
}
