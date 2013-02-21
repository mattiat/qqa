package qqa.be;


import java.sql.ResultSet;
import java.util.Hashtable;

import qqa.db.dataset.Interface;

/**
 * stores in memory the corresponding ids of each document (answer or question)  
 * and for each sentence, the content, parsed content, BEs and equivalence 
 * classes
 * @author Mattia Tomasoni
 *
 */
public class BETable {
	/**
	 * answers in the dataset. Their ids are needed to build the BETable
	 */
	public ResultSet answers;
	
	/**
	 * questions in the dataset. Their ids are needed to build the BETable
	 */
	public ResultSet questions;
	
	/**
	 * table containing a string id for each document in the dataset (answer or
	 * question) and the corresponding BEs and Equivalence classes
	 */
	public Hashtable<String, DocumentBEs> table;
	
	/**
	 * save a reference to the answers in the dataset.
	 * @param answers_par
	 */
	public BETable(Interface filteredInterface){
		answers = filteredInterface.answers;
		questions = filteredInterface.questions;
		table = new Hashtable<String, DocumentBEs>();
	}
	
	/**
	 * for currently selected document in the Result, save corresponding BEs
	 * and Equivalence classes in a DocumentBEs object.
	 * @throws Exception 
	 */
	public void build() throws Exception{
		answers.beforeFirst();
		while (answers.next()) {
			String answer_id = answers.getString("answer_id");
			String ques_id = answers.getString("ques_id");
			String doc_id = ques_id + "." + answer_id;
			// create DocumentBEs object and store in the table
			DocumentBEs doc = new DocumentBEs(doc_id);
			table.put(doc_id, doc);
		}
		questions.beforeFirst();
		while (questions.next()) {
			String ques_id = questions.getString("ques_id");
			String doc_id = ques_id + "." + "000";
			// create DocumentBEs object and store in the table
			DocumentBEs doc = new DocumentBEs(doc_id);
			table.put(doc_id, doc);
		}
	}
}
