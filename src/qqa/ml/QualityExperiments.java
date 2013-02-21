package qqa.ml;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import qqa.be.BETable;
import qqa.db.MysqlConnect;
import qqa.db.dataset.Interface;
import qqa.ml.listwise.PredictLinearNeuralModel;
import qqa.ml.listwise.TrainLinearNeuralModel;
import weka.classifiers.functions.LinearRegression;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

/**
 * perform quality assessing experiments on the unfiltered version of the 
 * dataset
 * @author Mattia Tomasoni
 *
 */
public class QualityExperiments extends QualityRanker{
	public QualityExperiments(Interface datasetInterface, BETable bETable, 
			MysqlConnect database, int trainingSize, int testSize) 
	throws Exception {
		this(datasetInterface, bETable, database, 
				trainingSize, testSize, null, null);
	}

	/**
	 * initializes training and test sets. trains classifier and prints 
	 * statistics to file
	 * @throws Exception 
	 */
	public QualityExperiments(Interface datasetInterface, BETable bETable, 
			MysqlConnect database, int trainingSize, int testSize, 
			String table, String offset) 
	throws Exception{
		super(datasetInterface, bETable, database, 
				trainingSize, testSize, table, offset);
		// save for LearningToRank experiments
		saveDataset(training,"trainingSetWEKA");
		saveDataset(test,"testSetWEKA");

		// coefficients for the model
		System.out.println(((LinearRegression) 
				classifier.getClassifier()).toString());

		// Opening/creating file 
		new File("data/qualityRanking/experiment.txt").delete();
		FileOutputStream file = 
			new FileOutputStream("data/qualityRanking/experiment.txt",
					true);
		DataOutputStream out = new DataOutputStream(file);
		out.writeBytes("File created by QualityExperiments contructor\n");	
		// coefficients for the model
		out.writeBytes(((LinearRegression) 
				classifier.getClassifier()).toString() + "\n");
		out.flush(); 
		out.close();
	}

	/**
	 * Uses logistic regression on features to print some statistics and 
	 * estimate probability of a isbest answer to be classified correctly with 
	 * respect to the other question-answer pairs for the same question
	 * 
	 * @param database
	 * @throws Exception
	 */
	public double testLinearRegression(ResultSet questions) 
	throws Exception{
		// store classification output for debugging purposes
		// outer String is ques_id
		// nested hash table: string in answer_id, vector contains expected
		// and obtained classification
		Hashtable<String, Hashtable<String,Vector<Double>>> classification =
			new Hashtable<String, Hashtable<String,Vector<Double>>>();
		// keep track of classification performance
		double tp = 0.0; // true positive: good!
		double fn = 0.0; // false negative: bad!
		double unknown = 0.0; // false positive or true negative: meaningless.
		for (int i = 0; i < test.numInstances(); i++) {
			Instance instance = test.instance(i);
			// expected classification
			Double label = Double.parseDouble(instance.toString(0));
			// obtained classification
			Double clsLabel = classifier.classifyInstance(instance);
			instance.setClassValue(clsLabel);
			// answer_id
			String answer_id = instance.toString(1);
			// ques_id
			String ques_id = instance.toString(2);
			// storing classification
			Vector<Double> v = new Vector<Double>();
			v.add(label);
			v.add(clsLabel);
			// associating classification with answer_id
			Hashtable<String,Vector<Double>> h = 
				new Hashtable<String, Vector<Double>>();
			h.put(answer_id, v);
			// associating answer_id with ques_id
			Hashtable<String,Vector<Double>> a =
				classification.get(ques_id);
			if (a==null)
				classification.put(ques_id, h);
			else a.put(answer_id, v);
		}
		// grade classification performance
		// for each question
		for (Enumeration<String> k = classification.keys(); 
		k.hasMoreElements();){
			String ques_id = k.nextElement();
			Hashtable<String,Vector<Double>> a = classification.get(ques_id);
			Double isbest_class = 0.0;
			Double max_class = 0.0;
			// for each answer to question get highest classification value
			// and value for isbest answer
			for (Enumeration<String> k2 = a.keys(); k2.hasMoreElements();){
				String answer_id = k2.nextElement();
				Vector<Double> v = a.get(answer_id);
				// save highest classification so far
				if (v.elementAt(1)>max_class) max_class = v.elementAt(1);
				// save classification for isbest
				if(v.elementAt(0)== 1) isbest_class = v.elementAt(1);
			}
//			if (isbest_class==0) unknown++;
//			else{
//				for (Enumeration<String> k2 = a.keys(); k2.hasMoreElements();){
//					String answer_id = k2.nextElement();
//					Vector<Double> v = a.get(answer_id);
//					if(v.elementAt(0)== 1){
//						if(v.elementAt(1)<max_class) fn++;
//						else tp++;
//					}
//					if(v.elementAt(0)==0){
//						if(v.elementAt(1)>isbest_class) fn++;
//						else tp++;
//					}
//				}
//			}
			// give bad grade if isbest answer didn't get highest 
			// classification value
			if (isbest_class==0) unknown++;
			else if (isbest_class == max_class) tp+=a.size();//tp++;
			else fn++;
		}

		// print classification performance
		System.out.println("unknown: " + Math.round(unknown));
		System.out.println("good (true positive): " + Math.round(tp));
		System.out.println("bad (false negative): " + Math.round(fn));
		System.out.println("RECALL: " + Math.round((tp/(tp+fn))*100) + "%");

		// Opening/creating file 
		java.io.FileOutputStream file = 
			new java.io.FileOutputStream("data/qualityRanking/experiment.txt",
					true);
		java.io.DataOutputStream out = new java.io.DataOutputStream(file);
		out.writeBytes("File created by " +
		"QualityExperiments.testLinearRegression()\n");
		// print classification performance
		out.writeBytes("\n");
		out.writeBytes("unknown: " + Math.round(unknown) + "\n");
		out.writeBytes("good (true positive): " + Math.round(tp) + "\n");
		out.writeBytes("bad (false negative): " + Math.round(fn) + "\n");
		out.writeBytes("RECALL: " + Math.round((tp/(tp+fn))*100) + "%\n\n");
		// print test set and its classification
		out.writeBytes(test.toString());
		out.flush(); 
		out.close();
		return Math.round((tp/(tp+fn))*100);
	}

