package de.upb.cognicryptfix;

import java.util.Arrays;
import java.util.List;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class Constants {

	public static final String outerFileSeparator = System.getProperty("file.separator");
	public static final String lineSeparator = System.getProperty("line.separator");
	public static final String crySLRulePath = Constants.class.getResource("/CrySLRules/").getPath();	
	public static final String jimpleOutputPath = "C:\\Users\\Andre\\git\\git\\CogniCrypt_FIX\\CogniCryptFIX\\jimpleOutput";
	public final static List<String> predefinedPreds = Arrays.asList("callTo", "noCallTo", "neverTypeOf", "length", "notHardCoded");
	public final static List<String> notSupportedPredicates = Arrays.asList("generatedManagerFactoryParameters");
	public final static List<String> notSupportedParameterTypes = Arrays.asList("java.io.FileDescriptor", "java.security.KeyStore$LoadStoreParameter", "java.security.cert.Certificate");
	public final static String requiredExceptionHandlingTag = "de.upb.cognicryptfix.RequiredExceptionHandlingUnit";
	

}
