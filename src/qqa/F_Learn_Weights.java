package qqa;

import java.io.File;
import java.io.FileWriter;

import qqa.db.MysqlCreateDatabase;


public class F_Learn_Weights {
	/**
	 * builds an API object and create a new table to contain all concepts with 
	 * corresponding quality, coverage, relevance, novelty...; those
	 * will be used along with supervision coming from human generated summaries
	 * to train a classifier to properly combine them.
	 * Note: this code is kept separate form Answer_Question class so that summarization
	 * can be repeated without having to go through training.
	 * @param args
	 * @return
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{
		// API to Qualitative QA system
		QQA_API api = new QQA_API("train");
		// creating table in the database where all concepts and corresponding
		// weights will be stored along with the isselected value 
		MysqlCreateDatabase.createWeightsTable(QQA_API.database);
		// for each question, store weights for each concept in the database
		boolean done = false;
		while (! done){
			String[]  question = api.getNextQuestion();
			if(! question[1].equals("done")){
				// store all weights
				QQA_API.storeWeights();
			}
			else done = true;
		}
		// for each question, store supervision for each concept in the database
		api.reset();
		done = false;
		while (! done){
			String[]  question = api.getNextQuestion();
			if(! question[1].equals("done")){
				// store all weights
				QQA_API.storeWeightSupervision();
			}
			else done = true;
		}
	}
}
