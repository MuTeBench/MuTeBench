package com.oltpbenchmark.multitenancy.gui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oltpbenchmark.DistributionStatistics;
import com.oltpbenchmark.multitenancy.ResultCollector;
import com.oltpbenchmark.multitenancy.sla.Penalty;

public enum Metric {
	THROUGHPUT("Throughput (transactions/sec)", // label for absolute axis
			"Relative Throughput (%)", 			// label for relative axis
			"METRIC THROUGHPUT SCOPE AVERAGE"), // Generated part of SQL Query
												// "CREATE SLA"
	LATENCY_AVERAGE("Average Latency (ms)", "Relative Average Latency (%)",
			"METRIC LATENCY SCOPE AVERAGE"), LATENCY_MINIMUM(
			"Minimum Latency (ms)", "Relative Minimum Latency (%)",
			"METRIC LATENCY SCOPE MINIMUM"), LATENCY_25TH_PERCENTILE(
			"25th Latency Percentile (ms)",
			"Relative 25th Latency Percentile (%)",
			"METRIC LATENCY SCOPE RATE 0.25"), LATENCY_MEDIAN(
			"Median Latency (ms)", "Relative Median Latency (%)",
			"METRIC LATENCY SCOPE RATE 0.5"), LATENCY_75TH_PERCENTILE(
			"75th Latency Percentile (ms)",
			"Relative 75th Latency Percentile (%)",
			"METRIC LATENCY SCOPE RATE 0.75"), LATENCY_90TH_PERCENTILE(
			"90th Latency Percentile (ms)",
			"Relative 90th Latency Percentile (%)",
			"METRIC LATENCY SCOPE RATE 0.9"), LATENCY_95TH_PERCENTILE(
			"95th Latency Percentile (ms)",
			"Relative 95th Latency Percentile (%)",
			"METRIC LATENCY SCOPE RATE 0.95"), LATENCY_99TH_PERCENTILE(
			"99th Latency Percentile (ms)",
			"Relative 99th Latency Percentile (%)",
			"METRIC LATENCY SCOPE RATE 0.99"), LATENCY_MAXIMUM(
			"Maximum Latency (ms)", "Relative Maximum Latency (%)",
			"METRIC LATENCY SCOPE MAXIMUM"), SLA_PENALTY_AMOUNT(
			"SLA Penalty Amount (US$)", "Relative SLA Penalty Amount (%)", null);

	private final List<String> values;
	final static double MILLISECONDS_FACTOR = 1e3;
	
	Metric(String... values) {
		this.values = Arrays.asList(values);
	}

	public static Metric getMetric(String value) {
		for (Metric metric : Metric.values()) {
			for (String metricValue : metric.getValues()) {
				if (metricValue.equalsIgnoreCase(value))
					return metric;
			}
		}
		return null;
	}

	public List<String> getValues() {
		return values;
	}

	public static double getAbsolutePerformance(Metric metric,
			DistributionStatistics s, int windowSize,
			ArrayList<Penalty> penalties, int i) {
		switch (metric) {
		case THROUGHPUT:
			return (double) s.getCount() / windowSize;
		case LATENCY_AVERAGE:
			return s.getAverage() / MILLISECONDS_FACTOR;
		case LATENCY_25TH_PERCENTILE:
			return s.get25thPercentile() / MILLISECONDS_FACTOR;
		case LATENCY_75TH_PERCENTILE:
			return s.get75thPercentile() / MILLISECONDS_FACTOR;
		case LATENCY_90TH_PERCENTILE:
			return s.get90thPercentile() / MILLISECONDS_FACTOR;
		case LATENCY_95TH_PERCENTILE:
			return s.get95thPercentile() / MILLISECONDS_FACTOR;
		case LATENCY_99TH_PERCENTILE:
			return s.get99thPercentile() / MILLISECONDS_FACTOR;
		case LATENCY_MAXIMUM:
			return s.getMaximum() / MILLISECONDS_FACTOR;
		case LATENCY_MEDIAN:
			return s.getMedian() / MILLISECONDS_FACTOR;
		case LATENCY_MINIMUM:
			return s.getMinimum() / MILLISECONDS_FACTOR;
		case SLA_PENALTY_AMOUNT: {
			// search for relevant penalties and print their added penalty
			// amounts
			boolean penaltyFound = false;
			int penaltyAmount = 0;
			for (Penalty penalty : penalties) {
				if (ResultCollector.startMillis + (i - 1)
						* (windowSize * MILLISECONDS_FACTOR) < penalty.getTime()
						&& ResultCollector.startMillis + i
								* (windowSize * MILLISECONDS_FACTOR) >= penalty
									.getTime()) {
					penaltyFound = true;
					penaltyAmount += penalty.getAmount();
				}
			}
			if (!penaltyFound)
				return 0;
			else
				return penaltyAmount;
		}
		}
		return -1;
	}

	public static double getRelativePerformance(Metric metric,
			DistributionStatistics s, int windowSize, String[] base,
			ArrayList<Penalty> penalties, int i) {
		switch (metric) {
		case THROUGHPUT:
			return (double) s.getCount() / windowSize
					/ Float.parseFloat(base[1]);
		case LATENCY_AVERAGE:
			return 1 / ((s.getAverage() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[2]));
		case LATENCY_MINIMUM:
			return 1 / ((s.getMinimum() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[3]));
		case LATENCY_25TH_PERCENTILE:
			return 1 / ((s.get25thPercentile() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[4]));
		case LATENCY_MEDIAN:
			return 1 / ((s.getMedian() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[5]));
		case LATENCY_75TH_PERCENTILE:
			return 1 / ((s.get75thPercentile() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[6]));
		case LATENCY_90TH_PERCENTILE:
			return 1 / ((s.get90thPercentile() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[7]));
		case LATENCY_95TH_PERCENTILE:
			return 1 / ((s.get95thPercentile() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[8]));
		case LATENCY_99TH_PERCENTILE:
			return 1 / ((s.get99thPercentile() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[9]));
		case LATENCY_MAXIMUM:
			return 1 / ((s.getMaximum() / MILLISECONDS_FACTOR) / Float
					.parseFloat(base[10]));
		case SLA_PENALTY_AMOUNT: {
			// search for relevant penalties and print their added penalty
			// amounts
			boolean penaltyFound = false;
			int penaltyAmount = 0;
			for (Penalty penalty : penalties) {
				if (ResultCollector.startMillis + (i - 1)
						* (windowSize * MILLISECONDS_FACTOR) < penalty.getTime()
						&& ResultCollector.startMillis + i
								* (windowSize * MILLISECONDS_FACTOR) >= penalty
									.getTime()) {
					penaltyFound = true;
					penaltyAmount += penalty.getAmount();
				}
			}
			if (!penaltyFound || base.length < 12)
				return Double.NaN;
			else
				return 1 / (penaltyAmount / Integer.parseInt(base[11]));
		}
		}
		return -1;
	}

	public static boolean metricGoalIsMaxValue(Metric metric) {
		if (metric.equals(THROUGHPUT))
			return true;
		else
			return false;
	}
}