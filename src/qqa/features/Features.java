package qqa.features;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import qqa.db.MysqlConnect;
import qqa.db.MysqlCreateDatabase;

public class Features {
	
	/**
	 * Hash table containing the features for all answers.
	 * String key is answer_id
	 */
	private Hashtable<String, Vector<Object>> featureTable;
	
	/**
	 * Inverse index table: String key is a term that is mapped to a collection
	 * of answer_id and Integers representing the occurrences of term in answer
	 */
	private Hashtable<String,Hashtable<String,Integer>> inverseTable;
	
	public Features(ResultSet answers) throws SQLException {
		// creating hash table for qqa.features
		featureTable = new Hashtable<String, Vector<Object>>();
		answers.beforeFirst();
		// storing answer keys and question ids
		while (answers.next()) {
			String answer_id = answers.getString("answer_id");
			String ques_id = answers.getString("ques_id");
			String isbest = answers.getString("isbest");
			Vector<Object> v = new Vector<Object>();
			v.add(0, isbest);
			v.add(1, ques_id);
			featureTable.put(answer_id, v);
		}
	}
	
	
	/**
	 * constructs the inverse index table  of all answers stored in the 
	 * database
	 * @param inverseTable
	 * @param answers
	 * @throws SQLException
	 */
	public void createInverseTable(ResultSet answers) throws SQLException{
		inverseTable = new Hashtable<String,Hashtable<String,Integer>>();
		answers.beforeFirst();
		while (answers.next()) {
			String content = answers.getString("content");
			String answer_id = answers.getString("answer_id");
			String [] terms = content.split(" ");
			for(String term : terms){
				// add document occurrence for term to inverse index
				// if the term is already in the index
				if(inverseTable.containsKey(term)){
					Hashtable<String,Integer> h = inverseTable.get(term);
					//if the document is already in the index for term
					if(h.containsKey(answer_id)){
						// add occurrence to document answer_id
						Integer occurrences = h.get(answer_id)+1;
						h.put(answer_id, occurrences);

					}
					//if the document is not in the index for term, create 
					// a new document entry
					else h.put(answer_id, 1);

				}
				// if the term is not in the index, create a new term entry
				else {
					Hashtable<String,Integer> h = new Hashtable<String,Integer>();
					// one occurrence in document answer_id
					h.put(answer_id,1);
					inverseTable.put(term,h);
				}
			}
		}
	}
	
	
	/**
	 * number of words in an answer
	 * @param answers
	 * @param database
	 * @throws SQLException 
	 * @throws SQLException
	 */
	public void answerLengthFeature(ResultSet answers) throws SQLException{
		// for all answers retrieved calculate answer_length
		System.out.println("1. answer_length...");
		answers.beforeFirst();
		while (answers.next()) {
			String answer_id = answers.getString("answer_id");
			String content = answers.getString("content");
			// number of words in the answer
			int answer_length = content.split(" ").length;
			// store length in feature table
			Vector<Object> v = featureTable.get(answer_id);
			v.add(2, answer_length);
		}
	}
	
	
	/**
	 * number of words in the answer with a corpus frequency larger then a 
	 * constant
	 * @param answers
	 * @param database
	 * @throws Exception 
	 */
	public void meningfulWordsFeature(ResultSet answers) 
	throws Exception{
		System.out.println("2. meaningful_words per answer...");
		int threshold = 5;
		answers.beforeFirst();
		while (answers.next()) {
			int meaningful_words = 0;
			String answer_id = answers.getString("answer_id");
			String content = answers.getString("content");
			// terms in the answer
			String[] terms = content.split(" ");
			for(String term : terms){
				Hashtable<String, Integer> h = inverseTable.get(term);
				// the answer must be in the term list 
				if(!h.containsKey(answer_id)) {
					throw new Exception("problems with the Inverse Index!");
				}
				// corpus frequency for term: sum of all document frequencies
				// in inverse table for the entry term
				int corpus_freq = 0;
				for (Enumeration<Integer> e = h.elements(); e.hasMoreElements();){
					corpus_freq += e.nextElement();
				}
				if(corpus_freq > threshold)
					meaningful_words++;
			}
			// store number of meaningful_words in feature table
			Vector<Object> v = featureTable.get(answer_id);
			v.add(3, meaningful_words);
		}
		
	}
	
