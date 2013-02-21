package qqa;

import java.sql.ResultSet;

import qqa.db.MysqlConnect;
import qqa.db.dataset.Interface;

/**
 * WARNING: VERY time consuming
 * compute quality features
 * 
 * @author Mattia Tomasoni
 */
public class B_Compute_Quality_Features {
	/** 
	 *  connection to database
	 */
	private static MysqlConnect database = null;
	
	/**
	 * contains features and inverse table and methods to fill them
	 */
	public static Features features;
	
	public static void main(String[] args) throws Exception{
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
		// compute quality features
		///////////////////////////////////////////////////////////////////////
		// compute quality features and store them to database
		// WARNING: VERY time consuming
		// use it once and then rely on database
		System.out.println("computing quality features.");
		System.out.println("might take a LONG time. Please be patient.");
		computeFeatures(datasetInterface);
		
		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");
	}
	
	/**
	 * WARNING: VERY time consuming
	 * compute quality features
	 * @param datasetInterface
	 * @throws Exception
	 */
	public static void computeFeatures(Interface datasetInterface) 
	throws Exception{
		ResultSet answers = datasetInterface.answers;
		ResultSet questions = datasetInterface.questions;
		ResultSet users = datasetInterface.users;
		features = new Features(answers);

		// 1. answer length
		features.answerLengthFeature(answers);
		// constructing inverse feature table
		// needed for the next feature, meaningful words
		features.createInverseTable(answers);
		// 2. meaningful words
		features.meningfulWordsFeature(answers);
		// 3. answerer points
		features.answererPointsFeature(answers, users);
		// 4. answerer's best answers ratio
		features.bestAnswersRatioFeature(answers, users);
		// 5. words overlap between question and answer
		features.overlapFeature(answers, questions);
		// creating tables in database to store the qqa.features
		features.storeFeatureTable(database);
	}

}
