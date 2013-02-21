package qqa;

import java.sql.ResultSet;

import qqa.be.BEXtractor;
import qqa.db.MysqlConnect;

/** 
 * WARNING: VERY time consuming
 * extract BEs: save answers to files and call BEwT_E on them to generate
 * BE files and equivalence classes. These files are saved in 
 * _QQA/data/BE/final/BEs
 * _QQA/data/BE/final/eqClasses
 * WARNING: the content of those directories will be deleted
 * 
 * @author Mattia Tomasoni
 */
public class TEST_D_Extract_BEs {
	/** 
	 *  connection to database
	 */
	private static MysqlConnect database = null;
	
	public static void main(String[] args) throws Exception{
		// keep track of execution time
		long start = System.currentTimeMillis();
		
		// connecting to database
		database = new MysqlConnect();
		
		// retrieve all answers
		String query = "SELECT answer_id, ques_id, content, content_unprocessed, " +
		"user_id, isbest FROM TestAnswers ORDER BY ques_id";
		ResultSet filteredAnswers = database.ask(query);
		// retrieve all questions
		query = "SELECT ques_id, subject, content, content_unprocessed, " +
		"subject_unprocessed FROM TestQuestions ORDER BY ques_id";
		ResultSet filteredQuestions = database.ask(query);
		
		///////////////////////////////////////////////////////////////////////
		// build BE table
		///////////////////////////////////////////////////////////////////////
		// extract BEs and stores results to file in QQA/data/BE/final/BEs and 
		// QQA/data/BE/final/eqClasses.
		// WARNING: VERY time consuming
		// use it once and then rely on QQA/data/BE/final/BEs/* and 
		// QQA/data/BE/final/eqClasses/*
		System.out.println("building BE representation of dataset.");
		System.out.println("might take a LONG time. Please be patient.");
		extractBEs(filteredAnswers, filteredQuestions);
		
		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");
	}
	
	/**
	 * WARNING: VERY time consuming
	 * extract BEs: save answers to files and call BEwT_E on them to generate
	 * BE files and equivalence classes. These files are saved in 
	 * _QQA/data/BE/final/BEs
	 * _QQA/data/BE/final/eqClasses
	 * WARNING: this method deletes the content of those directories
	 * @param answers
	 * @return
	 * @throws Exception
	 */
	public static void extractBEs(ResultSet answers, ResultSet questions) 
	throws Exception{
		// create a BE extractor to apply BEwT_E steps to question and answers
		BEXtractor BEXt = new BEXtractor();
		// better one answer at a time, the process is very RAM consuming!
		answers.beforeFirst();
		while (answers.next()) {
			// Step1; parameter is true if the document is an answer
			BEXt.Step1(answers, true);
			// Step2
			BEXt.Step2();
			// Step3
			BEXt.Step3();
		}
		// one question at a time
		questions.beforeFirst();
		while (questions.next()) {
			// Step1; parameter is false if the document is a question
			BEXt.Step1(questions, false);
			// Step2
			BEXt.Step2();
			// Step3
			BEXt.Step3();
		}
	}

}
