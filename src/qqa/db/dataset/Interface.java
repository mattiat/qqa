package qqa.db.dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;

/**
 * class that implements an interface to the dataset as stored in the database:
 * it allows to output data and statistics in human readable form to file
 * 
 * @author Mattia Tomasoni
 *
 */
public class Interface {
	/**
	 * reference to the answers dataset as in database
	 */
	public ResultSet answers = null;
	
	/**
	 * reference to the questions dataset as in database
	 */
	public ResultSet questions = null;
	
	/**
	 * reference to the users dataset as in database
	 */
	public ResultSet users = null;
	
	/**
	 * constructor that initializes answers and questions filed
	 * @param answers
	 * @param questions
	 */
	public Interface(ResultSet answersPar, ResultSet questionsPar, ResultSet
			usersPar){
		answers = answersPar;
		questions = questionsPar;
		users = usersPar;
	}

	/**
	 * Computes some statistics about the answers in the dataset:
	 * _answer length
	 * _sum of length of all answers to a question
	 * _sum of length of all answers over number of answers to a question
	 *
	 * @param answers
	 * @param questions
	 * @throws IOException
	 * @throws SQLException
	 */
	public void statisticsDataset(String Name) 
	throws IOException, SQLException{
		// answer length
		int[] singleAnswerLength = new int[1000];
		// sum of length of all answers to a question
		int[] totalAnswersLength = new int[1000];
		// sum of length of all answers over number of answers to a question
		int[] normalAnswerLength = new int[1000];
		// number of answers to a question
		int[] answersNumber = new int[100];
		// total number of answers
		int totalanswersCounter = 0;
		// total number of questions
		int totalquestionsCounter = 0;
		new File("data/statisticsDataset" + Name + ".txt").delete();
		FileWriter fw = 
			new FileWriter("data/statisticsDataset" + Name + ".txt", true);
		questions.beforeFirst();
		// for each question
		while (questions.next()) {
			// update total number of questions
			totalquestionsCounter ++;
			String ques_id = questions.getString("ques_id");
			int singleLength = 0;
			int totalLength = 0;
			int answersCounter = 0;
			answers.beforeFirst();
			// for all answers to this question
			try{
				while (answers.next()) {
					if(answers.getString("ques_id").equalsIgnoreCase(ques_id)){
						// update total number of answers
						totalanswersCounter ++;
						// update answer number counter for this question
						answersCounter ++;
						// calculate answer length (using the processed data)
						String content = answers.getString("content");
						singleLength = content.split(" ").length;
						// update number of answers with length equals to 
						// singleLength
						singleAnswerLength[singleLength]++;
						// update sum of the length of all answers to this 
						// question
						totalLength += singleLength;
					}
				}
				// update number of answers with sum of the length of all 
				// answers equals to totalLength
				totalAnswersLength[totalLength]++;
				// update number of answers with sum of the length of all 
				// answers (normalized in the number of answers for this 
				// question) equals to totalLength/answersCounter
				int norm = 0;
				if(answersCounter != 0)
					norm = (int) (totalLength/answersCounter);
				normalAnswerLength[norm]++;
				// update the number of questions with number of answers
				// equals to answersCounter
				answersNumber[answersCounter]++;
			} 
			// ignore answers with lengths longer then threshold
			catch (ArrayIndexOutOfBoundsException ignore){}
		} // all questions have been processed
		// output statistics to file
		fw.write(totalanswersCounter + " ANSWERS and ");
		fw.write(totalquestionsCounter + " QUESTIONS.\n");
		float avg = (float) totalanswersCounter/ (float) totalquestionsCounter;
		fw.write(avg + " ANSWERS ON AVERAGE PER QUESTION.\n\n\n");
		fw.write("NUMBER OF ANSWERS (DISTRIBUTION):\n");
		for (int i = 0; i < answersNumber.length; i++) {
			fw.write(i + " " + answersNumber[i] + "\n");
		}
		fw.write("ANSWER LENGTH (DISTRIBUTION):\n");
		for (int i = 0; i < singleAnswerLength.length; i++) {
			fw.write(i + " " + singleAnswerLength[i] + "\n");
		}
		fw.write("\n\n\nSUM OF THE LENGTH OF ALL ANSWERS TO A QUESTION " +
				"(DISTRIBUTION):\n");
		for (int i = 0; i < totalAnswersLength.length; i++) {
			fw.write(i + " " + totalAnswersLength[i] + "\n");
		}
		fw.write("\n\n\nSUM OF THE NORMALIZEDLENGTH OF ALL ANSWERS " +
				"(DISTRIBUTION):\n");
		for (int i = 0; i < normalAnswerLength.length; i++) {
			fw.write(i + " " + normalAnswerLength[i] + "\n");
		}
		fw.close();
	}


	/**
	 * Output dataset (human readable format) 
	 *
	 * @param answers
	 * @param questions
	 * @throws IOException
	 * @throws SQLException
	 */
	public void outputDataset(String Name) 
	throws IOException, SQLException{
		new File("data/outputDataset" + Name + ".txt").delete();
		FileWriter fw = 
			new FileWriter("data/outputDataset" + Name + ".txt", true);
		questions.beforeFirst();
		//for each question
		while (questions.next()) {
			String ques_id = questions.getString("ques_id");
			Vector<String> answers_content = new Vector<String>();
			String isbest_content = new String("");
			answers.beforeFirst();
			// memorize all its answers in temporary vectors
			while (answers.next()) {
				if(answers.getString("ques_id").equalsIgnoreCase(ques_id)){
					String content = answers.getString("content_unprocessed");
					String answer_id = answers.getString("answer_id");
					if(answers.getString("isbest").equals("1"))
						isbest_content = answer_id + "\n" + content;
					else
						answers_content.add( answer_id + "\n" + content);
				}
			}
			// output question and all answers to file
			if(!answers_content.isEmpty() || !isbest_content.equals("")){
				fw.write("QUESTION ID: " + ques_id + "\n");
				fw.write("QUESTION SUBJECT: ");
				fw.write(questions.getString("subject_unprocessed") + "\n");
				fw.write("QUESTION CONTENT: ");
				fw.write(questions.getString("content_unprocessed") + "\n");
				fw.write("(BEST) ANSWER ID: ");
				fw.write(isbest_content + "\n");
				for (Iterator<String> it = answers_content.iterator(); 
				it.hasNext();) {
					String content = (String) it.next();
					fw.write("(OTHER) ANSWER ID: " + content + "\n");
				}
				fw.write("\n"); 
			}
		}
		fw.close();
	}
}
