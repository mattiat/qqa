package qqa.db.dataset;

import java.sql.SQLException;

import qqa.db.MysqlConnect;
import qqa.ml.Preprocessor;

public class QuestionExtractor extends Extractor {

	public QuestionExtractor(String fileName) {
		super(fileName);
	}

	@Override
	public boolean store(String[] parts, MysqlConnect database) 
	throws SQLException {
		// check the datum contains the correct number of pieces of information
		// if so, insert and return 1: will update insert counter in 
		// Extractor.extract()
		if (parts.length!=14 
				// only store opinion and definition questions
//				|| !(OpinionDefinitionQuestions.isOpinionQuestion(parts[1]) ||
//				OpinionDefinitionQuestions.isDefinitionQuestion(parts[1]))
				// only store complex questions
//				|| !ComplicatedQuestions.isComplicatedQuestion(parts[1])
				) 
			return false;
		else {
			// preprocessing
			String subject = Preprocessor.removeStopwords(parts[1]);
			String content = Preprocessor.removeStopwords(parts[2]);
			// query to insert question
			String question = "INSERT Questions " +
			"VALUES(" + 
			"\'" + parts[0] + "\'," +
			"\"" + subject + "\"," +
			"\"" + parts[1] + "\"," +
			"\"" + content + "\"," +
			"\"" + parts[2] + "\"," +
			"\'" + parts[3] + "\'," +
			"\"" + parts[4] + "\"," +
			"\'" + parts[5] + "\'," +
			"\"" + parts[6] + "\"," +
			"\'" + parts[7] + "\'," +
			parts[8] + "," +
			"\"" + parts[9] + "\"," +
			parts[10] + "," +
			parts[11] + "," +
			parts[12] + "," +
			"\'" + parts[13] + "\'" +
			")";
			database.execute(question);
			storedQuesId.put(parts[0], parts[0]);
			return true;
		}
	}
}
