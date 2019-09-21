/**
 * 
 */
package de.upb.cognicryptfix.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.rules.CryptSLRule;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class CrySLReaderUtils {

    private static final Logger logger = LogManager.getLogger(CrySLReaderUtils.class);
	
	public static List<CryptSLRule> readRulesFromBinaryFiles(final String folderPath) {
		
		List<CryptSLRule> rules = new ArrayList<CryptSLRule>();
		Arrays.asList((new File(folderPath)).list()).stream()
				.filter(e -> ".cryptslbin".equals(e.substring(e.lastIndexOf(".")))).forEach(e -> {
					try {
						rules.add(readRuleFromBinaryFile(folderPath, e.substring(0, e.lastIndexOf("."))));
					} catch (ClassNotFoundException | IOException e1) {
						logger.error("Well, that didn't work.");
					}
				});
		return rules;
	}

	public static CryptSLRule readRuleFromBinaryFile(final String folderPath, final String ruleNameName) throws FileNotFoundException, IOException, ClassNotFoundException {
		logger.debug("read CrySL rule: "+ruleNameName);
		try (ObjectInputStream in = new ObjectInputStream(
				new FileInputStream(folderPath + "/" + ruleNameName + ".cryptslbin"));) {
			return (CryptSLRule) in.readObject();
		}
	}

}