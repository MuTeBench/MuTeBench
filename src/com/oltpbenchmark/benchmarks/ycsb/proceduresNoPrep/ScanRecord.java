package com.oltpbenchmark.benchmarks.ycsb.proceduresNoPrep;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oltpbenchmark.api.Procedure;

public class ScanRecord extends Procedure {
	public final String scan = "SELECT * FROM USERTABLE WHERE YCSB_KEY>%d AND YCSB_KEY<%d";

	// FIXME: The value in ysqb is a byteiterator
	public void run(Connection conn, int start, int count,
			List<Map<Integer, String>> results) throws SQLException {
		Statement selectStmt = conn.createStatement();
		ResultSet r = selectStmt.executeQuery(String.format(scan, start, start
				+ count));
		while (r.next()) {
			HashMap<Integer, String> m = new HashMap<Integer, String>();
			for (int i = 1; i < 11; i++)
				m.put(i, r.getString(i).replace("'", "#"));
			results.add(m);
		}
		r.close();
		selectStmt.close();
	}
}