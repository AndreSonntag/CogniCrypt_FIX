package de.upb.cognicryptfix;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.analysis.CryptoAnalysisListener;
import de.upb.cognicryptfix.utils.MavenProject;
import de.upb.cognicryptfix.utils.Utils;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class HeadlessRepairer {

 	private static CommandLine options;

	public static void main(String... args) throws ParseException {
		
		final CommandLineParser parser = new DefaultParser();
		options = parser.parse(new HeadlessRepairerOptions(), args);
	
		final String rulesDirectory = options.hasOption("rulesDir") ? options.getOptionValue("rulesDir") : "rulesDirectory";
		final String sootClassPath = options.hasOption("sootCp") ? options.getOptionValue("sootCp") : "sootClassPath";
		final String applicationClassPath = options.hasOption("applicationCp") ? options.getOptionValue("applicationCp") : "applicationClassPath";

		MavenProject mavenProject = Utils.createAndCompile(applicationClassPath);
		CryptoAnalysis analysis = new CryptoAnalysis();
		CryptoAnalysisListener handler = new CryptoAnalysisListener();
		analysis.runSoot(mavenProject, handler);
		
	}

}