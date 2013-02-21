package qqa.experiments;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Vector;

/**
 * This class evaluates all documents contained in QQA/data/annotation by computing 
 * for each the Precision, Recall and F1-score for against the (human generated) 
 * summaries contained in QQA/data/annotation_result. Mean values are calculated 
 * and printed to screen.
 * @author Mattia Tomasoni
 *
 */
public class HumanEvaluation {
	static Vector<Double> Recall = new Vector<Double>();
	static Vector<Double> Precision = new Vector<Double>();
	static Vector<Double> F1Score = new Vector<Double>();

	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, 
	InterruptedException {
		// get all peers to be evaluated
		File[] question_ids = new File("data/annotation").listFiles();
		// for each peer, store a representation of the summary
		for (int i = 0; i < question_ids.length; i++){
			if(question_ids[i].isDirectory())
				evaluateSummary(question_ids[i]);
		}
		// process the file results and generate means
		System.out.println("HUMAN EVALUATION ");
		outputResults();
	}

	/**
	 * This method takes in input the directory where the peer is contained and
	 * evaluates it against the human supervision in data/annotation_result that
	 * matches its question_id. Stores results to Precision, Recall and F1Score
	 * vectors.
	 * @param question_id
	 * @throws IOException 
	 */
	private static void evaluateSummary(File question_id) 
	throws IOException{
		// representation of peer
		Vector<String> peerSentences = new Vector<String>();
		// open summary file
		BufferedReader pb = new BufferedReader(new FileReader(
				new File(question_id.getPath() + "/summary.txt")));
		// skip first line (it's the number of sentences in the file)
		String line = pb.readLine();
		while (( line = pb.readLine()) != null){
			// one sentence (id) per line: store it
			peerSentences.add(line);
		}
		pb.close();

		// build filter for supervision files: we only want those and not
		// everything else contained in the directories we will process
		FilenameFilter select = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.contains("summary") && !name.contains("auto")) 
					return true;
				else return false;
			}
		};
		// retrieve supervision files for peer
		File[] supervisors = (new File(
				"data/annotation_result/" + question_id.getName())).
				listFiles(select);
		// for each supervision file in the question directory
		for (int j = 0; j < supervisors.length; j++) {
			// open the supervision file
			BufferedReader mb = new BufferedReader(
					new FileReader(supervisors[j].getAbsoluteFile()));
			// build data structure to capture content of file
			Vector<String> modelSentences = new Vector<String>();
			mb.readLine(); // skip first line
			while (( line = mb.readLine()) != null){
				modelSentences.add(line);
			}
			mb.close();
			// calculate number of sentences in common between peer and model
			// (i.e., between machine generate summary and human supervision)
			Double overlap = 0.0;
			for(String sentence : peerSentences)
				if(modelSentences.contains(sentence)) overlap++;
			// calculate and store Recall
			Double R = overlap/modelSentences.size();
			Recall.add(R);
			// calculate and store Precision
			Double P = overlap/peerSentences.size();
			Precision.add(P);
			// calculate and store F1-Score
			Double f = 2*R*P/(P+R);
			if(f.isNaN()) f = 0.0;
			F1Score.add(f);
		}

	}

	/**
	 * method that creates means based on values stored in Precision, Recall 
	 * and F1Score vectors.
	 * @throws NumberFormatException
	 * @throws IOException
	 */
	private static void outputResults() throws NumberFormatException, IOException{
		
		Vector<Double> rr = Recall;
		Vector<Double> pp = Precision;
		Vector<Double> ff = F1Score;
		
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

