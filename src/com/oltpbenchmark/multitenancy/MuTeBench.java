/*******************************************************************************
 * oltpbenchmark.com
 *  
 *  Project Info:  http://oltpbenchmark.com
 *  Project Members:  	Carlo Curino <carlo.curino@gmail.com>
 * 				Evan Jones <ej@evanjones.ca>
 * 				DIFALLAH Djellel Eddine <djelleleddine.difallah@unifr.ch>
 * 				Andy Pavlo <pavlo@cs.brown.edu>
 * 				CUDRE-MAUROUX Philippe <philippe.cudre-mauroux@unifr.ch>  
 *  				Yang Zhang <yaaang@gmail.com> 
 * 
 *  This library is free software; you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Foundation;
 *  either version 3.0 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 ******************************************************************************/
package com.oltpbenchmark.multitenancy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.log4j.Logger;

import com.oltpbenchmark.Phase;
import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.TransactionTypes;
import com.oltpbenchmark.multitenancy.gui.Gui;
import com.oltpbenchmark.multitenancy.schedule.Schedule;
import com.oltpbenchmark.multitenancy.schedule.ScheduleEvents;
import com.oltpbenchmark.types.DatabaseType;
import com.oltpbenchmark.util.ClassUtil;
import com.oltpbenchmark.util.StringUtil;

public class MuTeBench {
	private static final Logger LOG = Logger.getLogger(MuTeBench.class);
	private static final Logger INIT_LOG = Logger.getLogger(MuTeBench.class);

	private static final String SINGLE_LINE = "**********************************************************************************";

	private static final String RATE_DISABLED = "disabled";
	private static final String RATE_UNLIMITED = "unlimited";

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String duration = null;
		String scenarioFile = null;

		// -------------------------------------------------------------------
		// INITIALIZE LOGGING
		// -------------------------------------------------------------------
		String log4jPath = System.getProperty("log4j.configuration");
		if (log4jPath != null) {
			org.apache.log4j.PropertyConfigurator.configure(log4jPath);
		} else {
			throw new RuntimeException("Missing log4j.properties file");
		}

		// -------------------------------------------------------------------
		// PARSE COMMAND LINE PARAMETERS
		// -------------------------------------------------------------------
		CommandLineParser parser = new PosixParser();
		XMLConfiguration pluginConfig = null;
		try {
			pluginConfig = new XMLConfiguration("config/plugin.xml");
		} catch (ConfigurationException e1) {
			LOG.info("Plugin configuration file config/plugin.xml is missing");
			e1.printStackTrace();
		}
		pluginConfig.setExpressionEngine(new XPathExpressionEngine());
		Options options = new Options();
		options.addOption("s", "scenario", true,
				"[required] Workload scenario file");
		options.addOption("a", "analysis-buckets", true,
				"sampling buckets for result aggregation");
		options.addOption("r", "runtime", true,
				"maximum runtime  (no events will be started after finishing runtime)");
		options.addOption("v", "verbose", false, "Display Messages");
		options.addOption("g", "gui", false, "Show controlling GUI");
		options.addOption("h", "help", false, "Print this help");
		options.addOption("o", "output", true,
				"Output file (default System.out)");
		options.addOption("b", "baseline", true,
				"Output files of previous baseline run");
		options.addOption(null, "histograms", false, "Print txn histograms");
		options.addOption("d", "dialects-export", true,
				"Export benchmark SQL to a dialects file");

		// parse the command line arguments
		CommandLine argsLine = parser.parse(options, args);
		if (argsLine.hasOption("h")) {
			printUsage(options);
			return;
		} else if (!argsLine.hasOption("scenario")) {
			INIT_LOG.fatal("Missing scenario description file");
			System.exit(-1);
		} else
			scenarioFile = argsLine.getOptionValue("scenario");
		if (argsLine.hasOption("r"))
			duration = argsLine.getOptionValue("r");
		if (argsLine.hasOption("runtime"))
			duration = argsLine.getOptionValue("runtime");

		// -------------------------------------------------------------------
		// CREATE TENANT SCHEDULE
		// -------------------------------------------------------------------
		INIT_LOG.info("Create schedule");
		Schedule schedule = new Schedule(duration, scenarioFile);
		HashMap<Integer, ScheduleEvents> tenantEvents = schedule
				.getTenantEvents();
		ArrayList<Integer> tenantList = schedule.getTenantList();

		List<BenchmarkModule> benchList = new ArrayList<BenchmarkModule>();

