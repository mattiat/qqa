/**
 * Qualitative Question Answering
 */
package qqa;

import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import qqa.be.BETable;
import qqa.be.DocumentBEs;
import qqa.be.SentenceBEs;
import qqa.db.MysqlConnect;
import qqa.db.dataset.Interface;
import qqa.ml.ILP;


/**
 * Qualitative Question and Answering package
 * 
 * @author Mattia Tomasoni
 *
 */
public class TEST_QQA {
	/** 
	 *  connection to database
	 */
	private static MysqlConnect database = null;
 
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		// keep track of execution time
		long start = System.currentTimeMillis();

		// connecting to database
		database = new MysqlConnect();
		
		// retrieve all answers
		String query = "SELECT answer_id, ques_id, content, " +
		"content_unprocessed, user_id, isbest FROM TestAnswers " +
		"ORDER BY ques_id";
		ResultSet filteredAnswers = database.ask(query);
		// retrieve all questions
		query = "SELECT ques_id, subject, content, content_unprocessed, " +
		"subject_unprocessed FROM TestQuestions ORDER BY ques_id";
		ResultSet filteredQuestions = database.ask(query);
		// construct interface to the dataset in the database
		Interface filteredInterface = new Interface(filteredAnswers, 
				filteredQuestions, null);
		
		// create a BE table to store BEs
		// builds a BETable object and fills it with the information contained
		// in the files created by extractBEs.
		System.out.println("Retrieving BE representation of dataset...");
		BETable bETable = new BETable(filteredInterface);
		bETable.build();
		System.out.println("Successfully retrieved.");

		///////////////////////////////////////////////////////////////////////
		// assessing quality
		///////////////////////////////////////////////////////////////////////
		// fake it baby!
		
		///////////////////////////////////////////////////////////////////////
		// Sentence selection
		///////////////////////////////////////////////////////////////////////
		// question to be summarized
		String ques_id = "TEST";
		// creating pointer to question in the BETable
		DocumentBEs question = bETable.table.get(ques_id + ".000");
		
		// retrieving corresponding answers
		Vector<DocumentBEs> answers = new Vector<DocumentBEs>();
		bETable.answers.beforeFirst();
		while (bETable.answers.next()){
			if(bETable.answers.getString("ques_id").equalsIgnoreCase(ques_id)) {
				String answer_id = bETable.answers.getString("answer_id");
				// creating pointer to answer in the BETable
				answers.add(bETable.table.get(ques_id + "." + answer_id));
			}
		}
		
		// TODO: retrieve similar questions with API?
		
		// calculating BE weights
		// for each answer
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			DocumentBEs answer = a.next();
			// for each sentence
			for (Iterator<SentenceBEs> s = answer.sentences.iterator(); 
			s.hasNext();) {
				SentenceBEs sentence = s.next();
				// for each BE
				for (Enumeration<String> e = sentence.eqClasses.keys(); 
				e.hasMoreElements() ;) {
					String be = e.nextElement();
					Double quality=0.0;
					if(answer.doc_id.equalsIgnoreCase("TEST.9")){
						quality = 0.0;
					}
					if(answer.doc_id.equalsIgnoreCase("TEST.8")){
						quality = 0.0;
					}
					if(answer.doc_id.equalsIgnoreCase("TEST.7")){
						quality = 0.0;
					}
					// RELEVANCE
					Double relevance = assessRelevance(be, sentence, question);
					// COVERAGE
					Double coverage = assessCoverage(be, sentence, answers);
					// compute BE weight
					Double weight = (quality + relevance + coverage) / 3;
					// store weight
					sentence.weights.put(be, weight);
			     }
			}
		}
		
		// solving ILP problem
		// compute length limit for the answer
		int SummaryLength = 5;
		
		ILP sentenceSelector = new ILP(SummaryLength);
		// solve ILP problem
		sentenceSelector.solveBE(answers, question);
		
		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");

	}
	
	/**
	 * returns the relevance of a be to its question
	 * @param be
	 * @param question
	 */
	public static Double assessRelevance(String be, SentenceBEs sBE, 
			DocumentBEs question){
		Double relSentences = 0.0;
		// for each sentence in the question
		for (Iterator<SentenceBEs> s = question.sentences.iterator(); 
		s.hasNext();) {
			SentenceBEs sentence = s.next();
			// for all the elements in the equivalence class of the be 
			Set<String> eqClass = sBE.eqClasses.get(be);
			for (String eqBE : eqClass) {
				// if the sentence contains the element
				if(sentence.eqClassesSet.contains(eqBE)) {
					// update number of relevant sentences
					relSentences++;
					// jump to next sentence
					break;
				}
			}
		}
		return relSentences/question.sentences.size();
	}
	
	/**
	 * returns the coverage of a be, that is the percentage of answers 
	 * containing the be among those answering the same question as the
	 * one the be is being extracted
	 * @param be
	 * @param question
	 * @param answer
	 * @return
	 */
	public static Double assessCoverage(String be, SentenceBEs sBE, 
			Vector<DocumentBEs> answers){
		// number of answers containing the be
		Double relSentences = 0.0;
		// number of sentences
		Double totSentences = 0.0;
		// for each answer
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			DocumentBEs answer = a.next();
			// for each sentence
			for (Iterator<SentenceBEs> s = answer.sentences.iterator(); 
			s.hasNext();) {
				totSentences++;
				SentenceBEs sentence = s.next();
				// for all the elements in the equivalence class of the be 
				Set<String> eqClass = sBE.eqClasses.get(be);
				for (String eqBE : eqClass) {
					// if the sentence contains the element
					if(sentence.eqClassesSet.contains(eqBE)) {
						// update number of relevant sentences
						relSentences++;
						// jump to next sentence
						break;
					}
				}
			}
		}
		// calculate coverage as explained in Essential Pages for term 
		// importance
		return relSentences/totSentences;
//		Double r = relAnswers/sentencesNumber;
//		if(r!=0)
//			return r*Math.log(1/r)/Math.log(2);
//		else 
//			return 0.0;

	}

}
