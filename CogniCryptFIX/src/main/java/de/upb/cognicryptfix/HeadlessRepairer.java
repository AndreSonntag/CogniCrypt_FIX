package de.upb.cognicryptfix;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;

import crypto.analysis.CryptoScanner;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.analysis.CryptoAnalysisListener;
import de.upb.cognicryptfix.scheduler.ErrorScheduler;
import de.upb.cognicryptfix.utils.MavenProject;
import de.upb.cognicryptfix.utils.Utils;
import soot.options.Options;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class HeadlessRepairer {

	static {
        readLoggingConfiguration();
    }

 	private static CommandLine options;

	public static void main(String... args) throws ParseException, InterruptedException {
		
		final CommandLineParser parser = new DefaultParser();
		options = parser.parse(new HeadlessRepairerOptions(), args);
	
		final String rulesDirectory = options.hasOption("rulesDir") ? options.getOptionValue("rulesDir") : "rulesDirectory";
		final String sootClassPath = options.hasOption("sootCp") ? options.getOptionValue("sootCp") : "sootClassPath";
		final String applicationClassPath = options.hasOption("applicationCp") ? options.getOptionValue("applicationCp") : "applicationClassPath";
				
		ErrorScheduler scheduler = ErrorScheduler.getInstance();
		MavenProject analysingProject = Utils.createAndCompile(applicationClassPath);		
		CryptoAnalysisListener reporter = new CryptoAnalysisListener(scheduler);	
		AnalysisRepairThread repairThread = new AnalysisRepairThread(analysingProject, reporter);
		repairThread.start();	
		repairThread.join();
	}
		
	private static void readLoggingConfiguration() {
		ConfigurationSource source;
		try {
			source = new ConfigurationSource(new FileInputStream(Constants.log4j2XML_PATH));
			Configurator.initialize(null, source);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
