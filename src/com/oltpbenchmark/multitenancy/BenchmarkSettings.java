package com.oltpbenchmark.multitenancy;

import org.apache.commons.configuration.SubnodeConfiguration;

import com.oltpbenchmark.multitenancy.schedule.ScenarioConfigElements;

public class BenchmarkSettings {
	String configFile;
	String benchmark;
	String benchmarkSlaFile;
	String baselineFile = null;
	boolean sendSla = false;
	boolean create = false;
	boolean clear = false;
	boolean load = false;
	boolean execute = false;
	boolean runscript = false;
	String scriptname = null;

	public BenchmarkSettings(SubnodeConfiguration config) {
		configFile = config.getString(ScenarioConfigElements.CONFIG_FILE_KEY);
		baselineFile = config.getString(
				ScenarioConfigElements.BASELINE_FILE_KEY, null);
		benchmark = config.getString(ScenarioConfigElements.BENCHMARK_KEY);
		benchmarkSlaFile = config.getString(
				ScenarioConfigElements.BENCHMARK_SLA_FILE_KEY, null);
		String[] actionsString = config
				.getStringArray(ScenarioConfigElements.ACTION_KEY);
		for (int i = 0; i < actionsString.length; i++) {
			if (actionsString[i].trim().equalsIgnoreCase(
					ScenarioConfigElements.ACTION_CREATE_KEY)) {
				create = true;
			}
			if (actionsString[i].trim().equalsIgnoreCase(
					ScenarioConfigElements.ACTION_CLEAR_KEY)) {
				clear = true;
			}
			if (actionsString[i].trim().equalsIgnoreCase(
					ScenarioConfigElements.ACTION_LOAD_KEY)) {
				load = true;
			}
			if (actionsString[i].trim().equalsIgnoreCase(
					ScenarioConfigElements.ACTION_EXECUTE_KEY)) {
				execute = true;
			}
			if (actionsString[i].trim().equalsIgnoreCase(
					ScenarioConfigElements.ACTION_RUNSCRIPT_KEY)) {
				runscript = true;
			}
		}
		if (runscript) {
			scriptname = config.getString(
					ScenarioConfigElements.ACTION_SCRIPTNAME_KEY, null);
		}
		sendSla = config.getBoolean(ScenarioConfigElements.SEND_SLA_KEY, false);
		if (scriptname != null)
			runscript = true;
	}

	public String getConfigFile() {
		return configFile;
	}

	public String getBaselineFile() {
		return baselineFile;
	}

	public String getBenchmark() {
		return benchmark;
	}

	public String getBenchmarkSlaFile() {
		return benchmarkSlaFile;
	}

	public boolean isCreate() {
		return create;
	}

	public boolean isClear() {
		return clear;
	}

	public boolean isLoad() {
		return load;
	}

	public boolean isExecute() {
		return execute;
	}

	public boolean isRunscript() {
		return runscript;
	}

	public String getScriptname() {
		return scriptname;
	}

	public boolean isSendSla() {
		return sendSla;
	}

	public void setTenantID(int tenantID) {
		configFile = MuTeBench.setTenantIDinString(configFile, tenantID);
		benchmark = MuTeBench.setTenantIDinString(benchmark, tenantID);
		benchmarkSlaFile = MuTeBench.setTenantIDinString(benchmarkSlaFile,
				tenantID);
		baselineFile = MuTeBench.setTenantIDinString(baselineFile, tenantID);
		scriptname = MuTeBench.setTenantIDinString(scriptname, tenantID);
	}
}