	/**
	 * Uses logistic regression on features to estimate probability of an 
	 * answer to be isbest and prints classification output of a series of 
	 * random question-answer pairs plus some statistics.
	 * This test is not fair for the classifier. Don't expect good results.
	 * 
	 * @param database
	 * @throws Exception
	 */
	public void testClassification(ResultSet questions, MysqlConnect database) 
	throws Exception{
		// store classification output for debugging purposes
		// String contains is answer_id
		// Vector contains expected and obtained classification
		Hashtable<String, Vector<Double>> classification =
			new Hashtable<String, Vector<Double>>();
		// keep track of classification performance
		int good = 0;
		int acceptable = 0;
		int bad = 0;
		for (int i = 0; i < test.numInstances(); i++) {
			Instance instance = test.instance(i);
			// expected classification
			Double label = Double.parseDouble(instance.toString(0));
			// only process isbest instances
			if(label == 1){
				// obtained classification
				Double clsLabel = classifier.classifyInstance(instance);
				Vector<Double> v = new Vector<Double>();
				v.add(label);
				v.add(clsLabel);
				classification.put(instance.toString(1), v);
				instance.setClassValue(clsLabel);
				// grade classification performance
				if (clsLabel > 0.7) good++;
				else if (clsLabel > 0.5) acceptable++;
				// if the result is unsatisfactory investigate why
				else {
					bad++;
					System.out.println("classification: " + clsLabel);
					//retrieve and print the content of the answer
					String q = "SELECT * FROM Answers Where answer_id = " +
					instance.toString(1);
					ResultSet r = database.ask(q);
					r.first();
					System.out.println(r.getString("content"));
				}

			}
		}
		// print test set and its classification
		//System.out.println(test.toString());
		// print classification performance
		System.out.println("good: " + good);
		System.out.println("average: " + acceptable);
		System.out.println("bad: " + bad);
	}

	/**
	 * Uses learning to rank listwise learning to estimate the top 1
	 * probability of a best answer to get the highest score
	 */
	public static double listWiseTest(){
		TrainLinearNeuralModel.train("data/qualityRanking/testSetWEKA.dat");
		return PredictLinearNeuralModel.main(null); 
	}

	/**
	 * saves the dataset to file in both a format appropriate for 
	 * PredictLinearNeuralModel and in Weka format
	 * @param dataset
	 * @throws Exception 
	 */
	private void saveDataset(Instances dataset, String fileName) 
	throws Exception{
		// save in Weka dataset format (arff)
		ArffSaver saver = new ArffSaver();
		saver.setInstances(dataset);
		new File("./data/qualityRanking/" + fileName + ".arff").delete();
		saver.setFile(new File("./data/qualityRanking/" + fileName + ".arff"));
		saver.writeBatch();
		// save in PredictLinearNeuralModel format
		//Remove removeKeys = new Remove();
		//removeKeys.setAttributeIndices("2,3");
		//removeKeys.setInputFormat(dataset);
		//dataset = Filter.useFilter(dataset, removeKeys);
		new File("./data/qualityRanking/" + fileName + ".dat").delete();
		FileWriter fw = new FileWriter("./data/qualityRanking/" + fileName + 
				".dat", true);
		Vector<Instance> answers = new Vector<Instance>();
		String ques_id = null;
		//for all instances in the dataset
		for (int i = 0; i < dataset.numInstances(); i++) {
			Instance instance = dataset.instance(i);
			if(ques_id == null) ques_id = instance.toString(2);
			if(!ques_id.equals(instance.toString(2))){
				// make sure isbest labeling is available
				boolean labelled = false;
				for (Iterator<Instance> it = answers.iterator(); it.hasNext();) {
					Instance answer = it.next();
					if(answer.toString(0).equals("1")) labelled = true;
				}
				if(labelled){
					// the answer is in a new class
					// save all stored answers in the class to file
					fw.write(answers.size() + "\n");
					for (Iterator<Instance> it = answers.iterator(); it.hasNext();) {
						Instance answer = it.next();
						fw.write(answer.toString(0) + "," +
								answer.toString(3) + "," +
								answer.toString(4) + "," +
								answer.toString(5) + "," +
								answer.toString(6) + "," +
								answer.toString(7) + "\n");
					}
				}
				//empty answers vector and update ques_id
				answers.removeAllElements();
				ques_id = instance.toString(2);
			}
			answers.add(instance);
		}
		fw.close(); 		
	}

}
