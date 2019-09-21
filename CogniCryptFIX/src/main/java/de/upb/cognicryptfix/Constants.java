package de.upb.cognicryptfix;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class Constants {

	public static final String outerFileSeparator = System.getProperty("file.separator");
	public static final String lineSeparator = System.getProperty("line.separator");
	
	//CrySL related
	public static final String serializedCrySLRulePath = Constants.class.getResource("/SerializedCrySLRules/").getPath();
	public static final String crySLRulePath = Constants.class.getResource("/CrySLRules/").getPath();

}
