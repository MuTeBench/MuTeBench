package com.oltpbenchmark.multitenancy.sla;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.collections15.map.ListOrderedMap;

import com.oltpbenchmark.api.TransactionTypes;
import com.oltpbenchmark.multitenancy.gui.Metric;
import com.oltpbenchmark.util.StringUtil;

/**
 * Manages a Service Level Agreement
 * 
 * @author Andreas Goebel
 * 
 */
public class ServiceLevelAgreement {

	private TransactionTypes transTypes;
	private String slaName;
	private ArrayList<String> transactions;
	private boolean transactionsAll;
	private Metric metric;
	private String target;
	private long window;
	private ArrayList<ServiceLevel> serviceLevels;

	public ServiceLevelAgreement(TransactionTypes transTypes) {
		this.transTypes = transTypes;
		transactions = new ArrayList<String>();
		serviceLevels = new ArrayList<ServiceLevel>();
	}

	public TransactionTypes getTransTypes() {
		return transTypes;
	}

	public void addTransaction(String tt) {
		transactions.add(tt);
	}

	public void addServiceLevel(ServiceLevel sl) {
		serviceLevels.add(sl);
	}

	public void TransactionType(String tt) {
		transactions.add(tt);
	}

	public String getSlaName() {
		return slaName;
	}

	public void setSlaName(String slaName) {
		this.slaName = slaName;
	}

	public ArrayList<String> getTransactions() {
		return transactions;
	}

	public boolean isTransactionsAll() {
		return transactionsAll;
	}

	public void setTransactionsAll(boolean transactionsAll) {
		this.transactionsAll = transactionsAll;
	}

	public Metric getMetric() {
		return metric;
	}

	public void setMetric(Metric metric) {
		this.metric = metric;
	}

	public long getWindow() {
		return window;
	}

	public void setWindow(long window) {
		this.window = window;
	}

	public ArrayList<ServiceLevel> getServiceLevels() {
		return serviceLevels;
	}

	public void setServiceLevels(ArrayList<ServiceLevel> serviceLevels) {
		this.serviceLevels = serviceLevels;
	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	/**
	 * calculates maximum penalty amount for given service levels
	 * 
	 * @param metricValue
	 *            performance value for the metric
	 * @return penalty amount
	 */
	public int getPenaltyAmount(double metricValue) {
		int currentPenalty = 0;
		// iterate over all service levels
		for (int i = 0; i < serviceLevels.size(); i++) {
			// distinguish metric type: Goal is high throughput, but low
			// latencies
			if ((Metric.metricGoalIsMaxValue(metric) && metricValue < serviceLevels
					.get(i).getMetricAmount())
					|| (!Metric.metricGoalIsMaxValue(metric) && metricValue > serviceLevels
							.get(i).getMetricAmount())) {
				if (serviceLevels.get(i).getPenaltyAmount() > currentPenalty) {
					currentPenalty = serviceLevels.get(i).getPenaltyAmount();
				}
			}
		}
		return currentPenalty;
	}

	@Override
	public String toString() {
		Class<?> confClass = this.getClass();
		Map<String, Object> m = new ListOrderedMap<String, Object>();
		for (Field f : confClass.getDeclaredFields()) {
			Object obj = null;
			try {
				obj = f.get(this);
			} catch (IllegalAccessException ex) {
				throw new RuntimeException(ex);
			}
			m.put(f.getName().toUpperCase(), obj);
		} // FOR
		return StringUtil.formatMaps(m);
	}
}
