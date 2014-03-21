/*******************************************************************************
 * MuTeBench
 * 
 *  Creator:    Andreas Goebel <andreas.goebel@uni-jena.de>
 * 
 *  This library is free software; you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Foundation;
 *  either version 3.0 of the License, or (at your option) any later version.
 * 
 *  This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 ******************************************************************************/
package com.oltpbenchmark.multitenancy.schedule;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.apache.log4j.Logger;

import com.oltpbenchmark.multitenancy.BenchmarkSettings;

public class Schedule {

	private static final Logger LOG = Logger.getLogger(Schedule.class);
	private HashMap<Integer, ScheduleEvents> tenantEvents;
	private ArrayList<Integer> tenantList;

	private long duration = -1; // default: infinite duration (-1)
	private String scenarioFile;

	public Schedule(String duration, String scenarioFile) {
		this.duration = parseTimeFormat(duration);
		this.scenarioFile = scenarioFile;

		tenantList = new ArrayList<Integer>();
		tenantEvents = parseEvents();
	}

	private HashMap<Integer, ScheduleEvents> parseEvents() {
		HashMap<Integer, ScheduleEvents> newMap = new HashMap<Integer, ScheduleEvents>();
		try {
			// read config file
			XMLConfiguration xmlConfig = new XMLConfiguration(scenarioFile);
			xmlConfig.setExpressionEngine(new XPathExpressionEngine());

			// iterate over all defined events and parse configuration
			int size = xmlConfig.configurationsAt(
					ScenarioConfigElements.EVENTS_KEY + "/"
							+ ScenarioConfigElements.EVENT_KEY).size();
			for (int i = 1; i < size + 1; i++) {
				SubnodeConfiguration event = xmlConfig
						.configurationAt(ScenarioConfigElements.EVENTS_KEY
								+ "/" + ScenarioConfigElements.EVENT_KEY + "["
								+ i + "]");
				// create settings for a benchmark run
				BenchmarkSettings benchSettings = new BenchmarkSettings(event);

				// get schedule times
				long eventStart = 0, eventStop = -1, eventRepeat = -1;
				if (event.containsKey(ScenarioConfigElements.EVENT_START_KEY))
					eventStart = parseTimeFormat(event
							.getString(ScenarioConfigElements.EVENT_START_KEY));
				else
					LOG.debug("There is no start time defined for an event, it will be started immediately!");
				if (event.containsKey(ScenarioConfigElements.EVENT_REPEAT_KEY))
					eventRepeat = parseTimeFormat(event
							.getString(ScenarioConfigElements.EVENT_REPEAT_KEY));
				if (event.containsKey(ScenarioConfigElements.EVENT_STOP_KEY))
					eventStop = parseTimeFormat(event
							.getString(ScenarioConfigElements.EVENT_STOP_KEY));

				// validate schedule times
				if (eventRepeat > -1 && eventStop == -1 && duration == -1) {
					LOG.fatal("Infinitely event execution was defined: Repeated event without repeating end and scenario end");
					System.exit(-1);
				}
				if (eventRepeat == 0) {
					LOG.fatal("An Event cannot be repeated simoultaneously (avoid infinite loop)!");
					System.exit(-1);
				}
				if (eventStart > eventStop && eventStop != -1) {
					LOG.fatal("Event cannot be stopped until starting it!");
					System.exit(-1);
				}

				// get tenant IDs
				int firstTenantID = 1, tenantsPerExecution = 1, tenantIdIncement = 1;
				if (event
						.containsKey(ScenarioConfigElements.FIRST_TENANT_ID_KEY))
					firstTenantID = (event
							.getInt(ScenarioConfigElements.FIRST_TENANT_ID_KEY));
				if (event
						.containsKey(ScenarioConfigElements.TENANTS_PER_EXECUTION_KEY))
					tenantsPerExecution = (event
							.getInt(ScenarioConfigElements.TENANTS_PER_EXECUTION_KEY));
				if (event
						.containsKey(ScenarioConfigElements.TENANT_ID_INCREMENT_KEY))
					tenantIdIncement = (event
							.getInt(ScenarioConfigElements.TENANT_ID_INCREMENT_KEY));

				// validate tenant IDs
				if (tenantsPerExecution < 0) {
					LOG.fatal("Value '" + tenantsPerExecution
							+ "' for tenants per executions is not valid!");
					System.exit(-1);
				}

				// execution times and assign to tenants
				if (duration != -1 && duration < eventStop)
					eventStop = duration;
				int exec = 0;
				long execTime = eventStart;
				while ((execTime <= eventStop)
						|| (eventStop == -1 && exec == 0)) {
					// iterate over all tenants in this execution
					for (int j = 0; j < tenantsPerExecution; j++) {
						int currentTenantID = firstTenantID
								+ (exec * tenantsPerExecution * tenantIdIncement)
								+ (j * tenantIdIncement);
						if (!newMap.containsKey(currentTenantID)) {
							ScheduleEvents tenEvents = new ScheduleEvents();
							tenEvents.addEvent(execTime, benchSettings);
							newMap.put(currentTenantID, tenEvents);
							tenantList.add(currentTenantID);
						} else
							newMap.get(currentTenantID).addEvent(execTime,
									benchSettings);
					}
					if (eventRepeat == -1)
						break;
					execTime += eventRepeat;
					exec++;
				}
			}
		} catch (ConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newMap;
	}

	/**
	 * adds a given time span with format hh:mm:ss and a given time stamp
	 * 
	 * @param duration
	 *            (format: hh:mm:ss)
	 * @param start
	 * @return start+duration
	 */
	public long parseTimeFormat(String duration) {
		if (duration == null)
			return -1;
		if (!duration.matches("\\d\\d[:]\\d\\d[:]\\d\\d")) {
			LOG.fatal(duration
					+ " in scenario definition file doesn't have a valid format (hh:mm:ss)");
			System.exit(-1);
		}
		String[] dur = duration.split(":");
		return (Integer.valueOf(dur[2])) + (Integer.valueOf(dur[1]) * 60)
				+ (Integer.valueOf(dur[0]) * 60 * 60);
	}

	public HashMap<Integer, ScheduleEvents> getTenantEvents() {
		return tenantEvents;
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration(long duration) {
		this.duration = duration;
	}

	public String getScenarioFile() {
		return scenarioFile;
	}

	public ArrayList<Integer> getTenantList() {
		return tenantList;
	}
}
