package org.jsystemtest;

import static jsystem.utils.StringUtils.isEmpty;

import java.io.File;

import jsystem.framework.FrameworkOptions;
import jsystem.framework.JSystemProperties;
import jsystem.runner.loader.LoadersManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

public class CommandLineInterface {
	@SuppressWarnings("static-access")
	private static Option projectOpt = OptionBuilder.hasArg().withLongOpt("project").withArgName("project")
			.withDescription("Test project").create("p");

	@SuppressWarnings("static-access")
	private static Option sutOpt = OptionBuilder.hasArg().withArgName("sut").withLongOpt("sut")
			.withDescription("Sut file to be used").create("u");

	@SuppressWarnings("static-access")
	private static Option scenarioOpt = OptionBuilder.hasArg().withLongOpt("scenario").withArgName("scenario")
			.withDescription("Scenario file to execute").create("s");

	@SuppressWarnings("static-access")
	private static Option helpOpt = OptionBuilder.withLongOpt("help").withDescription("Help").create("h");

	@SuppressWarnings("static-access")
	private static Option fileOpt = OptionBuilder.hasArg().withLongOpt("file").withArgName("file").withLongOpt("file")
			.withDescription("Multiple scenario suit execution XML file").create("f");

	private static void printHelp(Options options) {
		HelpFormatter forametter = new HelpFormatter();
		forametter.printHelp("possix", options);
	}

	public static void main(String... args) throws ParseException {
		Options options = new Options();
		options.addOption(projectOpt);
		options.addOption(sutOpt);
		options.addOption(scenarioOpt);
		options.addOption(helpOpt);
		options.addOption(fileOpt);

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption(helpOpt.getOpt())) {
			printHelp(options);
			return;
		}
		if (!cmd.hasOption(projectOpt.getOpt())) {
			printHelp(options);
			return;
		}
		File baseDir = new File(cmd.getOptionValue(projectOpt.getOpt()));
		
		String xmlFile = null;
		String scenario = null;
		String sut = null;
		ScenarioExecutor executor = null;
		if (cmd.hasOption(fileOpt.getOpt())){
			xmlFile = cmd.getOptionValue(fileOpt.getOpt());
			executor = new ScenarioExecutor(baseDir, xmlFile);
		} else {
			if (!cmd.hasOption(scenarioOpt.getOpt()) || !cmd.hasOption(sutOpt.getOpt()) ) {
				printHelp(options);
				return;
			}
			scenario = cmd.getOptionValue(scenarioOpt.getOpt());
			sut = cmd.getOptionValue(sutOpt.getOpt());
			executor = new ScenarioExecutor(baseDir, scenario, sut);
		}


		System.setProperty("user.dir", baseDir.getAbsolutePath());
		JSystemProperties.getInstance().setPreference(FrameworkOptions.LOG_FOLDER,
				new File(baseDir.getParentFile(), "log").getAbsolutePath());
		if (isEmpty(JSystemProperties.getInstance().getPreference(FrameworkOptions.LIB_DIRS))) {
			JSystemProperties.getInstance().setPreference(FrameworkOptions.LIB_DIRS,
					new File(baseDir.getParentFile(), "lib").getAbsolutePath());
		}

		JSystemProperties.getInstance().setJsystemRunner(true);
		LoadersManager.getInstance().getLoader();
		JSystemProperties.getInstance().setJsystemRunner(false);
		
		try {
			executor.execute();
		} catch (Throwable t) {
			System.err.println("Failed due to " + t.getMessage());
			t.printStackTrace();
		}
	}
}
