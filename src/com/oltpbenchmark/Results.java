/*******************************************************************************
 * oltpbenchmark.com
 *  
 *  Project Info:  http://oltpbenchmark.com
 *  Project Members:    Carlo Curino <carlo.curino@gmail.com>
 *              Evan Jones <ej@evanjones.ca>
 *              DIFALLAH Djellel Eddine <djelleleddine.difallah@unifr.ch>
 *              Andy Pavlo <pavlo@cs.brown.edu>
 *              CUDRE-MAUROUX Philippe <philippe.cudre-mauroux@unifr.ch>  
 *                  Yang Zhang <yaaang@gmail.com> 
 * 
 *  This library is free software; you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Foundation;
 *  either version 3.0 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 ******************************************************************************/
package com.oltpbenchmark;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.oltpbenchmark.LatencyRecord.Sample;
import com.oltpbenchmark.ThreadBench.TimeBucketIterable;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.multitenancy.ResultCollector;
import com.oltpbenchmark.multitenancy.TenantResults;
import com.oltpbenchmark.multitenancy.sla.Penalty;
import com.oltpbenchmark.util.Histogram;
import com.oltpbenchmark.util.StringUtil;

public final class Results{
	final static long MILLISECONDS_FACTOR = (long)1e3;
	
	private long nanoSeconds;
	private int measuredRequests;
	HashMap<Integer, String> baselineResults;	

	public Histogram<TransactionType> txnSuccess = new Histogram<TransactionType>(true);
	public Histogram<TransactionType> txnAbort = new Histogram<TransactionType>(true);
	public Histogram<TransactionType> txnRetry = new Histogram<TransactionType>(true);
	public Histogram<TransactionType> txnErrors = new Histogram<TransactionType>(true);
	public Map<TransactionType, Histogram<String>> txnAbortMessages = new HashMap<TransactionType, Histogram<String>>();

	private List<LatencyRecord.Sample> latencySamples;

	public Results(long nanoSeconds, int measuredRequests,
			List<LatencyRecord.Sample> latencySamples) {
		this.nanoSeconds = nanoSeconds;
		this.measuredRequests = measuredRequests;
		this.latencySamples = latencySamples;
		Locale.setDefault(new Locale("en", "US"));
	}

	/**
	 * adds results of a given results-object to this object
	 * 
	 * @param newResults
	 */
	public synchronized void addResults(Results newResults) {
		nanoSeconds = +newResults.getNanoSeconds();
		measuredRequests = +newResults.getMeasuredRequests();
		latencySamples.addAll(newResults.getLatencySamples());

		// update histograms
		txnSuccess.putHistogram(newResults.txnSuccess);
		txnAbort.putHistogram(newResults.txnAbort);
		txnRetry.putHistogram(newResults.txnRetry);
		txnErrors.putHistogram(newResults.txnErrors);
		txnAbortMessages.putAll(newResults.txnAbortMessages);
	}

	public double getRequestsPerSecond() {
		return (double) measuredRequests / (double) nanoSeconds * 1e9;
	}

	@Override
	public String toString() {
		return "Results(nanoSeconds=" + nanoSeconds + ", measuredRequests="
				+ measuredRequests + ") = " + getRequestsPerSecond()
				+ " requests/sec";
	}

	public void writeAbsPerf(int windowSizeSeconds, PrintStream out, boolean header, TenantResults results) {
		if (header) {
			out.print("time(sec); throughput(req/sec); avg_lat(ms); min_lat(ms); 25th_lat(ms); "
				+ "median_lat(ms); 75th_lat(ms); 90th_lat(ms); 95th_lat(ms); 99th_lat(ms); max_lat(ms)");
			if (results.getPenalties()!=null) out.print(";sla_penalty(US$)");
		}
		int i = 0;
		
		for (DistributionStatistics s : new TimeBucketIterable(latencySamples,
				windowSizeSeconds)) {
			//only write data if workers were active
			if (!results.isActivityInterval(ResultCollector.startMillis+i*(windowSizeSeconds*MILLISECONDS_FACTOR)+1, 
					ResultCollector.startMillis+(i+1)*(windowSizeSeconds*MILLISECONDS_FACTOR))) continue;
			out.printf(
					"\n%d;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f", i
							* windowSizeSeconds, (double) s.getCount()
							/ windowSizeSeconds, s.getAverage()
							/ MILLISECONDS_FACTOR, s.getMinimum()
							/ MILLISECONDS_FACTOR, s.get25thPercentile()
							/ MILLISECONDS_FACTOR, s.getMedian()
							/ MILLISECONDS_FACTOR, s.get75thPercentile()
							/ MILLISECONDS_FACTOR, s.get90thPercentile()
							/ MILLISECONDS_FACTOR, s.get95thPercentile()
							/ MILLISECONDS_FACTOR, s.get99thPercentile()
							/ MILLISECONDS_FACTOR, s.getMaximum()
							/ MILLISECONDS_FACTOR);
			if (results.getPenalties()!=null) {
				//search for relevant penalties and print their added penalty amounts
				boolean penaltyFound = false;
				int penaltyAmount = 0; 
				for (Penalty penalty : results.getPenalties()) {					
					if (ResultCollector.startMillis+(i-1)*(windowSizeSeconds*MILLISECONDS_FACTOR)<penalty.getTime()
							&&ResultCollector.startMillis+i*(windowSizeSeconds*MILLISECONDS_FACTOR)>=penalty.getTime()) {
						penaltyFound = true;
						penaltyAmount+=penalty.getAmount();
					}
				}
				if (!penaltyFound) out.println(";0");
				else out.println(";"+penaltyAmount);
			}
			i++;
		}
	}
	
