package com.oltpbenchmark.multitenancy.sla;

/**
 * Managing of service level objectives
 * 
 * @author Andreas Goebel
 * 
 */
public class ServiceLevel {
	private double metricAmount;
	private int penaltyAmount;

	public double getMetricAmount() {
		return metricAmount;
	}

	public void setMetricAmount(double metricAmount) {
		this.metricAmount = metricAmount;
	}

	public int getPenaltyAmount() {
		return penaltyAmount;
	}

	public void setPenaltyAmount(int penaltyAmount) {
		this.penaltyAmount = penaltyAmount;
	}
}