		for (int tenInd = 0; tenInd < tenantList.size(); tenInd++) {
			int tenantID = tenantList.get(tenInd);
			for (int tenEvent = 0; tenEvent < tenantEvents.get(tenantID).size(); tenEvent++) {

				BenchmarkSettings benchmarkSettings = (BenchmarkSettings) tenantEvents
						.get(tenantID).getBenchmarkSettings(tenEvent);

				// update benchmark Settings
				benchmarkSettings.setTenantID(tenantID);

				// -------------------------------------------------------------------
				// GET PLUGIN LIST
				// -------------------------------------------------------------------
				String plugins = benchmarkSettings.getBenchmark();
				String[] pluginList = plugins.split(",");

				String configFile = benchmarkSettings.getConfigFile();
				XMLConfiguration xmlConfig = new XMLConfiguration(configFile);
				xmlConfig.setExpressionEngine(new XPathExpressionEngine());
				int lastTxnId = 0;

				for (String plugin : pluginList) {

					// ----------------------------------------------------------------
					// WORKLOAD CONFIGURATION
					// ----------------------------------------------------------------

					String pluginTest = "";

					pluginTest = "[@bench='" + plugin + "']";

					WorkloadConfiguration wrkld = new WorkloadConfiguration();
					wrkld.setTenantId(tenantID);
					wrkld.setBenchmarkName(setTenantIDinString(plugin, tenantID));
					wrkld.setXmlConfig(xmlConfig);
					wrkld.setDBType(DatabaseType.get(setTenantIDinString(
							xmlConfig.getString("dbtype"), tenantID)));
					wrkld.setDBDriver(setTenantIDinString(
							xmlConfig.getString("driver"), tenantID));
					wrkld.setDBConnection(setTenantIDinString(
							xmlConfig.getString("DBUrl"), tenantID));
					wrkld.setDBName(setTenantIDinString(
							xmlConfig.getString("DBName"), tenantID));
					wrkld.setDBUsername(setTenantIDinString(
							xmlConfig.getString("username"), tenantID));
					wrkld.setDBPassword(setTenantIDinString(
							xmlConfig.getString("password"), tenantID));
					String terminalString = setTenantIDinString(
							xmlConfig.getString("terminals[not(@bench)]", "0"),
							tenantID);
					int terminals = Integer.parseInt(xmlConfig.getString(
							"terminals" + pluginTest, terminalString));
					wrkld.setTerminals(terminals);
					int taSize = Integer.parseInt(xmlConfig.getString("taSize", "1"));
					if (taSize < 0)
						INIT_LOG.fatal("taSize must not be negative!");
					wrkld.setTaSize(taSize);
					wrkld.setProprietaryTaSyntax(xmlConfig.getBoolean(
							"proprietaryTaSyntax", false));
					wrkld.setIsolationMode(setTenantIDinString(
							xmlConfig.getString("isolation",
									"TRANSACTION_SERIALIZABLE"), tenantID));
					wrkld.setScaleFactor(Double
							.parseDouble(setTenantIDinString(
									xmlConfig.getString("scalefactor", "1.0"),
									tenantID)));
					wrkld.setRecordAbortMessages(xmlConfig.getBoolean(
							"recordabortmessages", false));

					int size = xmlConfig.configurationsAt("/works/work").size();

					for (int i = 1; i < size + 1; i++) {
						SubnodeConfiguration work = xmlConfig
								.configurationAt("works/work[" + i + "]");
						List<String> weight_strings;

						// use a workaround if there multiple workloads or
						// single
						// attributed workload
						if (pluginList.length > 1
								|| work.containsKey("weights[@bench]")) {
							weight_strings = get_weights(plugin, work);
						} else {
							weight_strings = work
									.getList("weights[not(@bench)]");
						}
						int rate = 1;
						boolean rateLimited = true;
						boolean disabled = false;

						// can be "disabled", "unlimited" or a number
						String rate_string;
						rate_string = setTenantIDinString(
								work.getString("rate[not(@bench)]", ""),
								tenantID);
						rate_string = setTenantIDinString(work.getString("rate"
								+ pluginTest, rate_string), tenantID);
						if (rate_string.equals(RATE_DISABLED)) {
							disabled = true;
						} else if (rate_string.equals(RATE_UNLIMITED)) {
							rateLimited = false;
						} else if (rate_string.isEmpty()) {
							LOG.fatal(String
									.format("Tenant "
											+ tenantID
											+ ": Please specify the rate for phase %d and workload %s",
											i, plugin));
							System.exit(-1);
						} else {
							try {
								rate = Integer.parseInt(rate_string);
								if (rate < 1) {
									LOG.fatal("Tenant "
											+ tenantID
											+ ": Rate limit must be at least 1. Use unlimited or disabled values instead.");
									System.exit(-1);
								}
							} catch (NumberFormatException e) {
								LOG.fatal(String
										.format("Tenant "
												+ tenantID
												+ ": Rate string must be '%s', '%s' or a number",
												RATE_DISABLED, RATE_UNLIMITED));
								System.exit(-1);
							}
						}
						Phase.Arrival arrival = Phase.Arrival.REGULAR;
						String arrive = setTenantIDinString(
								work.getString("@arrival", "regular"), tenantID);
						if (arrive.toUpperCase().equals("POISSON"))
							arrival = Phase.Arrival.POISSON;

						int activeTerminals;
						activeTerminals = Integer.parseInt(setTenantIDinString(
								work.getString("active_terminals[not(@bench)]",
										String.valueOf(terminals)), tenantID));
						activeTerminals = Integer.parseInt(setTenantIDinString(
								work.getString("active_terminals" + pluginTest,
										String.valueOf(activeTerminals)),
								tenantID));
						if (activeTerminals > terminals) {
							LOG.fatal("Tenant "
									+ tenantID
									+ ": Configuration error in work "
									+ i
									+ ": number of active terminals"
									+ ""
									+ "is bigger than the total number of terminals");
							System.exit(-1);
						}
						wrkld.addWork(
								Integer.parseInt(setTenantIDinString(
										work.getString("/time"), tenantID)),
								rate, weight_strings, rateLimited, disabled,
								activeTerminals, arrival);
					} // FOR

					int numTxnTypes = xmlConfig.configurationsAt(
							"transactiontypes" + pluginTest
									+ "/transactiontype").size();
					if (numTxnTypes == 0 && pluginList.length == 1) {
						// if it is a single workload run, <transactiontypes />
						// w/o attribute is used
						pluginTest = "[not(@bench)]";
						numTxnTypes = xmlConfig.configurationsAt(
								"transactiontypes" + pluginTest
										+ "/transactiontype").size();
					}
					wrkld.setNumTxnTypes(numTxnTypes);

					// CHECKING INPUT PHASES
					int j = 0;
					for (Phase p : wrkld.getAllPhases()) {
						j++;
						if (p.getWeightCount() != wrkld.getNumTxnTypes()) {
							LOG.fatal(String
									.format("Tenant "
											+ tenantID
											+ ": Configuration files is inconsistent, phase %d contains %d weights but you defined %d transaction types",
											j, p.getWeightCount(),
											wrkld.getNumTxnTypes()));
							System.exit(-1);
						}
					} // FOR

					// Generate the dialect map
					wrkld.init();

					assert (wrkld.getNumTxnTypes() >= 0);
					assert (xmlConfig != null);

					// ----------------------------------------------------------------
					// BENCHMARK MODULE
					// ----------------------------------------------------------------

					String classname = pluginConfig.getString("/plugin[@name='"
							+ plugin + "']");

					if (classname == null) {
						throw new ParseException("Plugin " + plugin
								+ " is undefined in config/plugin.xml");
					}
					BenchmarkModule bench = ClassUtil.newInstance(classname,
							new Object[] { wrkld },
							new Class<?>[] { WorkloadConfiguration.class });
					assert (benchList.get(0) != null);

					Map<String, Object> initDebug = new ListOrderedMap<String, Object>();
					initDebug.put("Benchmark", String.format("%s {%s}",
							plugin.toUpperCase(), classname));
					initDebug.put("Configuration", configFile);
					initDebug.put("Type", wrkld.getDBType());
					initDebug.put("Driver", wrkld.getDBDriver());
					initDebug.put("URL", wrkld.getDBConnection());
					initDebug.put(
							"Isolation",
							setTenantIDinString(xmlConfig.getString(
									"isolation",
									"TRANSACTION_SERIALIZABLE [DEFAULT]"),
									tenantID));
					initDebug.put("Scale Factor", wrkld.getScaleFactor());
					INIT_LOG.info(SINGLE_LINE + "\n\n"
							+ StringUtil.formatMaps(initDebug));
					INIT_LOG.info(SINGLE_LINE);

					// Load TransactionTypes
					List<TransactionType> ttypes = new ArrayList<TransactionType>();

					// Always add an INVALID type for Carlo
					ttypes.add(TransactionType.INVALID);
					int txnIdOffset = lastTxnId;
					for (int i = 1; i < wrkld.getNumTxnTypes() + 1; i++) {
						String key = "transactiontypes" + pluginTest
								+ "/transactiontype[" + i + "]";
						String txnName = setTenantIDinString(
								xmlConfig.getString(key + "/name"), tenantID);
						int txnId = i + 1;
						if (xmlConfig.containsKey(key + "/id")) {
							txnId = Integer
									.parseInt(setTenantIDinString(
											xmlConfig.getString(key + "/id"),
											tenantID));
						}
						ttypes.add(bench.initTransactionType(txnName, txnId
								+ txnIdOffset));
						lastTxnId = i;
					} // FOR
					TransactionTypes tt = new TransactionTypes(ttypes);
					wrkld.setTransTypes(tt);
					if (benchmarkSettings.getBenchmarkSlaFile() != null)
						wrkld.setSlaFromFile(benchmarkSettings
								.getBenchmarkSlaFile());
					LOG.debug("Tenant " + tenantID
							+ ": Using the following transaction types: " + tt);

					bench.setTenantOffset(tenantEvents.get(tenantID).getTime(
							tenEvent));
					bench.setTenantID(tenantID);
					bench.setBenchmarkSettings(benchmarkSettings);
					benchList.add(bench);
				}
			}
		}
		// create result collector
		ResultCollector rCollector = new ResultCollector(tenantList);

