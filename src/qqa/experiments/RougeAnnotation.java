package qqa.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Vector;

/**
 * This class processes the annotation given in data/annotation_result and 
 * populates ROUGE-1.5.5/models directory (note that ROUGE-1.5.5/peers is 
 * populated any time QQA is run!); it then creates the corresponding 
 * ROUGE configuration files. 
 * It is intended to be run before running Rouge Evaluation.
 * @author Mattia Tomasoni
 */
public class RougeAnnotation {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// get all annotated question ids
		File dir = new File("data/annotation_result");
		File[] question_ids = dir.listFiles();
		// build filter for supervision files: we only want those and not
		// everything else contained in the directories we will process
		FilenameFilter select = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.contains("summary") && !name.contains("auto")) 
					return true;
				else return false;
			}
		};
		
		// for each question
		for (int i = 0; i < question_ids.length; i++) {
			// create rouge configuration file
			new File("../ROUGE-1.5.5/config/" + question_ids[i].getName() + 
					".xml").delete();
			FileWriter cw = new FileWriter("../ROUGE-1.5.5/config/" + 
					question_ids[i].getName() + ".xml", true);
			// write configuration file 
			String confSrt = "<ROUGE_EVAL version=\"1.5.5\">\n";
			confSrt+= "<EVAL ID=\"" + question_ids[i].getName() + "\">\n";
			confSrt+= "<PEER-ROOT>\n";
			//TODO: these should go in configuration file. 
			confSrt+= "/home/sphereworld/workspace/ROUGE-1.5.5/peers\n";
			confSrt+= "</PEER-ROOT>\n";
			confSrt+= "<MODEL-ROOT>\n";
			confSrt+= "/home/sphereworld/workspace/ROUGE-1.5.5/models\n";
			confSrt+= "</MODEL-ROOT>\n";
			confSrt+= "<INPUT-FORMAT TYPE=\"SPL\">\n";
			confSrt+= "</INPUT-FORMAT>\n";
			confSrt+= "<PEERS>\n";
			confSrt+= "";
			confSrt+= "<P ID=\"000\">";
			confSrt+=question_ids[i].getName() + ".000.spl</P>\n";
			confSrt+= "</PEERS>\n";
			confSrt+= "<MODELS>\n";
			// construct representation of all possible sentences in all the 
			// answers: hashtable with sentence ids and contents
			Hashtable<String, String> answer = new Hashtable<String, String>();
			BufferedReader input = new BufferedReader(
					new FileReader(question_ids[i].getAbsolutePath() +
					"/sentences.txt"));
			String line;
			while (( line = input.readLine()) != null){
				String[] parts = line.split("\t");
				answer.put(parts[0], parts[1]);
			}
			
			//filter supervision files
			File[] supervisors = question_ids[i].listFiles(select);
			// for each supervision file in the question directory
			for (int j = 0; j < supervisors.length; j++) {
				// open the file
				input = new BufferedReader(
						new FileReader(supervisors[j].getAbsoluteFile()));
				// build data structure to capture content of file
				Vector<String> sentences = new Vector<String>();
				input.readLine(); // skip first line
				while (( line = input.readLine()) != null){
					sentences.add(line);
				}
				// extract name of annotator (precedes "_")
				String name = supervisors[j].getName();
				int index = name.indexOf("_");
				name = ((String) name.subSequence(0, index)).toUpperCase();
				// create rouge annotation file
				new File("../ROUGE-1.5.5/models/" + question_ids[i].getName() + 
						"." + name).delete();
				// fill in the annotation file
				FileWriter rw = new FileWriter("../ROUGE-1.5.5/models/" + 
						question_ids[i].getName() + "." + name, true);
				for (String sentence : sentences) {
					rw.write(answer.get(sentence) + "\n");
				}
				rw.close();
				confSrt+= "<M ID=\"" + name + "\">";
				confSrt+=question_ids[i].getName() + "." + name + "</M>\n";
			}
			confSrt+= "</MODELS>";
			confSrt+= "</EVAL>";
			confSrt+= "</ROUGE_EVAL>";
			cw.write(confSrt);
			input.close();
			cw.close();
		}
	}

}