	public void writeRelPerf(int windowSizeSeconds, PrintStream out, String baselineFile, TenantResults results) throws Exception {
		//parse baseline file and get results
		baselineResults = new HashMap<Integer, String>();
		try {
			BufferedReader in = new BufferedReader(new FileReader(baselineFile));
			String line = null;
			boolean dataArea = false;
			while ((line = in.readLine()) != null) {
				if (line.startsWith("0;")) {
					dataArea = true;
				}
				if (dataArea) baselineResults.put(Integer.parseInt(line.split(";")[0]), line);
			}
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		} 
		
		out.print("time(sec); throughput(%); avg_lat(%); min_lat(%); 25th_lat(%); "
				+ "median_lat(%); 75th_lat(%); 90th_lat(%); 95th_lat(%); 99th_lat(%); max_lat(%)");
		if (results.getPenalties()!=null) out.print(";sla_penalty(%)");
		
		int i = 0;
		for (DistributionStatistics s : new TimeBucketIterable(latencySamples,
				windowSizeSeconds)) {
			//only write data if workers were active
			if (!results.isActivityInterval(ResultCollector.startMillis+i*(windowSizeSeconds*MILLISECONDS_FACTOR)+1, 
					ResultCollector.startMillis+(i+1)*(windowSizeSeconds*MILLISECONDS_FACTOR))) continue;
			
			String[] base = baselineResults.get(i * windowSizeSeconds).split(";");
			if (base==null) {
				throw new Exception("Baseline file doesn't contain a value for time slot"+i * windowSizeSeconds+
						". Did you use a different bucket size or benchmark duration in your baseline run?");
			}
			out.printf(
					"%d;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f;%.3f\n", 
					i * windowSizeSeconds, (double) s.getCount()
					/ windowSizeSeconds/Float.parseFloat(base[1]), 1/((s.getAverage()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[2])), 1/((s.getMinimum()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[3])), 1/((s.get25thPercentile()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[4])), 1/((s.getMedian()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[5])), 1/((s.get75thPercentile()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[6])), 1/((s.get90thPercentile()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[7])), 1/((s.get95thPercentile()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[8])), 1/((s.get99thPercentile()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[9])), 1/((s.getMaximum()
					/ MILLISECONDS_FACTOR)/Float.parseFloat(base[10])));
			if (results.getPenalties()!=null) {
				//search for relevant penalties, add their penalty amounts and calc relative value
				boolean penaltyFound = false;
				int penaltyAmount = 0; 
				
				for (Penalty penalty : results.getPenalties()) {					
					if (ResultCollector.startMillis+(i-1)*(windowSizeSeconds*MILLISECONDS_FACTOR)<penalty.getTime()
							&&ResultCollector.startMillis+i*(windowSizeSeconds*MILLISECONDS_FACTOR)>=penalty.getTime()) {
						penaltyFound = true;
						penaltyAmount+=penalty.getAmount();
					}
				}
				if (!penaltyFound||base.length<12) out.println(";-");
				else {
					out.println(";"+1/(penaltyAmount/Integer.parseInt(base[11])));
				}
			}
			i++;
		}
	}

	public void writeHistograms(PrintStream out) {
		out.println("Completed Transactions:\n" + txnSuccess + "\n");
		out.println("Aborted Transactions:\n" + txnAbort + "\n");
		out.println("Rejected Transactions:\n" + txnRetry + "\n");
		out.println("Unexpected Errors:\n" + txnErrors + "\n");
		if (!txnAbortMessages.isEmpty())
			out.println("User Aborts:\n"
					+ StringUtil.formatMaps(txnAbortMessages));
	}

	public void writeRAW(PrintStream out, HashMap<Integer, Integer> workerMap) {

		// This is needed because nanTime does not guarantee offset... we
		// ground it (and round it) to ms from 1970-01-01 like currentTime
		double x = ((double) System.nanoTime() / (double) 1000000000);
		double y = ((double) System.currentTimeMillis() / (double) 1000);
		double offset = x - y;

		// long startNs = latencySamples.get(0).startNs;
		if (workerMap!=null) {
			out.println("tenant id; transaction type (index in config file); start time (microseconds); latency (microseconds); worker id(start number); phase id(index in config file)");
			for (Sample s : latencySamples) {
				double startUs = ((double) s.startNs / (double) 1000000000);
				out.println(workerMap.get(s.workerId)+";"+s.tranType + ";"
						+ String.format("%10.6f", startUs - offset) + ";"
						+ s.latencyUs + ";" + s.workerId + ";" + s.phaseId);
			}
		} else {
			out.println("transaction type (index in config file); start time (microseconds); latency (microseconds); worker id(start number); phase id(index in config file)");
			for (Sample s : latencySamples) {
				double startUs = ((double) s.startNs / (double) 1000000000);
				out.println(s.tranType + ";"
						+ String.format("%10.6f", startUs - offset) + ";"
						+ s.latencyUs + ";" + s.workerId + ";" + s.phaseId);
			}
		}
	}

	public long getNanoSeconds() {
		return nanoSeconds;
	}

	public int getMeasuredRequests() {
		return measuredRequests;
	}

	public List<LatencyRecord.Sample> getLatencySamples() {
		return latencySamples;
	}

	public HashMap<Integer, String> getBaselineResults() {
		return baselineResults;
	}	
}