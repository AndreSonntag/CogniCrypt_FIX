package de.upb.cognicryptfix;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class HeadlessRepairerOptions extends Options {

	private static final long serialVersionUID = 1L;

	public HeadlessRepairerOptions() {

		Option rulesDir = Option.builder().longOpt("rulesDir").hasArg()
				.desc("Specify the directory for the CrySL rules")
				.build();
		addOption(rulesDir);
		
		Option sootCp = Option.builder().longOpt("sootCp").hasArg()
				.desc("The class path of the whole project, including dependencies.").build();
		addOption(sootCp);
		
		Option applicationCp = Option.builder().longOpt("applicationCp").hasArg().required()
				.desc("The class path of the application, excluding dependencies. Objects within theses classes are analyzed.")
				.build();
		addOption(applicationCp);
		

		//TODO: insert option to choose the error type which should be repaird!
	}
}
