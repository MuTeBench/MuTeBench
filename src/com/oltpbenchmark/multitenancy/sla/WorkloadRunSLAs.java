package com.oltpbenchmark.multitenancy.sla;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.collections15.map.ListOrderedMap;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;

import com.oltpbenchmark.api.TransactionTypes;
import com.oltpbenchmark.multitenancy.gui.Metric;
import com.oltpbenchmark.util.StringUtil;

/**
 * Organizing of Service Level Agreements
 * 
 * @author Andreas Goebel
 * 
 */
public class WorkloadRunSLAs {

	private static final Logger LOG = Logger.getLogger(WorkloadRunSLAs.class);

	private ArrayList<ServiceLevelAgreement> slas;
	private TransactionTypes transTypes;
	private String benchmarkName;
	private long start;

	/**
	 * Constructor with benchmark transactions and SLO configuration file
	 * 
	 * @param transTypes
	 * @param configFile
	 */
	public WorkloadRunSLAs(TransactionTypes transTypes,
			XMLConfiguration xmlConfig) {
		slas = new ArrayList<ServiceLevelAgreement>();
		this.transTypes = transTypes;
		this.start = System.nanoTime();
		parseSLAs(xmlConfig);
	}

	/**
	 * Method for reading Service Level Agreements
	 * 
	 * @param configFile
	 */
	private void parseSLAs(XMLConfiguration xmlConfig) {

		// read benchmark name
		try {
			benchmarkName = xmlConfig
					.getString(SlaConfigElements.BENCHMARK_KEYWORD);
		} catch (Exception e) {
			LOG.error("Cannot find node '"
					+ SlaConfigElements.BENCHMARK_KEYWORD
					+ "' in your sla definition file.");
			e.printStackTrace();
		}

		// iterate over slas
		for (int slaNr = 0; slaNr <= xmlConfig
				.getMaxIndex(SlaConfigElements.SLA_KEYWORD); slaNr++) {
			ServiceLevelAgreement sla = new ServiceLevelAgreement(transTypes);

			// parse sla name
			sla.setSlaName(xmlConfig.getProperty(
					SlaConfigElements.SLA_KEYWORD + "(" + slaNr + ")."
							+ SlaConfigElements.SLA_NAME_KEYWORD).toString());

			// parse sla target
			try {
				sla.setTarget(xmlConfig.getProperty(
						SlaConfigElements.SLA_KEYWORD + "(" + slaNr + ")."
								+ SlaConfigElements.TARGET_KEYWORD).toString());
			} catch (Exception e) {
				sla.setTarget(null);
			}

			// parse and validate metric
			String metricInput = xmlConfig.getProperty(
					SlaConfigElements.SLA_KEYWORD + "(" + slaNr + ")."
							+ SlaConfigElements.METRIC_KEYWORD).toString();
			try {
				sla.setMetric(Metric.valueOf(metricInput));
				if (sla.getMetric() == Metric.SLA_PENALTY_AMOUNT)
					throw new Exception();
			} catch (Exception e) {
				LOG.error("Invalid metric '" + metricInput
						+ "' in your sla definition file.");
			}

			// parse and validate window
			sla.setWindow(Long.valueOf(xmlConfig.getProperty(
					SlaConfigElements.WINDOW_KEYWORD).toString()));
			try {
				if (sla.getWindow() <= 0)
					throw new IllegalArgumentException();
			} catch (IllegalArgumentException e) {
				LOG.error("Invalid window '" + sla.getWindow()
						+ "' in your sla definition file.");
			}

			// parse transactions
			try {
				if (xmlConfig
						.getProperty(
								SlaConfigElements.SLA_KEYWORD
										+ "("
										+ slaNr
										+ ")."
										+ SlaConfigElements.TRANSACTIONS_KEYWORD)
						.toString()
						.equalsIgnoreCase(SlaConfigElements.ALL_TA_KEYWORD)) {
					sla.setTransactionsAll(true);
				}
			} catch (NullPointerException e) {
				try {
					for (int taNr = 0; taNr <= xmlConfig
							.getMaxIndex(SlaConfigElements.SLA_KEYWORD + "("
									+ slaNr + ")."
									+ SlaConfigElements.TRANSACTIONS_KEYWORD
									+ "." + SlaConfigElements.TA_KEYWORD); taNr++) {
						String taName = xmlConfig
								.getProperty(
										SlaConfigElements.SLA_KEYWORD
												+ "("
												+ slaNr
												+ ")."
												+ SlaConfigElements.TRANSACTIONS_KEYWORD
												+ "."
												+ SlaConfigElements.TA_KEYWORD
												+ "(" + taNr + ")").toString();
						for (int i = 0; i < sla.getTransTypes().size(); i++) {
							if (sla.getTransTypes().getType(i).getName()
									.equalsIgnoreCase(taName)) {
								sla.addTransaction(taName);
								break;
							}
							if (i == sla.getTransTypes().size() - 1) {
								LOG.error("Transaction '"
										+ taName
										+ "' (used in sla definition file) is not defined in benchmark "
										+ benchmarkName);
							}
						}
					}
					if (sla.getTransactions().isEmpty())
						LOG.error("Transactions have to be defined in sla definition file.");
				} catch (Exception e2) {
					e2.printStackTrace();
				}
			}

			// parse service levels
			for (int slNr = 0; slNr <= xmlConfig
					.getMaxIndex(SlaConfigElements.SLA_KEYWORD + "(" + slaNr
							+ ")." + SlaConfigElements.SERVICE_LEVELS_KEYWORD
							+ "." + SlaConfigElements.SL_KEYWORD); slNr++) {
				ServiceLevel sl = new ServiceLevel();
				String slPath = SlaConfigElements.SLA_KEYWORD + "(" + slaNr
						+ ")." + SlaConfigElements.SERVICE_LEVELS_KEYWORD + "."
						+ SlaConfigElements.SL_KEYWORD + "(" + slNr + ")";

				// read metric amount
				sl.setMetricAmount(Long.valueOf(xmlConfig.getProperty(
						slPath + "." + SlaConfigElements.METRIC_AMOUNT_KEYWORD)
						.toString()));

				// read penalty amount
				sl.setPenaltyAmount(Integer
						.valueOf(xmlConfig
								.getProperty(
										slPath
												+ "."
												+ SlaConfigElements.PENALTY_AMOUNT_KEYWORD)
								.toString()));
				sla.addServiceLevel(sl);
			}
			if (sla.getServiceLevels().isEmpty()) {
				LOG.error("Service Levels have to be defined in sla definition file.");
			}
			slas.add(sla);
		}
	}

	public ArrayList<ServiceLevelAgreement> getSLAs() {
		return slas;
	}

	public long getStart() {
		return start;
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

	public boolean equals(WorkloadRunSLAs obj) {
		boolean equal = true;
		if (!this.benchmarkName.equalsIgnoreCase(obj.benchmarkName))
			equal = false;
		if (!this.transTypes.equals(obj.transTypes))
			equal = false;
		if (!this.slas.equals(obj.slas))
			equal = false;
		return equal;
	}
}
