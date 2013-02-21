package qqa.db.dataset;

import java.sql.SQLException;
import qqa.db.*;

public class UserExtractor extends Extractor {

	public UserExtractor(String fileName) {
		super(fileName);
	}

	@Override
	public boolean store(String[] parts, MysqlConnect database) throws SQLException {
		// check the datum contains the correct number of pieces of information
		// if so, insert and return 1: will update insert counter in Extractor.extract()
		if (parts.length!=8) 
			return false;
		else {
			String user = "INSERT Users " +
			"VALUES(" + 
			"\'" + parts[0] + "\'," +
			parts[1] + "," +
			parts[2] + "," +
			parts[3] + "," +
			parts[4] + "," +
			parts[5] + "," +
			parts[6] + "," +
			"\'" + parts[7] + "\'" +
			")";
			database.execute(user);
			return true;
		}
	}
}
