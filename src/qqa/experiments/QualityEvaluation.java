package qqa.experiments;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import qqa.db.MysqlConnect;
import qqa.ml.QualityExperiments;
import qqa.ml.Ranker;

public class QualityEvaluation {
	/**
	 * table containing the dataset to be tested
	 */
	static String datasetTable = "Features";

	/**
	 * size of the test 
	 */
	static int testSize = 3000;

	/** 
	 *  connection to database
	 */
	private static MysqlConnect database = null;

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		database = new MysqlConnect();

		// structure to capture recall as training size increases for each 
		// given experiment (offset)
		Hashtable<Integer, Vector<Double>> Recalls = 
			new Hashtable<Integer, Vector<Double>>();
		// Progressively increase training size
		// table Features has in total 89,814 answers
		for (int trainSize = 10; trainSize < 15000; trainSize+=300){
			// offset in the SELECT statement that retrieves the dataset
			for (int offset=0; offset+testSize+trainSize<89000; offset+=20000){
				Ranker testRank = new QualityExperiments(null, 
						null, database, trainSize, testSize, datasetTable, 
						String.valueOf(offset));
				Double r = 0.0;
				// call appropriate classifier
				if(args[0].equals("LLR")){
					r = QualityExperiments.listWiseTest();
				} else{
					r = ((QualityExperiments) testRank).
					testLinearRegression(null);
				}
				// if the offset is already in the hashtable...
				if(Recalls.containsKey(trainSize)){
					//retrieve it and augment the vector with the new recall
					Recalls.get(trainSize).add(r);
				} //...if not add the offset to the hashtable
				else{
					Vector<Double> v = new Vector<Double>();
					v.add(r);
					Recalls.put(trainSize, v);
				}
			}
		}

		// calculate average values
		Vector<Double> MeanRecalls = new Vector<Double>();
		Enumeration<Integer> key = Recalls.keys();
		System.out.println(args[0] + " EVALUATION");
		// save the output to file 
		new File("data/qualityRanking/QUALITYoutput_size").delete();
		new File("data/qualityRanking/QUALITYoutput_recall").delete();
		FileWriter sw = new FileWriter("data/qualityRanking/QUALITYoutput_size",true);
		FileWriter rw = new FileWriter("data/qualityRanking/QUALITYoutput_recall",true);
		// for each training set size
		while(key.hasMoreElements()){
			
			Integer trainSize = key.nextElement();
			// get vector containing all Recall values for different offsets
			Vector<Double> v = Recalls.get(trainSize);
			Double sum = 0.0;
			for (Double r : v) sum += r;
			// calculate average
			Double mean = sum/v.size();
			// store average
			MeanRecalls.add(mean);
			System.out.println("(trainig set size = "+ trainSize +" ) Recall: " 
					+ mean);
			sw.write(trainSize + "\n");
			rw.write(mean + "\n");
		}
		
		rw.close();
		sw.close();

	}
}

