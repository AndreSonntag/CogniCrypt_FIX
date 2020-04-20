package de.upb.cognicryptfix;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import soot.Scene;
import soot.Type;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class Constants {

	public static final String FILE_SEPARATOR = System.getProperty("file.separator");
	public static final String LINE_SEPARATOR = System.getProperty("line.separator");
	public static final String CRYSL_RULE_PATH = Constants.class.getResource("/CrySLRules/").getPath();	
	public static final String log4j2XML_PATH = Constants.class.getResource("/log4j2.xml").getPath();	
	public static final String JIMPLE_OUTPUT_PATH = "C:\\Users\\Andre\\git\\git\\CogniCrypt_FIX\\CogniCryptFIX\\sootOutput\\jimpleOutput";
	public static final String DAVA_OUTPUT_PATH = "C:\\Users\\Andre\\git\\git\\CogniCrypt_FIX\\CogniCryptFIX\\sootOutput\\davaOutput";
	public static final String CLASS_OUTPUT_PATH = "C:\\Users\\Andre\\git\\git\\CogniCrypt_FIX\\CogniCryptFIX\\sootOutput\\classOutput";
	public static final String DAVA_OUTPUT_COPY_PROJECT = "C:\\Users\\Andre\\Desktop\\MasterThesis\\thesis-workspace\\DavaOutputTestProject";
	public static final String JCE_PATH = System.getProperty("java.home") + File.separator + "lib" + File.separator + "jce.jar";
	public final static List<String> NOT_SUPPORTED_PREDICATES = Arrays.asList("generatedManagerFactoryParameters");
	public final static List<String> NOT_SUPPORTED_PARAMETER_TYPES = Arrays.asList("java.io.FileDescriptor", "java.security.KeyStore$LoadStoreParameter", "java.security.cert.Certificate");
	public final static String REQUIRED_EXCEPTION_HANDLING_TAG = "de.upb.cognicryptfix.RequiredExceptionHandlingUnit";
	public final static String ENSURED_PREDICATE_TAG= "de.upb.cognicryptfix.EnsuredPredicateTag";

}
