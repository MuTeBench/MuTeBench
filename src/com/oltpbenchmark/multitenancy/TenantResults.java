package com.oltpbenchmark.multitenancy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.oltpbenchmark.DistributionStatistics;
import com.oltpbenchmark.LatencyRecord;
import com.oltpbenchmark.Results;
import com.oltpbenchmark.ThreadBench.TimeBucketIterable;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.multitenancy.gui.Metric;
import com.oltpbenchmark.multitenancy.sla.Penalty;
import com.oltpbenchmark.multitenancy.sla.ServiceLevelAgreement;
import com.oltpbenchmark.multitenancy.sla.WorkloadRunSLAs;
import com.oltpbenchmark.util.Histogram;

public class TenantResults {
	private List<Worker> workers;
	private Results results;
	private ArrayList<WorkloadRunSLAs> tenantSLAs = new ArrayList<WorkloadRunSLAs>();
	private ArrayList<LatencyRecord.Sample> samples = new ArrayList<LatencyRecord.Sample>();
	private ArrayList<Penalty> penalties;
	private int overallPenaltyAmount = 0;

	public TenantResults() {
	}

	public List<Worker> getWorkers() {
		return workers;
	}

	@SuppressWarnings("unchecked")
	public void addWorkers(List<? extends Worker> workers) {
		if (this.workers == null) {
			// TODO: We don't need extensions by benchmark workers -> this
			// should be easier to handle
			this.workers = (List<Worker>) workers;
		} else {
			this.workers.addAll(workers);
		}
	}

	public Results getResults() {
		return results;
	}

	public void setResults(Results results) {
		this.results = results;
	}

	public void calcResults() {
		long start = Long.MAX_VALUE;
		long measureEnd = Long.MIN_VALUE;
		int requests = 0;
		for (Worker w : workers) {
			if (w.getWorkloadConfiguration().getTenantSla() != null) {
				addSla(w.getWorkloadConfiguration().getTenantSla());
			}
			if (w.getStart() < start)
				start = w.getStart();
			if (w.getMeasureEnd() > measureEnd)
				measureEnd = w.getMeasureEnd();
			requests += w.getRequests();
			for (LatencyRecord.Sample sample : w.getLatencyRecords()) {
				samples.add(sample);
			}
		}
		Collections.sort(samples);

		// Compute statistics on all the latencies
		int[] latencies = new int[samples.size()];
		for (int i = 0; i < samples.size(); ++i) {
			latencies[i] = samples.get(i).latencyUs;
		}

		results = new Results(measureEnd - start, requests, samples);

		// Compute transaction histogram
		Set<TransactionType> txnTypes = new HashSet<TransactionType>();
		for (Worker w : workers) {
			txnTypes.addAll(w.getWorkloadConfiguration().getTransTypes());
		}
		txnTypes.remove(TransactionType.INVALID);

		results.txnSuccess.putAll(txnTypes, 0);
		results.txnRetry.putAll(txnTypes, 0);
		results.txnAbort.putAll(txnTypes, 0);
		results.txnErrors.putAll(txnTypes, 0);

		for (Worker w : workers) {
			results.txnSuccess.putHistogram(w.getTransactionSuccessHistogram());
			results.txnRetry.putHistogram(w.getTransactionRetryHistogram());
			results.txnAbort.putHistogram(w.getTransactionAbortHistogram());
			results.txnErrors.putHistogram(w.getTransactionErrorHistogram());

			for (Entry<TransactionType, Histogram<String>> e : w
					.getTransactionAbortMessageHistogram().entrySet()) {
				Histogram<String> h = results.txnAbortMessages.get(e.getKey());
				if (h == null) {
					h = new Histogram<String>(true);
					results.txnAbortMessages.put(e.getKey(), h);
				}
				h.putHistogram(e.getValue());
			} // FOR
		} // FOR
		// calc penalties if required
		if (!tenantSLAs.isEmpty()) {
			calcPenalties();
		}
	}

