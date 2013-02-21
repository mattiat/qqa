package qqa.db.dataset;

import java.sql.SQLException;
import qqa.db.*;

public class CategoryExtractor extends Extractor {

	public CategoryExtractor(String fileName) {
		super(fileName);
	}

	@Override
	public boolean store(String[] parts, MysqlConnect database) throws SQLException {
		// check the datum contains the correct number of pieces of information
		// if so, insert and return 1: will update insert counter in Extractor.extract()
		if (parts.length!=2) 
			return false;
		else {
			String category = "INSERT Categories " +
			"VALUES(" + 
			"\'" + parts[0] + "\'," +
			"\"" + parts[1] + "\"" +
			")";
			database.execute(category);
			return true;
		}
	}
}
