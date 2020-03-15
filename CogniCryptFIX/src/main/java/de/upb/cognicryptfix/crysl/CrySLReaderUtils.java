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

	private static final Logger logger = LogManager.getLogger(CrySLReaderUtils.class.getSimpleName());

	public static List<CrySLRule> readRulesFromSourceFiles(final String folderPath) {

		try {
			CrySLModelReader analysisCrySLReader = new CrySLModelReader();
			List<CrySLRule> rules = new ArrayList<CrySLRule>();
			File[] files = new File(folderPath).listFiles();
			for (File file : files) {
				if (file != null && file.getName().endsWith(".crysl")) {
					rules.add(analysisCrySLReader.readRule(file));
				}
			}

			return rules;
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

}