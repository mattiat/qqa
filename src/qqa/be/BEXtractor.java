package qqa.be;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 
 * Saves answers to files and allows to call BEwT_E on them to generate
 * BE files and equivalence classes. These files are saved in 
 * _QQA/data/BE/final/BEs
 * _QQA/data/BE/final/eqClasses
 * WARNING: creating an instance of this object deletes the content of those
 * directories!
 */
public class BEXtractor {
	
	/**
	 * Deletes the content of the following directories to avoid conflicts
	 * _QQA/data/BE/final/BEs
     * _QQA/data/BE/final/eqClasses
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public BEXtractor() throws IOException, InterruptedException{
		String remove = "rm data/BE/final/BEs/*";
		executeScript(remove);
		remove = "rm data/BE/final/eqClasses/*";
		executeScript(remove);
	}
	
	
	/**
	 * method to execute a bash command: is used to execute the BE scripts
	 * @param command
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void executeScript(String command) throws IOException, 
	InterruptedException{
		ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
		pb.redirectErrorStream(true); // capture stderr
		Process shell = pb.start(); //execute command
		InputStream shellIn = shell.getInputStream(); // capture output
		shell.waitFor(); // wait for the shell to finish
		int c;
		// print output Step1
		while ((c = shellIn.read()) != -1) {System.out.write(c);}
		try {shellIn.close();} catch (IOException ignoreMe) {}
	}
	
	/**
	 * Executes BE ant Spet1 command.
	 * Process input documents (summary directory): output parsed documents 
	 * (parsed directory)
	 * @param answers
	 * @throws SQLException
	 * @throws IOException 
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws InterruptedException
	 */
	public void Step1(ResultSet doc, boolean isAnswer) throws SQLException, 
	IOException, InterruptedException{
		// empty all output directories to avoid conflicts
		//TODO: these should go in configuration file. 
		// They are already in BEwT_E ant script: merge with QQA's one.
		String remove = "rm data/BE/summaries/*";
		executeScript(remove);
		remove = "rm data/BE/intermediate/parsed/*";
		executeScript(remove);
		
		// for the answer currently selected in the ResultSet
		String answer_id;
		String content;
		if(isAnswer) { // if answer
			answer_id = doc.getString("answer_id");
			content = doc.getString("content_unprocessed");
		}
		else { // if question
			answer_id = "000";
			content = doc.getString("subject_unprocessed") + 
			doc.getString("content_unprocessed");
		}
		String ques_id = doc.getString("ques_id");
		// save document content in a file for BE extraction
		String fileName = "data/BE/summaries/QQA." + ques_id + "." + answer_id;
		BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
		out.write(content);
		out.close();
		
		// run BE Step1:
		// produce parse documents from answers 
		String cmd = "cd ../BEwT_E_0.3/build/; ant Step1";
		executeScript(cmd);
	}
	
	/**
	 * Executes BE ant Spet2 command.
	 * Extracts BEs from parsed documents
	 * Process parsed documents (parsed directory): output BEs (BEs directory)
	 * @param answers
	 * @throws SQLException
	 * @throws IOException 
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws InterruptedException
	 */
	public void Step2() throws SQLException, 
	IOException, InterruptedException{
		// empty all output directories to avoid conflicts
		String remove = "rm data/BE/intermediate/BEs/00000000/*";
		executeScript(remove);
		// run BE Step2: 
		// extract BEs from parsed documents
		String cmd = "cd ../BEwT_E_0.3/build/; ant Step2";
		executeScript(cmd);
		// this file will be used to generate the BE index, but BEs/00000000
		// folder needs to be empty for step3 on the next document: more file
		// to a safe location!
		String move = "cp data/BE/intermediate/BEs/00000000/* " +
		"data/BE/final/BEs";
		executeScript(move);
	}
	
	/**
	 * Executes BE ant Spet3 command.
	 * Builds BEs equivalence classes
	 * Process BEs (BEs directory): outputs equivalence classes (EqClasses 
	 * directory)
	 * @param answers
	 * @throws SQLException
	 * @throws IOException 
	 * @throws IOException
	 * @throws InterruptedException 
	 * @throws InterruptedException
	 */
	public void Step3() throws SQLException, 
	IOException, InterruptedException{
		// empty all output directories to avoid conflicts
		String remove = "rm data/BE/intermediate/BEXs/00000000/*";
		executeScript(remove);
		// run BE Step3: 
		// build equivalence classes for extracted BEs
		String cmd = "cd ../BEwT_E_0.3/build/; ant Step3";
		executeScript(cmd);
	}
}
