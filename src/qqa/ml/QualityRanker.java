package qqa.ml;

import qqa.be.BETable;
import qqa.db.MysqlConnect;
import qqa.db.dataset.Interface;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.Normalize;

/**
 * Contains the tools to rank answers according to their quality.
 * @author Mattia Tomasoni
 *
 */
public class QualityRanker extends Ranker{
	
	/**
	 * constructor initializes datasets, training set and test set to assess
	 * quality
	 * @param datasetInterfacePar
	 * @param bETablePar
	 * @param trainingSizePar
	 * @param testSizePar
	 * @throws Exception 
	 */
	public QualityRanker(Interface datasetInterfacePar, BETable bETablePar, 
			MysqlConnect database, int trainingSizePar, int testSizePar, 
			String table, String offset) 
	throws Exception{
		super (datasetInterfacePar, bETablePar, trainingSizePar, testSizePar);
		// retrieving the dataset from the database
		if(table==null) table = "QualityFeatures";
		if(offset==null) offset = "0";
		String query = "SELECT * FROM "+ table +" ORDER BY ques_id LIMIT " + 
			offset + "," + (trainingSize + testSize);
		// create WEKA dataset
		Instances dataset = database.retrieveWEKAInstances(query);
		// set supervision index
		dataset.setClassIndex(0);
		// preprocessing: normalize values
		Filter n = new Normalize();
		n.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, n);
		// create training set and test set from dataset
		training = new Instances(dataset);
		test = new Instances(dataset);
		for (int i = 0; i < testSize; i++) training.delete(0);
		for (int i = 0; i < trainingSize; i++) test.delete(testSize);
		
		// build Linear Regression classifier
		Classifier cModel = (Classifier)new LinearRegression();
		// classifier options: Set the attibute selection method.
		options = new String[2];
		options[0] = "-S";
		options[1] = "1"; // 1 = None, 2 = Greedy (default 0 = M5' method) 
		cModel.setOptions(options);
		// build meta-classifier for filtered data
		classifier = new FilteredClassifier();
		// prevent answer and question id from being used as features
		Remove f = new Remove();
		f.setAttributeIndices("2,3"); //attributes are indexed from 1
		classifier.setFilter(f);
		classifier.setClassifier(cModel);
		// train
		classifier.buildClassifier(training);
	}
	
	/**
	 * computes quality values on test set and stores them in the BETable
	 */
	public void rank() throws Exception{
		for (int i = 0; i < test.numInstances(); i++) {
			Instance instance = test.instance(i);
			// obtained classification
			Double clsLabel = classifier.classifyInstance(instance);
			// answer_id
			String answer_id = instance.toString(1);
			// ques_id
			String ques_id = instance.toString(2);
			// storing classification in BETable if it is contained in the 
			// Interface to the database (otherwise there will be no entry in
			// the bETable)
			datasetInterface.questions.beforeFirst();
			while(datasetInterface.questions.next()){
				String id = datasetInterface.questions.getString("ques_id");
				// if corresponding question had been found
				if(id.equalsIgnoreCase(ques_id)){
					// update corresponding bETable entry
					bETable.table.get(ques_id + "." + answer_id).docQuality = 
						clsLabel;
					// no need to look at the other questions
					break;
				}
			}
		}
	}

}