		// execute benchmarks in parallel
		ArrayList<Thread> benchThreads = new ArrayList<Thread>();
		for (BenchmarkModule benchmark : benchList) {
			BenchmarkExecutor benchThread = new BenchmarkExecutor(benchmark,
					argsLine);
			Thread t = new Thread(benchThread);
			t.start();
			benchThreads.add(t);
			benchmark.getWorkloadConfiguration().setrCollector(rCollector);
		}

		// waiting for completion of all benchmarks
		for (Thread t : benchThreads) {
			t.join();
		}

		// print statistics
		int analysisBuckets = -1;
		if (argsLine.hasOption("analysis-buckets"))
			analysisBuckets = Integer.parseInt(argsLine
					.getOptionValue("analysis-buckets"));
		String output = null;
		if (argsLine.hasOption("o"))
			output = argsLine.getOptionValue("o");
		String baseline = null;
		if (argsLine.hasOption("b"))
			baseline = argsLine.getOptionValue("b");

		rCollector.printStatistics(output, analysisBuckets,
				argsLine.hasOption("histograms"), baseline);

		// create GUI
		if (argsLine.hasOption("g") && (!rCollector.getAllResults().isEmpty())) {
			try {
				Gui gui = new Gui(Integer.parseInt(argsLine.getOptionValue(
						"analysis-buckets", "10")), rCollector, output);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * buggy piece of shit of Java XPath implementation made me do it replaces
	 * good old [@bench="{plugin_name}", which doesn't work in Java XPath with
	 * lists
	 */
	private static List<String> get_weights(String plugin,
			SubnodeConfiguration work) {

		List<String> weight_strings = new LinkedList<String>();
		@SuppressWarnings("unchecked")
		List<SubnodeConfiguration> weights = work.configurationsAt("weights");
		boolean weights_started = false;

		for (SubnodeConfiguration weight : weights) {

			// stop if second attributed node encountered
			if (weights_started && weight.getRootNode().getAttributeCount() > 0) {
				break;
			}
			// start adding node values, if node with attribute equal to current
			// plugin encountered
			if (weight.getRootNode().getAttributeCount() > 0
					&& weight.getRootNode().getAttribute(0).getValue()
							.equals(plugin)) {
				weights_started = true;
			}
			if (weights_started) {
				weight_strings.add(weight.getString(""));
			}

		}
		return weight_strings;
	}

	private static void printUsage(Options options) {
		HelpFormatter hlpfrmt = new HelpFormatter();
		hlpfrmt.printHelp("MuTeBench", options);
	}

	public static String setTenantIDinString(String value, int tenantID) {
		if (value != null && value.startsWith("@") && value.endsWith("@")) {
			return value.substring(1, value.length() - 1).replaceAll("#tid",
					String.valueOf(tenantID));
		} else
			return value;
	}
}
