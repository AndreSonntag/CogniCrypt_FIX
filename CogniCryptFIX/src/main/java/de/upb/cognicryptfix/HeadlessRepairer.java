package de.upb.cognicryptfix;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.analysis.CryptoAnalysisListener;
import de.upb.cognicryptfix.utils.CrySLReaderUtils;
import de.upb.cognicryptfix.utils.MavenProject;
import de.upb.cognicryptfix.utils.Utils;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class HeadlessRepairer {

	private static final Logger logger = LogManager.getLogger(HeadlessRepairer.class.getSimpleName());
 	private static CommandLine options;

	public static void main(String... args) throws ParseException {
		infoLogLevel();
		
		final CommandLineParser parser = new DefaultParser();
		options = parser.parse(new HeadlessRepairerOptions(), args);
	
		final String rulesDirectory = options.hasOption("rulesDir") ? options.getOptionValue("rulesDir") : "rulesDirectory";
		final String sootClassPath = options.hasOption("sootCp") ? options.getOptionValue("sootCp") : "sootClassPath";
		final String applicationClassPath = options.hasOption("applicationCp") ? options.getOptionValue("applicationCp") : "applicationClassPath";

		MavenProject analysingProject = Utils.createAndCompile(applicationClassPath);
		CryptoAnalysis analysis = new CryptoAnalysis();
		CryptoAnalysisListener listener = new CryptoAnalysisListener(analysingProject);
		analysis.runSoot(analysingProject, listener);
		
	}
	
	public static void infoLogLevel() {
		setLoggerLevel(Level.INFO);	
	}
	
	public static void setLoggerLevel(Level level) {
		Configurator.setAllLevels(LogManager.getRootLogger().getName(), level);
		logger.debug("Logger level: "+LogManager.getRootLogger().getLevel());
	}

}
