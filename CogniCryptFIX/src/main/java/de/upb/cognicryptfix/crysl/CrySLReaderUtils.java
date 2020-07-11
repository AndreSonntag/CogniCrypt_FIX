/**
 * 
 */
package de.upb.cognicryptfix.crysl;

import java.io.File;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.cryslhandler.CrySLModelReader;
import crypto.rules.CrySLRule;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class CrySLReaderUtils {

	private static final Logger LOGGER = LogManager.getLogger(CrySLReaderUtils.class);

	public static List<CrySLRule> readRulesFromSourceFiles(final String folderPath) {

		File temp = null;
		try {
			LOGGER.info("Read rules from "+folderPath);
			CrySLModelReader analysisCrySLReader = new CrySLModelReader();
			List<CrySLRule> rules = new ArrayList<CrySLRule>();
			File[] files = new File(folderPath).listFiles();
			for (File file : files) {
				if (file != null && file.getName().endsWith(".crysl")) {
					temp = file;
//					logger.debug("read: "+file.getName());
					CrySLRule rule = analysisCrySLReader.readRule(file);
					if(!rule.getClassName().equals("void")) {
						rules.add(rule);
					} else {
						LOGGER.error("Rule: "+file.getName()+" is a void rule!");
					}
				}
			}
			return rules;
		} catch (MalformedURLException e) {
			LOGGER.error("Problem with rule: "+temp.getAbsolutePath());
			e.printStackTrace();
		}
		return null;
	}

}