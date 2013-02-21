package qqa;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import qqa.db.MysqlConnect;
import qqa.db.dataset.Filter;
import qqa.db.dataset.Interface;

/**
 * Filters a dataset in order to keep only those question and answer 
 * pairs that are most suited for summarization
 * 
 * @author Mattia Tomasoni
 */
public class C_Filter_Dataset {
	/** 
	 *  connection to database
	 */
	private static MysqlConnect database = null;
	
	public static void main(String[] args) throws InstantiationException, 
	IllegalAccessException, ClassNotFoundException, SQLException, IOException{
		// keep track of execution time
		long start = System.currentTimeMillis();
		
		// connecting to database
		database = new MysqlConnect();
		
		// retrieve all answers
		String query = "SELECT answer_id, ques_id, content, content_unprocessed," 
			+ " user_id, isbest FROM Answers ORDER BY ques_id";
		ResultSet answers = database.ask(query);
		// retrieve all users
		query = "SELECT user_id, total_answers, best_answers, " +
		"total_points FROM Users ORDER BY user_id";
		ResultSet users = database.ask(query);
		// retrieve all questions
		query = "SELECT ques_id, subject, content, content_unprocessed, " +
		"subject_unprocessed FROM Questions ORDER BY ques_id";
		ResultSet questions = database.ask(query);
		
		// construct interface to the dataset in the database
		Interface datasetInterface = new Interface(answers, questions, users);
		
		///////////////////////////////////////////////////////////////////////
		// Sentence selection Preprocessing: filter Dataset
		///////////////////////////////////////////////////////////////////////
		// Filters a dataset in order to keep only those question and answer 
		// pairs that are most suited for summarization
		// WARNING: VERY time consuming if done on the whole dataset 
		// (approximately 35 min)
		System.out.println("filtering dataset.");
		System.out.println("might take a LONG time (approx 35 min). " +
				"Please be patient.");
		Filter filter = new Filter(datasetInterface);
		filter.apply(database);
		
		// retrieve all answers
		query = "SELECT answer_id, ques_id, content, content_unprocessed, " +
		"user_id, isbest FROM FilteredAnswers ORDER BY ques_id";
		ResultSet filteredAnswers = database.ask(query);

		// retrieve all questions
		query = "SELECT ques_id, subject, content, content_unprocessed, " +
		"subject_unprocessed FROM FilteredQuestions ORDER BY ques_id";
		ResultSet filteredQuestions = database.ask(query);
		
		// construct interface to the dataset in the database
		Interface filteredInterface = 
			new Interface(filteredAnswers, filteredQuestions, users);
		filteredInterface.statisticsDataset("FILTERED");
		filteredInterface.outputDataset("FILTERED");
		
		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");
	}

}
