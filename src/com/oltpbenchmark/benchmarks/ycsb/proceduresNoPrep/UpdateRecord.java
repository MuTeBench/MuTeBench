package com.oltpbenchmark.benchmarks.ycsb.proceduresNoPrep;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.oltpbenchmark.api.Procedure;

public class UpdateRecord extends Procedure {

	public final String update = "UPDATE USERTABLE SET FIELD1='%s',FIELD2='%s',FIELD3='%s',FIELD4='%s',FIELD5='%s',"
			+ "FIELD6='%s',FIELD7='%s',FIELD8='%s',FIELD9='%s',FIELD10='%s' WHERE YCSB_KEY=%d";

	public void run(Connection conn, int keyname, Map<Integer, String> vals)
			throws SQLException {
		Statement updateStmt = conn.createStatement();
		updateStmt.executeUpdate(String.format(update,
				vals.get(1).replace("'", "#"), vals.get(2).replace("'", "#"),
				vals.get(3).replace("'", "#"), vals.get(4).replace("'", "#"),
				vals.get(5).replace("'", "#"), vals.get(6).replace("'", "#"),
				vals.get(7).replace("'", "#"), vals.get(8).replace("'", "#"),
				vals.get(9).replace("'", "#"), vals.get(10).replace("'", "#"),
				keyname));
		updateStmt.close();
	}
}