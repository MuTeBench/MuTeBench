package com.oltpbenchmark.benchmarks.ycsb.proceduresNoPrep;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.oltpbenchmark.api.Procedure;

public class ReadRecord extends Procedure {
	public final String select = "Select * from USERTABLE where YCSB_KEY=%d";

	// FIXME: The value in ysqb is a byteiterator
	public void run(Connection conn, int keyname, Map<Integer, String> results)
			throws SQLException {
		Statement selectStmt = conn.createStatement();
		ResultSet r = selectStmt.executeQuery(String.format(select, keyname));
		while (r.next()) {
			for (int i = 1; i < 11; i++)
				results.put(i, r.getString(i).replace("'", "#"));
		}
		r.close();
		selectStmt.close();
	}

}
