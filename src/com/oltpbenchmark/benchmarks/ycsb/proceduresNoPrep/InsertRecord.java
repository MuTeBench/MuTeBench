package com.oltpbenchmark.benchmarks.ycsb.proceduresNoPrep;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.oltpbenchmark.api.Procedure;


public class InsertRecord extends Procedure {
	public final String insert = "INSERT INTO USERTABLE VALUES (%d,'%s','%s','%s','%s','%s','%s','%s','%s','%s','%s')";

	// FIXME: The value in ysqb is a byteiterator
	public void run(Connection conn, int keyname, Map<Integer, String> vals)
			throws SQLException {
		Statement insertStmt = conn.createStatement();
		insertStmt.executeUpdate(String.format(insert, keyname, vals.get(1)
				.replace("'", "#"), vals.get(2).replace("'", "#"), vals.get(3)
				.replace("'", "#"), vals.get(4).replace("'", "#"), vals.get(5)
				.replace("'", "#"), vals.get(6).replace("'", "#"), vals.get(7)
				.replace("'", "#"), vals.get(8).replace("'", "#"), vals.get(9)
				.replace("'", "#"), vals.get(10).replace("'", "#")));
		insertStmt.close();
	}
}