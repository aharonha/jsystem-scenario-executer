package org.jsystemtest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import jsystem.extensions.report.junit.JUnitReporter;
import jsystem.framework.FrameworkOptions;
import jsystem.framework.JSystemProperties;
import jsystem.framework.scenario.RunningProperties;
import jsystem.framework.scenario.ScenariosManager;
import jsystem.runner.AntExecutionListener;
import jsystem.runner.loader.LoadersManager;
import jsystem.utils.PackageUtils;
import jsystem.utils.StringUtils;

import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.types.Path;
import org.jsystemtest.MultipleScenarioSuitExecutionFileParser.Execution;

public class ScenarioExecutor {

	private static final String SCENARIO_PATH = "classes/scenarios";
	private static final String SUT_PATH = "classes/sut";
	private static final String TEST_PROPERTIES_FILE_EMPTY = ".testPropertiesFile_Empty";
	private static final String DEFAULT_REPORTERS = "jsystem.extensions.report.html.LevelHtmlTestReporter;jsystem.framework.report.SystemOutTestReporter;jsystem.extensions.report.xml.XmlReporter";
	private static final String DELIMITER = ",";

	/**
	 * @parameter expression="${scenario}"
	 */
	private String scenario;

	/**
	 * @parameter expression="${sut}"
	 */
	private String sut;

	/**
	 * @parameter expression="${xmlFile}"
	 */
	private String xmlFile;

	private File baseDir;

	/**
	 */

	class Log {
		public void info(String message) {
			System.out.println(message);
		}

		public void error(String message) {
			System.err.println(message);
		}

		public void error(Throwable t) {
			System.err.println(t.getMessage());
		}

		public void debug(String message) {
			System.out.println(message);
		}
	}

	Log log = new Log();

	public ScenarioExecutor(File baseDir, String scenario, String sut) {
		this.baseDir = new File(baseDir , "target");
		this.scenario = scenario;
		this.sut = sut;
		JSystemProperties.getInstance().setPreference(FrameworkOptions.TESTS_CLASS_FOLDER, new File(this.baseDir,"classes").getAbsolutePath());
	}

	public Log getLog() {
		return log;
	}

	public static void main(String... args) {
		File baseDir = new File(args[0]);
		System.setProperty("user.dir", baseDir.getAbsolutePath());
		JSystemProperties.getInstance().setPreference(FrameworkOptions.LOG_FOLDER, new File(baseDir,"log").getAbsolutePath());		
		JSystemProperties.getInstance().setJsystemRunner(true);
		LoadersManager.getInstance().getLoader();
		JSystemProperties.getInstance().setJsystemRunner(false);
		ScenarioExecutor executor = new ScenarioExecutor(baseDir, args[1], args[2]);
		try {
			executor.execute();
		} catch (Throwable t) {
			System.err.println("Failed due to " + t.getMessage());
			t.printStackTrace();
		}
	}

	public void execute() throws Exception {

		getLog().info("changing user working dir to: " + baseDir.getAbsolutePath());
		// This line is for setting the current folder to the project root
		// folder. This is very important if we want to run the plug-in from the
		// parent folder.
		System.setProperty("user.dir", baseDir.getAbsolutePath());
		
		final File scenariosPath = new File(baseDir, SCENARIO_PATH);
		// Collect parameters that are required for the execution

		if (!StringUtils.isEmpty(xmlFile)) {
			xmlFileToParameters();
		}

		final File[] sutFilesArr = sutParameterToFileArray();
		final File[] scenarioFilesArr = scenarioParameterToFileArray(scenariosPath);

		// Check input correction
		if (sutFilesArr == null || sutFilesArr.length == 0 || scenarioFilesArr == null || scenarioFilesArr.length == 0) {
			throw new Exception("Sut or scenario parameters was not specified");
		}

		if (sutFilesArr.length != scenarioFilesArr.length) {
			throw new Exception("Number of scenarios must be equals to the number of sut files");
		}

		try {
			// This file is mandatory for scenario execution
			createEmptyTestPropertiesFile(scenariosPath);
		} catch (IOException e) {
			getLog().error("Failed to create new empty scenario properties file");
			getLog().error(e);
			throw new Exception("Failed to create new empty scenario properties file");
		}

		getLog().info("--------------------- Jsystem Scenario Executor ------------------------");
		getLog().info("About to execute scenarios " + scenario + " with sut files " + sut);
		getLog().info("of project=" + baseDir);
		getLog().info("------------------------------------------------------------------------");

		for (int i = 0; i < scenarioFilesArr.length; i++) {
			final Project p = createNewAntProject(scenariosPath, scenarioFilesArr[i], scenario.split(DELIMITER)[i],
					sut.split(DELIMITER)[i]);

			updateJSystemProperties(sutFilesArr[i], sut.split(DELIMITER)[i], scenarioFilesArr[i],
					scenario.split(DELIMITER)[i]);
			executeSingleScenario(scenarioFilesArr[i], p);
		}
		getLog().info("------------------------------------------------------------------------");
		getLog().info("Execution of scenarios " + scenario + " ended ");
		getLog().info("Reports can be found in " + JSystemProperties.getInstance().getPreference(FrameworkOptions.LOG_FOLDER));

	}

	private void xmlFileToParameters() throws IOException {
		MultipleScenarioSuitExecutionFileParser parser = new MultipleScenarioSuitExecutionFileParser(new File(xmlFile));
		parser.parse();
		StringBuilder scenarioSb = new StringBuilder();
		StringBuilder sutSb = new StringBuilder();
		for (Execution execution : parser.getExecutions()) {
			scenarioSb.append(execution.getScenario().replaceFirst("\\.xml", "")).append(",");
			sutSb.append(execution.getSut().replaceFirst("sut\\\\", "")).append(",");
		}
		scenario = scenarioSb.toString();
		sut = sutSb.toString();
	}

