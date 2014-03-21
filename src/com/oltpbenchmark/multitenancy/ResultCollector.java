package com.oltpbenchmark.multitenancy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.oltpbenchmark.LatencyRecord;
import com.oltpbenchmark.Results;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.multitenancy.sla.Penalty;

public class ResultCollector {

	private static final Logger LOG = Logger.getLogger(MuTeBench.class);
	public static final long startMillis = System.currentTimeMillis();
	public static final long startNanos = System.nanoTime();

	HashMap<Integer, TenantResults> tenantResultsMap = new HashMap<Integer, TenantResults>();
	ArrayList<Integer> tenantList;
	TenantResults overallResults = new TenantResults();
	HashMap<Integer, Integer> workerMap = new HashMap<Integer, Integer>();

	String output;
	String baseline;
	int analysisBuckets;
	boolean histograms;

	public ResultCollector(ArrayList<Integer> tenantList) {
		super();
		this.tenantList = tenantList;
		overallResults.setResults(new Results(0, 0,
				new ArrayList<LatencyRecord.Sample>()));
	}

	public void printStatistics(String output, int analysisBuckets,
			boolean histograms, String baseline) {
		this.output = output;
		this.baseline = baseline;
		this.analysisBuckets = analysisBuckets;
		this.histograms = histograms;
		printTenantStatistics();
		printSystemStatistics();
	}

	private void printTenantStatistics() {
		// print statistics for all tenants
		for (int i = 0; i < tenantList.size(); i++) {
			int tenantId = tenantList.get(i);
			TenantResults r = tenantResultsMap.get(tenantId);
			r.calcResults();
			// go to next tenant, if no results are available
			if (r == null) {
				continue;
			}
			
			PrintStream abs = System.out, rel = System.out, raw = System.out, his = System.out;
			if (output != null) {
				try {
					raw = new PrintStream(new File(output + "_tenant"
							+ tenantId + ".raw"));
					LOG.info("Tenant " + tenantId
							+ ": Write raw data into file: " + output
							+ "_tenant" + tenantId + ".raw");

					if (analysisBuckets > 0) {
						abs = new PrintStream(new File(output + "_tenant"
							+ tenantId + ".abs"));
						LOG.info("Tenant "
								+ tenantId
							+ ": Write absolute performance values into file: "
							+ output + "_tenant" + tenantId + ".abs");
					}

					if (baseline != null) {
						rel = new PrintStream(new File(output + "_tenant"
								+ tenantId + ".rel"));
						LOG.info("Tenant "
								+ tenantId
								+ ": Write relative performance values into file: "
								+ output + "_tenant" + tenantId + ".rel");
					}

					if (histograms) {
						his = new PrintStream(new File(output + "_tenant"
								+ tenantId + ".his"));
						LOG.info("Tenant " + tenantId
								+ ": Write output histogram into file: "
								+ output + "_tenant" + tenantId + ".his");
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} else if (LOG.isDebugEnabled()) {
				LOG.debug("Tenant " + tenantId + ": No output file specified");
			}

			// write performance data
			r.getResults().writeRAW(raw, workerMap);
			if (analysisBuckets > 0) {
				LOG.info("Tenant " + tenantId
						+ ": Results are grouped into Buckets of "
						+ analysisBuckets + " seconds");
				r.getResults().writeAbsPerf(analysisBuckets, abs, true, r);
				try {
					if (baseline != null) {
						r.getResults().writeRelPerf(analysisBuckets, rel,
								baseline + "_tenant" + tenantId + ".rel", r);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (LOG.isDebugEnabled()) {
				LOG.debug("Tenant " + tenantId + ": No bucket size specified");
			}

			// write histograms
			if (histograms) {
				r.getResults().writeHistograms(his);
			}

			// write raw data and close files
			if (output != null) {
				abs.close();
				raw.close();
				his.close();
				rel.close();
			}
		}
	}

	private void printSystemStatistics() {
		// merge results from all tenants without another result calculation
		for (int i = 0; i < tenantList.size(); i++) {
			TenantResults r = tenantResultsMap.get(tenantList.get(i));
			// go to next tenant if no results are available
			if (r == null)
				continue;
			overallResults.addWorkers(r.getWorkers());
			overallResults.getResults().addResults(r.getResults());
			if (r.getPenalties() != null) {
				for (Penalty penalty : r.getPenalties()) {
					overallResults.setPenalties(overallResults.addPenalty(
							overallResults.getPenalties(), penalty));
				}
			}
		}
		if (!overallResults.getResults().getLatencySamples().isEmpty()) {
			Collections.sort(overallResults.getResults().getLatencySamples());

			PrintStream abs = System.out, overview = System.out, raw = System.out, his = System.out;
			if (output != null) {
				try {
					raw = new PrintStream(new File(output + "_overall.raw"));
					LOG.info("System statistics: Write raw data "
							+ "into file: " + output + "_overall.raw");

					if (analysisBuckets > -1) {
						abs = new PrintStream(new File(output + "_overall.abs"));
						LOG.info("System statistics: Write absolute performance values "
								+ "into file: " + output + "_overall.abs");

						overview = new PrintStream(new File(output
								+ "_tenant_overview.abs"));
						LOG.info("System statistics: Write tenant performance overview "
								+ "into file: "
								+ output
								+ "_tenant_overview.abs");
					}

					if (histograms) {
						his = new PrintStream(new File(output + "_overall.his"));
						LOG.info("System statistics: Write histogram "
								+ "into file: " + output + "_overall.his");
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			} else if (LOG.isDebugEnabled()) {
				LOG.debug("System statistics: No output file specified");
			}

			// write performance data
			overallResults.getResults().writeRAW(raw, workerMap);
			if (analysisBuckets > -1) {
				// write system overview
				overallResults.getResults().writeAbsPerf(analysisBuckets, abs,
						true, overallResults);
				// write results for each tenant
				for (int i = 0; i < tenantList.size(); i++) {
					TenantResults r = tenantResultsMap.get(tenantList.get(i));
					if (r != null) {
						r.getResults().writeAbsPerf(analysisBuckets, overview,
								i == 0, overallResults);
					}
				}
			} else if (LOG.isDebugEnabled()) {
				LOG.debug("System statistics: No bucket size specified");
			}

			// write histograms
			if (histograms) {
				overallResults.getResults().writeHistograms(his);
			}

			// write raw data and close files
			if (output != null) {
				abs.close();
				overview.close();
				raw.close();
				his.close();
			}
		}
	}

	public void collectEventResults(List<? extends Worker> workers, int tenantId) {
		synchronized (workerMap) {
			for (Worker worker : workers) {
				workerMap.put(worker.getId(), tenantId);
			}
		}
		synchronized (tenantResultsMap) {
			if (!tenantList.contains(tenantId))
				LOG.fatal("Collect results for unknown tenant!");
			else {
				if (!tenantResultsMap.containsKey(tenantId)
						|| tenantResultsMap.get(tenantId) == null) {
					TenantResults tenResults = new TenantResults();
					tenResults.addWorkers(workers);
					tenantResultsMap.put(tenantId, tenResults);
				} else {
					TenantResults oldR = tenantResultsMap.get(tenantId);
					oldR.addWorkers(workers);
					tenantResultsMap.put(tenantId, oldR);
				}
			}
		}
	}

	public HashMap<Integer, TenantResults> getAllResults() {
		return tenantResultsMap;
	}

	public TenantResults getResults(Integer id) {
		return tenantResultsMap.get(id);
	}

	public ArrayList<Integer> getTenantList() {
		return tenantList;
	}
}
