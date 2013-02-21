package qqa.experiments;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

/**
 * This class runs the ROUGE evaluation on all documents contained in 
 * ROUGE-1.5.5/peers evaluating them against documents contained in 
 * ROUGE-1.5.5/models. The output will be saved in ROUGE-1.5.5/ROUGEoutput
 * and then processed to get mean values for Recall, Precision and F1 score.
 * @author Mattia Tomasoni
 *
 */
public class RougeEvaluation {

	/**
	 * file containing the ROUGE scores for all the answers. They will be
	 * averaged and a mean score will be outputed to the user
	 */
	// TODO: this should go in a configuration file
	static String ROUGEoutputFile = "../ROUGE-1.5.5/ROUGEoutput";

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, 
	InterruptedException {
		new File(ROUGEoutputFile).delete();
		// get all peers to be evaluated
		File dir = new File("../ROUGE-1.5.5/peers");
		File[] question_ids = dir.listFiles();
		// for each peer
		for (int i = 0; i < question_ids.length; i++) {
			//TODO: these should go in configuration file. 
			String cmd = "cd ../ROUGE-1.5.5/; ./ROUGE-1.5.5.pl -a " + args[0] +
			" ./config/" + question_ids[i].getName().split("\\.")[0] + ".xml";
			System.out.println("\n" + question_ids[i].getName() + "\n");
			// execute command (output will be saved in file ROUGEoutput)
			executeScript(cmd);
		}
		System.out.println("The output is also availabel in" + ROUGEoutputFile);
		// process the file ROUGEoutput and generate statistics
		System.out.println("\nFINAL ROUGE PERFORMANCES for  ");
		processResults();
	}

	/**
	 * method to execute a bash command: is used to execute the ROUGE scripts
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
		OutputStream out = new FileOutputStream(
				new File(ROUGEoutputFile), true);
		int c;
		// print output Step1
		while ((c = shellIn.read()) != -1) {
			System.out.write(c);
			// save output to file
			out.write(c);
		}
		out.close();
		try {shellIn.close();} catch (IOException ignoreMe) {}
	}

	/**
	 * method to evaluate the ROUGE results: it creates mean Recall, Precision 
	 * and F1 scores based on the output saved in ROUGE-1.5.5/ROUGEoutput
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	private static void processResults() throws NumberFormatException, IOException{
		String[] RougeVersion = {"ROUGE-1","ROUGE-2","ROUGE-L"};
		// for each ROUGE measure
		for (int i = 0; i < RougeVersion.length; i++) {
			Vector<Double> Recall = new Vector<Double>();
			Vector<Double> Precision = new Vector<Double>();
			Vector<Double> F1Score = new Vector<Double>();
			BufferedReader input = new BufferedReader(new FileReader(ROUGEoutputFile));
			String line;
			// go through all lines in the file
			while (( line = input.readLine()) != null){
				try{
					String version = line.substring(4, 11);
					// process only info regarding current version
					if(version.equalsIgnoreCase(RougeVersion[i])){
						String measure = line.substring(12, 21);
						String value = line.substring(23, 30);
						if(measure.equalsIgnoreCase("Average_R")){
							// store Recall
							Recall.add(Double.valueOf(value));
						} else if(measure.equalsIgnoreCase("Average_P")){
							// store Precision
							Precision.add(Double.valueOf(value));
						} else if(measure.equalsIgnoreCase("Average_F")){
							// store F1 score
							F1Score.add(Double.valueOf(value));
						}
					}
				} catch(IndexOutOfBoundsException e){}
			}
			// output for current ROUGE version
			System.out.println(RougeVersion[i]);
			// mean Recall
			Double MeanRecall = 0.0;
			for (Double r : Recall) MeanRecall += r;
			MeanRecall = MeanRecall/Recall.size();
			System.out.println("Recall: " + MeanRecall);
			// mean Precision
			Double MeanPrecision = 0.0;
			for (Double r : Precision) MeanPrecision += r;
			MeanPrecision = MeanPrecision/Precision.size();
			System.out.println("Precision: " + MeanPrecision);
			// mean FMeasure
			Double MeanFMeasure = 0.0;
			for (Double r : F1Score) MeanFMeasure += r;
			MeanFMeasure = MeanFMeasure/F1Score.size();
			System.out.println("FMeasure: " + MeanFMeasure);
		}
	}

}

