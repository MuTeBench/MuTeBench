package com.oltpbenchmark.multitenancy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.log4j.Logger;

import com.oltpbenchmark.ThreadBench;
import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.util.QueueLimitException;

public class BenchmarkExecutor implements Runnable {
	private static final Logger LOG = Logger.getLogger(BenchmarkExecutor.class);
	private static final Logger CREATE_LOG = Logger
			.getLogger(BenchmarkExecutor.class);
	private static final Logger LOAD_LOG = Logger
			.getLogger(BenchmarkExecutor.class);
	private static final Logger SCRIPT_LOG = Logger
			.getLogger(BenchmarkExecutor.class);
	private static final Logger EXEC_LOG = Logger
			.getLogger(BenchmarkExecutor.class);
	private static final String SINGLE_LINE = "**********************************************************************************";

	BenchmarkModule bench;
	CommandLine argsLine;

	public BenchmarkExecutor(BenchmarkModule bench, CommandLine argsLine) {
		super();
		this.bench = bench;
		this.argsLine = argsLine;
	}

	@Override
	public void run() {
		try {
			LOG.info("Tenant " + bench.getTenantID()
					+ ": Executing workload in " + bench.getTenantOffset()
					* 1000 + "ms");
			Thread.sleep(bench.getTenantOffset() * 1000);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		LOG.info("Tenant " + bench.getTenantID() + ": Execute workload");

		BenchmarkSettings benchsettings = (BenchmarkSettings) bench
				.getBenchmarkSettings();

		// Export StatementDialects
		if (isBooleanOptionSet(argsLine, "dialects-export")) {
			if (bench.getStatementDialects() != null) {
				LOG.info("Tenant " + bench.getTenantID()
						+ ": Exporting StatementDialects for " + bench);
				String xml = bench.getStatementDialects().export(
						bench.getWorkloadConfiguration().getDBType(),
						bench.getProcedures().values());
				System.out.println(xml);
				System.exit(0);
			}
			throw new RuntimeException("Tenant " + bench.getTenantID()
					+ ": No StatementDialects is available for " + bench);
		}

		@Deprecated
		boolean verbose = argsLine.hasOption("v");

		// Create service level agreements
		try {
			if (bench.getWorkloadConfiguration().getTenantSla() != null
					&& benchsettings.isSendSla())
				bench.forwardSLAtoDB();
		} catch (Exception e) {
			e.printStackTrace();
		}

		// Create the Benchmark's Database
		if (benchsettings.isCreate()) {
			CREATE_LOG.info("Tenant " + bench.getTenantID() + ": Creating new "
					+ bench.getBenchmarkName().toUpperCase() + " database...");
			bench.createDatabase();
			CREATE_LOG.info("Tenant " + bench.getTenantID() + ": Finished!");
			CREATE_LOG.info(SINGLE_LINE);
		} else if (CREATE_LOG.isDebugEnabled()) {
			CREATE_LOG.debug("Tenant " + bench.getTenantID()
					+ ": Skipping creating benchmark database tables");
			CREATE_LOG.info(SINGLE_LINE);
		}

		// Clear the Benchmark's Database
		if (benchsettings.isClear()) {
			CREATE_LOG.info("Tenant " + bench.getTenantID() + ": Resetting "
					+ bench.getBenchmarkName().toUpperCase() + " database...");
			bench.clearDatabase();
			CREATE_LOG.info("Tenant " + bench.getTenantID() + ": Finished!");
			CREATE_LOG.info(SINGLE_LINE);
		} else if (CREATE_LOG.isDebugEnabled()) {
			CREATE_LOG.debug("Tenant " + bench.getTenantID()
					+ ": Skipping creating benchmark database tables");
			CREATE_LOG.info(SINGLE_LINE);
		}

		// Execute Loader
		if (benchsettings.isLoad()) {
			LOAD_LOG.info("Tenant " + bench.getTenantID()
					+ ": Loading data into "
					+ bench.getBenchmarkName().toUpperCase() + " database...");
			bench.loadDatabase();
			LOAD_LOG.info("Tenant " + bench.getTenantID() + ": Finished!");
			LOAD_LOG.info(SINGLE_LINE);
		} else if (LOAD_LOG.isDebugEnabled()) {
			LOAD_LOG.debug("Tenant " + bench.getTenantID()
					+ ": Skipping loading benchmark database records");
			LOAD_LOG.info(SINGLE_LINE);
		}

		// Execute a Script
		if (benchsettings.isRunscript()) {
			String script = benchsettings.getScriptname();
			SCRIPT_LOG.info("Tenant " + bench.getTenantID()
					+ ": Running a SQL script: " + script);
			bench.runScript(script);
			SCRIPT_LOG.info("Tenant " + bench.getTenantID() + ": Finished!");
			SCRIPT_LOG.info(SINGLE_LINE);
		}

		// Execute Workload
		if (benchsettings.isExecute()) {
			// Bombs away!
			List<? extends Worker> finishedWorkers = null;
			try {
				finishedWorkers = runWorkload(bench, verbose);
			} catch (Throwable ex) {
				LOG.error("Tenant " + bench.getTenantID()
						+ ": Unexpected error when running benchmarks.", ex);
				System.exit(1);
			}
			assert (finishedWorkers != null);
			EXEC_LOG.info("Tenant " + bench.getTenantID()
					+ ": workload finished");
			bench.getWorkloadConfiguration().getrCollector()
					.collectEventResults(finishedWorkers, bench.getTenantID());
		} else {
			EXEC_LOG.info("Tenant " + bench.getTenantID()
					+ ": Skipping benchmark workload execution");
		}
	}

	/**
	 * Returns true if the given key is in the CommandLine object and is set to
	 * true.
	 * 
	 * @param argsLine
	 * @param key
	 * @return
	 */
	private static boolean isBooleanOptionSet(CommandLine argsLine, String key) {
		if (argsLine.hasOption(key)) {
			LOG.debug("CommandLine has option '" + key
					+ "'. Checking whether set to true");
			String val = argsLine.getOptionValue(key);
			LOG.debug(String.format("CommandLine %s => %s", key, val));
			return (val != null ? val.equalsIgnoreCase("true") : false);
		}
		return (false);
	}

	private static List<? extends Worker> runWorkload(BenchmarkModule bench,
			boolean verbose) throws QueueLimitException, IOException {
		List<Worker> workers = new ArrayList<Worker>();
		List<WorkloadConfiguration> workConfs = new ArrayList<WorkloadConfiguration>();
		EXEC_LOG.info("Tenant " + bench.getTenantID() + ": Creating "
				+ bench.getWorkloadConfiguration().getTerminals()
				+ " virtual terminals...");
		workers.addAll(bench.makeWorkers(verbose));
		// EXEC_LOG.info("done.");
		EXEC_LOG.info(String.format("Tenant " + bench.getTenantID()
				+ ": Launching the %s Benchmark with %s Phases...", bench
				.getBenchmarkName(), bench.getWorkloadConfiguration()
				.getNumberOfPhases()));
		workConfs.add(bench.getWorkloadConfiguration());

		List<? extends Worker> finishedWorkers = ThreadBench
				.runRateLimitedBenchmark(workers, workConfs);
		EXEC_LOG.info(SINGLE_LINE);
		return finishedWorkers;
	}

}
