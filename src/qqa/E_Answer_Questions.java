package qqa;

import java.io.File;
import java.io.FileWriter;

/**
 * This class invokes the QQA_API answering all (100) annotated questions using 
 * the Pie (multiplication) scoring function: the QQA/data/answers folder is 
 * populated with human readable results (question + summarized answer + 
 * original answers with corresponding ids) and the ROUGE-1.5.5/peers directory
 * is populated with the machine readable text (only summarized answer, one
 * sentence per line) for ROUGE evaluation.
 * 
 * @author Mattia Tomasoni
 */
public class E_Answer_Questions {
	/**
	 * builds an API object and answers all questions in the database saving
	 * the results in /data/answers
	 * @param args
	 * @return
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception{
		// Preliminary operations to clean up files created during old runs
		// empty the data/answers directory
		File[] files = (new File("data/answers")).listFiles();
		for (int i = 0; i < files.length; i++) files[i].delete();
		// deleting groups.txt file
		new File("data/annotation/groups.txt").delete();
		// empty the data/annotation directory
		files = (new File("data/annotation")).listFiles();
		for (int i = 0; i < files.length; i++) {
			File[] old = files[i].listFiles();
			for (int j = 0; j < old.length; j++) old[j].delete();
			files[i].delete();
		}
		// creating groups.txt file
		// will contain answered question ids and contents one per line
		new File("data/annotation/groups.txt");
		// empty the ROUGE-1.5.5/peers directory
		files = (new File("../ROUGE-1.5.5/peers")).listFiles();
		for (int i = 0; i < files.length; i++) files[i].delete();
		// API to Qualitative QA system
		QQA_API api = null;
		if(args[1].equals("SIGMA")) {
			// API to test set: the rest of the summarizable questions were
			// used by F_Learn_Weights to train the SIGMA scoring function
			// classifier
			api = new QQA_API("test");
		}
		if(args[1].equals("PIE")) {
			// API to all summarizable questions (train + test) since no 
			// training is required for PIE scoring function
			api = new QQA_API("all");
		}
		boolean done = false;
		// process all questions
		while (! done){
			String[]  question = api.getNextQuestion();
			if(! question[1].equals("done")){
				// add line to groups.txt file

				// Save all answers to a file identified by the question id
				FileWriter fw = 
					new FileWriter("data/answers/" + question[0] + ".000.txt", 
							true);
				// append question to file
				fw.write(question[1].toUpperCase() + "\n");
				// invoke appropriate getSummary method according to the scoring
				// function required
				if(args[1].equals("SIGMA")) {
					// append (summarized) answer to file
					fw.write(QQA_API.getSummaryML(args[0]));
				}
				if(args[1].equals("PIE")) {
					// append (summarized) answer to file
					fw.write(QQA_API.getSummary(args[0]));
				}
				fw.close();
			}
			else done = true;
		}
	}
}
