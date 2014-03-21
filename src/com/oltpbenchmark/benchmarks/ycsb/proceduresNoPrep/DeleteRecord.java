package com.oltpbenchmark.benchmarks.ycsb.proceduresNoPrep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import com.oltpbenchmark.api.Procedure;

public class DeleteRecord extends Procedure {
	public final String delete = "DELETE FROM USERTABLE where YCSB_KEY=%d";

	// FIXME: The value in ysqb is a byteiterator
	public void run(Connection conn, int keyname) throws SQLException {
		Statement deleteStmt = conn.createStatement();
		deleteStmt.executeUpdate(String.format(delete, keyname));
		deleteStmt.close();
	}

}
