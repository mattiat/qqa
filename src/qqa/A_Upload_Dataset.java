package qqa;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import qqa.db.MysqlConnect;
import qqa.db.MysqlCreateDatabase;
import qqa.db.dataset.AnswerExtractor;
import qqa.db.dataset.CategoryExtractor;
import qqa.db.dataset.Extractor;
import qqa.db.dataset.Interface;
import qqa.db.dataset.QuestionExtractor;
import qqa.db.dataset.UserExtractor;

/**
 * WARNING: VERY time consuming
 * upload dataset to database
 * 
 * @author Mattia Tomasoni
 */
public class A_Upload_Dataset {
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
		
		///////////////////////////////////////////////////////////////////////
		// upload dataset to database
		///////////////////////////////////////////////////////////////////////
		// WARNING: VERY time consuming
		// use it once and then rely on database
		System.out.println("uploading dataset to database.");
		System.out.println("might take a LONG time. Please be patient.");
		uploadDataset(args);
		
		///////////////////////////////////////////////////////////////////////
		// output dataset in human readable format
		///////////////////////////////////////////////////////////////////////
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
		datasetInterface.statisticsDataset("UNFILTERED");
		datasetInterface.outputDataset("UNFILTERED");
		
		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");
	}
	
	/**
	 * WARNING: VERY time consuming
	 * upload dataset to database
	 * @param args
	 * @throws SQLException
	 * @throws IOException
	 */
	public static void uploadDataset(String[] args) 
	throws SQLException, IOException{
		// reading user-defined parameters: file containing dataset 
		String answersStr = args[0];
		String categoriesStr = args[1];
		String questionsStr = args[2];
		String usersStr = args[3];
		// create tables in database to store the dataset the will be uploaded 
		// from files
		System.out.println("Creating database");
		MysqlCreateDatabase.createAnswersTable(database); 
		MysqlCreateDatabase.createCategoriesTable(database);
		MysqlCreateDatabase.createQuestionsTable(database);
		MysqlCreateDatabase.createUsersTable(database);
		
		// creating and executing extractors for each type of information
		// in the dataset: answers, categories, questions and users are
		// extracted from the files and stored in the respective tables
		Extractor ext;
		// questions
		ext = new QuestionExtractor(questionsStr);
		System.out.println("Storing questions into the database");
		ext.extract(database);
		// answers
		ext = new AnswerExtractor(answersStr);
		System.out.println("Storing answers into the database");
		ext.extract(database);
		// categories
		ext = new CategoryExtractor(categoriesStr);
		System.out.println("Storing categories into the database");
		ext.extract(database);
		// users
		ext = new UserExtractor(usersStr);
		System.out.println("Storing users into the database");
		ext.extract(database);
	} 
}