	private void addSla(WorkloadRunSLAs newSLA) {
		// add first element
		if (tenantSLAs.isEmpty()) {
			tenantSLAs.add(newSLA);
		} else {
			// check if element already exists
			boolean slaExists = false;
			for (int i = 0; i < tenantSLAs.size(); i++) {
				// replace equal events only for earlier start times to cover
				// the hole SLA time
				if (tenantSLAs.get(i).equals(newSLA)) {
					slaExists = true;
					if (tenantSLAs.get(i).getStart() > newSLA.getStart()) {
						tenantSLAs.set(i, newSLA);
					}
				}
			}
			// add new SLASs
			if (!slaExists)
				tenantSLAs.add(newSLA);
		}
	}

	public void calcPenalties() {
		orderSLAs();
		// iterate over all runs
		for (int j = 0; j < tenantSLAs.size(); j++) {
			// iterate over all SLAs within a run
			for (ServiceLevelAgreement sla : tenantSLAs.get(j).getSLAs()) {
				// iterate over all
				int i = 0;
				for (DistributionStatistics s : new TimeBucketIterable(
						results.getLatencySamples(), (int) sla.getWindow())) {
					long curTime = (long) (tenantSLAs.get(j).getStart() + (i * 1e9 * (int) sla
							.getWindow()));
					if (j < tenantSLAs.size() - 1
							&& curTime < tenantSLAs.get(j + 1).getStart()) {
						break;
					}
					// calc performance result and penalty amount
					double resultValue = Metric.getAbsolutePerformance(
							sla.getMetric(), s, (int) sla.getWindow(),
							penalties, i);
					int penaltyAmount = sla.getPenaltyAmount(resultValue);
					// add penalty
					Penalty penalty = new Penalty(curTime, penaltyAmount);
					penalties = addPenalty(penalties, penalty);
					overallPenaltyAmount += penalty.getAmount();
					i++;
				}
			}
		}
	}

	public ArrayList<Penalty> addPenalty(ArrayList<Penalty> list,
			Penalty penalty) {
		// add new penalty
		if (list == null || list.isEmpty()) {
			list = new ArrayList<Penalty>();
			list.add(penalty);
		} else {
			// search for existing penalty for this time
			boolean penaltyTimeExist = false;
			for (Penalty existingPenalty : list) {
				if (penalty.getTime() == existingPenalty.getTime()) {
					existingPenalty.addAmount(penalty.getAmount());
					penaltyTimeExist = true;
				}
			}
			// add new penalty
			if (!penaltyTimeExist) {
				// add penalty by sorting the array
				if (list.get(list.size() - 1).getTime() < penalty.getTime()) {
					list.add(penalty);
				} else {
					for (int i = list.size() - 1; i >= 0; i--) {
						if (list.get(i).getTime() < penalty.getTime()) {
							list.add(i + 1, penalty);
						}
					}
				}
			}
		}
		return list;
	}

	private void orderSLAs() {
		ArrayList<WorkloadRunSLAs> orderedSLAs = new ArrayList<WorkloadRunSLAs>();
		while (tenantSLAs.size() > 0) {
			int minimum = 0;
			for (int i = 0; i < tenantSLAs.size(); i++) {
				if (tenantSLAs.get(i).getStart() < tenantSLAs.get(minimum)
						.getStart()) {
					minimum = i;
				}
			}
			orderedSLAs.add(tenantSLAs.get(minimum));
			tenantSLAs.remove(minimum);
		}
		tenantSLAs = orderedSLAs;
	}

	public ArrayList<Penalty> getPenalties() {
		return penalties;
	}

	public void setPenalties(ArrayList<Penalty> penalties) {
		this.penalties = penalties;
	}

	public int getOverallPenaltyAmount() {
		return overallPenaltyAmount;
	}

	public boolean isActivityInterval(long start, long end) {
		boolean result = false;
		for (Worker w : workers) {
			if (w.intersectsInterval(start, end))
				result = true;
		}
		return result;
	}
}