	/**
	 * number of thumbs up minus thumbs down received by the answerer divided 
	 * by the total number of thumbs he/she received
	 * @param answers
	 * @param database
	 * @throws SQLException
	 */
	public void answererPointsFeature(ResultSet answers, ResultSet users) 
	throws SQLException{
		System.out.println("3. answerer_points...");
		answers.beforeFirst();
		//for each answer
		while (answers.next()) {
			String answer_id = answers.getString("answer_id");
			String user_id = answers.getString("user_id");
			users.beforeFirst();
			// find answerer in user table
			while (users.next()) {
				if(users.getString("user_id").equalsIgnoreCase(user_id)){
					// retrieve answerer's points
					String p = users.getString("total_points");
					// store them in feature table
					Vector<Object> v = featureTable.get(answer_id);
					v.add(4, p);
					// only one answerer
					break;
				}
			}
		}
	}

	
	/**
	 * fraction of answers of the answerer that have been picked as best 
	 * answers
	 * @param answers
	 * @param database
	 * @throws SQLException
	 */
	public void bestAnswersRatioFeature(ResultSet answers, ResultSet users)
	throws SQLException{
		System.out.println("4. best_answers_ratio...");
		answers.beforeFirst();
		//for each answer
		while (answers.next()) {
			String answer_id = answers.getString("answer_id");
			String user_id = answers.getString("user_id");
			users.beforeFirst();
			// find answerer in user table
			while (users.next()) {
				if(users.getString("user_id").equalsIgnoreCase(user_id)){
					// retrieve answerer's total_answers and best_answers
					String ta = users.getString("total_answers");
					String ba = users.getString("best_answers");
					// calculate ratio
					Double r = Double.parseDouble(ba)/Double.parseDouble(ta);
					// store it in feature table
					Vector<Object> v = featureTable.get(answer_id);
					if (!r.equals(Double.NaN))
						v.add(5, r);
					else
						// this should not happen: ta==0
						// inconsistency in the dataset
						v.add(5, 0.0);
					// only one answerer
					break;
				}
			}
		}
	}

	/**
	 * non-stopword overlap between the question and the answer
	 * @param answers
	 * @param questions
	 * @throws SQLException 
	 */
	public void overlapFeature(ResultSet answers, ResultSet questions) 
	throws SQLException {
		System.out.println("5. word overlap between question and answer...");
		answers.beforeFirst();
		//for each answer
		while (answers.next()) {
			int overlap = 0;
			String answer_id = answers.getString("answer_id");
			String ques_id = answers.getString("ques_id");
			// find corresponding question
			questions.beforeFirst();
			while (questions.next()) {
				if(questions.getString("ques_id").equalsIgnoreCase(ques_id)){
					// get question's subject and content
					String q = questions.getString("subject") + 
					questions.getString("content");
					String[] terms = q.split(" ");
					// for each term in the question
					for(String term : terms){
						// get list of documents containing that term
						Hashtable<String, Integer> h = inverseTable.get(term);
						// if the answer is among the retrieved documents... 
						if(h!= null && h.containsKey(answer_id)) {
							// ... then question and answer have term in common
							overlap ++;
						}
					}
					// only one question
					break;
				}
			}
			// store overlap in feature table
			Vector<Object> v = featureTable.get(answer_id);
			v.add(6, overlap);
		}
	}

	/**
	 * stores computed qqa.features in the database
	 * @param database
	 * @throws SQLException
	 */
	public void storeFeatureTable(MysqlConnect database) throws SQLException {
		MysqlCreateDatabase.createFeaturesTable(database);
		// for each question
		for (Enumeration<String> k = featureTable.keys(); k.hasMoreElements();){
			// get qqa.features
			String answer_id = k.nextElement();
			Vector<Object> features = featureTable.get(answer_id);
			// store qqa.features in database
			String features_query = "INSERT " +
			"Features (isbest, answer_id, ques_id, answer_length, " +
			"meaningful_words, answerer_points, best_answers_ratio, " +
			"overlap) VALUES (" +
			features.get(0) + ", " +
			"\'" + answer_id + "\', " +
			"\'" + features.get(1) + "\', " +
			features.get(2) + ", " +
			features.get(3) + ", " +
			features.get(4) + ", " +
			features.get(5) + ", " +
			features.get(6) +
			")";
			database.execute(features_query);
		}
	}
	
	/**
	 * isbest is the probability of an answer to be a best answer.It is learned
	 * based on the other qqa.features. This function sets its value back to what 
	 * it is stated in the answers table: 1 if it is actually a best answer, 0
	 * otherwise
	 * @param database
	 * @param answers
	 * @throws SQLException 
	 */
	public void resetIsBest(MysqlConnect database, ResultSet answers) 
	throws SQLException{
		while (answers.next()) {
			String answer_id = answers.getString("answer_id");
			String isbest = answers.getString("isbest");
			String query = "UPDATE Features SET isbest=\'" + isbest + "\' " +
					"WHERE answer_id=\'" + answer_id + "\'";
			database.execute(query);
		}
	}
	

}
