package qqa.db.dataset;

import java.sql.SQLException;
import qqa.db.*;
import qqa.ml.Preprocessor;

public class AnswerExtractor extends Extractor {

	public AnswerExtractor(String fileName) {
		super(fileName);
	}

	@Override
	public boolean store(String[] parts, MysqlConnect database) 
	throws SQLException {
		// check the datum contains the correct number of pieces of information
		// and the corresponding question was selected. If so, insert and return
		// 1: will update insert counter in Extractor.extract()
		if (parts.length!=7 || !storedQuesId.containsKey(parts[0])) 
			return false;
		else {
			// preprocessing
			String content = Preprocessor.removeStopwords(parts[1]);
			// query to insert answer
			String answer = "INSERT Answers " +
			"(ques_id, content, content_unprocessed, isbest, user_id, " +
			"datetime, thumb_up, thumb_down) " +
			"VALUES(" + 
			"\'" + parts[0] + "\'," +
			"\"" + content + "\"," +
			"\"" + parts[1] + "\"," +
			parts[2] + "," +
			"\'" + parts[3] + "\'," +
			"\'" + parts[4] + "\'," +
			parts[5] + "," +
			parts[6] +
			")";
			database.execute(answer);
			return true;
		}
	}
}