	private void executeSingleScenario(final File scenarioFile, final Project p) {
		getLog().info("Executing scenario " + scenarioFile.getName() + " with sut " + p.getProperty("sutFile"));
		try {
			
			p.fireBuildStarted();
			p.init();
			ProjectHelper helper = ProjectHelper.getProjectHelper();
			p.addReference("ant.projectHelper", helper);
			helper.parse(p, scenarioFile);
			p.setCoreLoader(p.createClassLoader(LoadersManager.getInstance().getLoader(), new Path(p)));
			p.executeTarget(p.getDefaultTarget());

		} catch (Exception e) {
			getLog().error("Failed to execute scenario " + scenarioFile.getName());
			getLog().error(e);
		} finally {
			p.fireBuildFinished(null);
		}
		getLog().info(
				"Execution of scenario " + scenarioFile.getName() + " with sut " + p.getProperty("sutFile")
						+ " has ended");
		getLog().info("------------------------------------------------------------------------");
	}

	private File[] scenarioParameterToFileArray(File scenariosPath) {
		final List<File> filesList = new ArrayList<File>();
		for (String scenarioName : scenario.split(DELIMITER)) {
			filesList.add(new File(scenariosPath, scenarioName.replaceFirst("scenarios", "") + ".xml"));
		}
		return filesList.toArray(new File[] {});

	}

	private File[] sutParameterToFileArray() {
		final List<File> filesList = new ArrayList<File>();
		for (String sutFileName : sut.split(DELIMITER)) {
			filesList.add(new File(baseDir + File.separator + SUT_PATH, sutFileName));
		}
		return filesList.toArray(new File[] {});
	}

	/**
	 * Updates the JSystem properties file with all the data required for the
	 * execution
	 * 
	 * @param sutFile
	 *            - The SUT file to use
	 * @param scenarioFile
	 *            - The scenario to use
	 */
	private void updateJSystemProperties(final File sutFile, final String sutName, final File scenarioFile,
			final String scenarioName) {
		
		String reporters = JSystemProperties.getInstance().getPreference(FrameworkOptions.REPORTERS_CLASSES);

		// Making sure that the JUnit reporter is in the reporter.classes
		String reporterName = JUnitReporter.class.getName();
		if (null == reporters) {
			JSystemProperties.getInstance().setPreference(FrameworkOptions.REPORTERS_CLASSES,
					DEFAULT_REPORTERS + ";" + reporterName);
		} else if (!reporters.contains(reporterName)) {
			reporters += ";" + reporterName;
			JSystemProperties.getInstance().setPreference(FrameworkOptions.REPORTERS_CLASSES, reporters);
		}

		// Configure all other required parameters:

		// Scenario
		JSystemProperties.getInstance().setPreference(FrameworkOptions.CURRENT_SCENARIO, scenarioName);

		// SUT
		JSystemProperties.getInstance().setPreference(FrameworkOptions.USED_SUT_FILE, sutName);

		// Class Folder
		JSystemProperties.getInstance().setPreference(FrameworkOptions.TESTS_CLASS_FOLDER,
				baseDir.getAbsolutePath() + File.separator + "classes");

		// Test Source
		JSystemProperties.getInstance().setPreference(FrameworkOptions.TESTS_SOURCE_FOLDER,
				baseDir.getAbsolutePath() + File.separator + "tests");

	
	}

	/**
	 * Create ANT project that can be executed programatically
	 * 
	 * @param scenariosPath
	 * @param scenarioFile
	 * @param sutFile
	 * @return
	 * @throws Exception 
	 */
	private Project createNewAntProject(File scenariosPath, File scenarioFile, String scenarioName, String sutName) throws Exception {
		System.setProperty(RunningProperties.CURRENT_SCENARIO_NAME, scenarioName);
		System.setProperty(RunningProperties.CURRENT_SUT, sutName);
		Project p = new Project();
		p.setName("JSystem Executor Project");
		p.setBaseDir(baseDir);
		p.addBuildListener(new AntExecutionListener());
		p.setProperty("basedir", scenariosPath.getAbsolutePath());
		p.setProperty("scenarios.base", scenariosPath.getParentFile().getAbsolutePath());
		p.setProperty("sutFile", sutName);
		p.setProperty("ant.file", scenarioFile.getAbsolutePath());
		DefaultLogger consoleLogger = new DefaultLogger();
		consoleLogger.setErrorPrintStream(System.err);
		consoleLogger.setOutputPrintStream(System.out);
		consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
		
		p.addBuildListener(consoleLogger);
		if (!ScenariosManager.getInstance().isScenarioExists(scenarioName)){
			throw new Exception("Scenario does not exist!");
		}
		
		return p;
	}

	/**
	 * This is required for executing scenarios
	 * 
	 * @param scenariosPath
	 * @throws IOException
	 */
	private void createEmptyTestPropertiesFile(final File scenariosPath) throws IOException {
		File testPropFile = new File(scenariosPath, TEST_PROPERTIES_FILE_EMPTY);
		getLog().debug("About to create file " + testPropFile.getAbsolutePath());
		if (!testPropFile.exists()) {
			if (!testPropFile.createNewFile()) {
				throw new IOException("Failed to create new empty properties file");
			}
		}

		if (!testPropFile.exists()) {
			throw new IOException("Failed to create " + testPropFile.getAbsolutePath());
		}
		getLog().debug("Created file " + testPropFile.getAbsolutePath());
	}

}
