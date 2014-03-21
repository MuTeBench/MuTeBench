package com.oltpbenchmark.multitenancy.schedule;

import java.util.ArrayList;

import com.oltpbenchmark.multitenancy.BenchmarkSettings;

public class ScheduleEvents {

	private ArrayList<Long> startTimes;
	private ArrayList<BenchmarkSettings> benchmarkSettings;

	public ScheduleEvents() {
		startTimes = new ArrayList<Long>();
		benchmarkSettings = new ArrayList<BenchmarkSettings>();
	}

	public void addEvent(long time, BenchmarkSettings run) {
		// add to empty lists
		if (startTimes.isEmpty()) {
			startTimes.add(time);
			benchmarkSettings.add(run);
		}
		// order events for existing lists
		else {
			for (int i = startTimes.size() - 1; i >= 0; i--) {
				if (startTimes.get(i) < time) {
					startTimes.add(i + 1, time);
					benchmarkSettings.add(i + 1, run);
					break;
				}
				if (i == 0) {
					startTimes.add(0, time);
					benchmarkSettings.add(0, run);
					break;
				}
			}
		}
	}

	public int size() {
		return benchmarkSettings.size();
	}

	public void removeEvent(int index) {
		startTimes.remove(index);
		benchmarkSettings.remove(index);
	}

	public void removeEvent(long time) {
		int index = startTimes.indexOf(time);
		startTimes.remove(index);
		benchmarkSettings.remove(index);
	}

	public BenchmarkSettings getBenchmarkSettings(int index) {
		return benchmarkSettings.get(index);
	}

	public BenchmarkSettings getBenchmarkSettings(long startTime) {
		int index = startTimes.indexOf(startTime);
		return benchmarkSettings.get(index);
	}

	public long getTime(int index) {
		return startTimes.get(index);
	}
}
