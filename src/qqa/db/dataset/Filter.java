package qqa.db.dataset;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;

import qqa.db.MysqlConnect;

/**
 * Filters a dataset in order to keep only those question and answer pairs that
 * are most suited for summarization
 * @author Mattia Tomasoni
 *
 */
public class Filter{
	
	/**
	 * reference to the dataset to be filtered
	 */
	public Interface interf = null;
	
	/**
	 * list of ques_id remained after filtering
	 */
	public Vector<String> filteredQuestions = null;

	/**
	 * constructor initializes dataset reference and threshold and filters the
	 * dataset
	 * @param interfacePar
	 * @param thresholdPar
	 * @throws SQLException
	 */
	public Filter(Interface interfacePar)
	throws SQLException {
		interf = interfacePar;
		int numberOfAnswers_low = 2;
		// longest answer
		int Lthreshold_high = 400;
		// sum of answers length
		int TLthreshold_low = 100;
		int TLthreshold_high = 1000;
		// normalized sum of answers length
		int NLthreshold_low = 50;
		int NLthreshold_high = 300;
		filteredQuestions = new Vector<String>();
		interf.questions.beforeFirst();
		// for each question
		while (interf.questions.next()) {
			String ques_id = interf.questions.getString("ques_id");
			// sum of length of all answers to this question
			int longhestAnswer = 0;
			int totalLength = 0;
			int answersCounter = 0;
			interf.answers.beforeFirst();
			while (interf.answers.next()) {
				// for each corresponding question
				if(interf.answers.getString("ques_id").
						equalsIgnoreCase(ques_id)){
					// calculate answer length (using the processed data)
					String content = interf.answers.getString("content");
					int length = content.split(" ").length;
					// update totalLength
					totalLength += length; 
					// update length of longest answer if it is the case
					if(length > longhestAnswer) longhestAnswer = length;
					// update answer counter for this question
					answersCounter ++;
				}
			}// all answers have been processed
			// select questions with totalLength above threshold
			if(answersCounter >= numberOfAnswers_low){
				int normLength = (int) (totalLength/answersCounter);
				if (longhestAnswer < Lthreshold_high &&
						totalLength > TLthreshold_low &&
						totalLength < TLthreshold_high &&
						normLength > NLthreshold_low &&
						normLength < NLthreshold_high)
					filteredQuestions.add(ques_id);
			}
		}
	}
	
	/**
	 * applies filter to the database: filling the FilteredAnswers and 
	 * FilteredQuestions tables with the questions and answers corresponding to
	 * the ids in filteredQuestions
	 * @throws SQLException 
	 */
	public void apply(MysqlConnect database) throws SQLException{
		// creating FilteredAnswers table in the database with the same 
		// structure of the Answers table
		String query = "DROP TABLE IF EXISTS FilteredAnswers";
		database.execute(query);
		query =	"CREATE TABLE FilteredAnswers LIKE Answers";
		database.execute(query);
		// creating FilteredQuestions table
		query = "DROP TABLE IF EXISTS FilteredQuestions";
		database.execute(query);
		query = "CREATE TABLE FilteredQuestions LIKE Questions";
		database.execute(query);
		// creating QualityFeatures table 
		query = "DROP TABLE IF EXISTS QualityFeatures";
		database.execute(query);
		query = "CREATE TABLE QualityFeatures LIKE Features";
		database.execute(query);
		// filling the FilteredAnswers and FilteredQuestions tables with the 
		// questions and answers corresponding to the ids in filteredQuestions
		for (Iterator<String> iterator = filteredQuestions.iterator(); 
		iterator.hasNext();) {
			// getting question_id
			String ques_id = (String) iterator.next();
			// add to FilteredAnswers
			query = "INSERT FilteredAnswers SELECT * FROM Answers WHERE " +
			"ques_id = \'" + ques_id + "\'";
			database.execute(query);
			// add to FilteredQuestions
			query = "INSERT FilteredQuestions SELECT * FROM Questions WHERE " +
			"ques_id = \'" + ques_id + "\'";
			database.execute(query);
			// add to QualityFeatures
			query = "INSERT QualityFeatures SELECT * FROM Features WHERE " +
			"ques_id = \'" + ques_id + "\'";
			database.execute(query);
		}
		// add to the QualityFeatures table with 10000 records from 
		// Features table that will be used for quality training
		query = "INSERT IGNORE QualityFeatures SELECT * FROM Features LIMIT 0,10000";
		database.execute(query);
	}
	
}
