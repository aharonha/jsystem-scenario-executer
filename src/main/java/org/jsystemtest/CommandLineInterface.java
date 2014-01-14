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
	private static Option projectOpt = OptionBuilder.hasArg().withLongOpt("project").withArgName("project").withDescription("Test project").create("p");

	@SuppressWarnings("static-access")
	private static Option sutOpt = OptionBuilder.hasArg().withArgName("sut").withLongOpt("sut").withDescription("Sut file to be used")
			.create("u");

	@SuppressWarnings("static-access")
	private static Option scenarioOpt = OptionBuilder.hasArg().withLongOpt("scenario").withArgName("scenario").withDescription("Scenario file to execute")
			.create("s");

	@SuppressWarnings("static-access")
	private static Option helpOpt = OptionBuilder.withLongOpt("help").withDescription("Help").create("h");

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

		CommandLineParser parser = new PosixParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption(helpOpt.getOpt())) {
			printHelp(options);
			return;
		}
		if (!cmd.hasOption(scenarioOpt.getOpt()) || !cmd.hasOption(sutOpt.getOpt())
				|| !cmd.hasOption(projectOpt.getOpt())) {
			printHelp(options);
			return;
		}

		File baseDir = new File(cmd.getOptionValue(projectOpt.getOpt()));
		String scenario = cmd.getOptionValue(scenarioOpt.getOpt());
		String sut = cmd.getOptionValue(sutOpt.getOpt());

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
		ScenarioExecutor executor = new ScenarioExecutor(baseDir, scenario, sut);
		try {
			executor.execute();
		} catch (Throwable t) {
			System.err.println("Failed due to " + t.getMessage());
			t.printStackTrace();
		}
	}
}
