package com.oltpbenchmark.multitenancy.sla;

public class Penalty {
	private long time;
	private int amount;

	public Penalty(long time, int amount) {
		super();
		this.time = time;
		this.amount = amount;
	}

	public long getTime() {
		return time;
	}

	public int getAmount() {
		return amount;
	}

	public void addAmount(int newAmount) {
		amount += newAmount;
	}
}
