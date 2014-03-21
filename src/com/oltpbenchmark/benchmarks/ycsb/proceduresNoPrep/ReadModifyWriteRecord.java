package com.oltpbenchmark.benchmarks.ycsb.proceduresNoPrep;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import com.oltpbenchmark.api.Procedure;

public class ReadModifyWriteRecord extends Procedure {
	public final String select = "Select * from USERTABLE where YCSB_KEY=%d for update";
	public final String update = "UPDATE USERTABLE SET FIELD1='%s',FIELD2='%s',FIELD3='%s',FIELD4='%s',FIELD5='%s',"
			+ "FIELD6='%s',FIELD7='%s',FIELD8='%s',FIELD9='%s',FIELD10='%s' WHERE YCSB_KEY=%d";

	// FIXME: The value in ysqb is a byteiterator
	public void run(Connection conn, int keyname, String fields[],
			Map<Integer, String> results) throws SQLException {
		Statement selectStmt = conn.createStatement();
		ResultSet r = selectStmt.executeQuery(String.format(select, keyname));
		while (r.next()) {
			for (int i = 1; i < 11; i++)
				results.put(i, r.getString(i).replace("'", "#"));
		}
		r.close();
		
		Statement updateStmt = conn.createStatement();
		updateStmt.executeUpdate(String.format(update,
				fields[0].replace("'", "#"), fields[1].replace("'", "#"),
				fields[2].replace("'", "#"), fields[3].replace("'", "#"),
				fields[4].replace("'", "#"), fields[5].replace("'", "#"),
				fields[6].replace("'", "#"), fields[7].replace("'", "#"),
				fields[8].replace("'", "#"), fields[9].replace("'", "#"),
				keyname));
		selectStmt.close();
		updateStmt.close();
	}
}
