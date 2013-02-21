package qqa.ml;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import qqa.db.MysqlConnect;
import weka.classifiers.Classifier;
import weka.classifiers.functions.LinearRegression;
import weka.classifiers.functions.Logistic;
import weka.classifiers.meta.FilteredClassifier;
import weka.filters.unsupervised.attribute.MakeIndicator;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.Normalize;

public class WeightsCombiner {
	FilteredClassifier classifier;
	Filter norm;
	Instances dataset;
	
	/**
	 * creates and trains a classifier to combine weights based on dataset 
	 * contained in the Weights table
	 * @param database
	 * @throws Exception
	 */
	public WeightsCombiner(MysqlConnect database) throws Exception{
		// retrieving the dataset from the database
		String query = "SELECT * FROM Weights";
		// create WEKA dataset
		dataset = database.retrieveWEKAInstances(query);
		// set supervision index
		dataset.setClassIndex(0);
		// preprocessing: normalize values
		norm = new Normalize();
		norm.setInputFormat(dataset);
		dataset = Filter.useFilter(dataset, norm);
		// build Linear Regression classifier
		Classifier cModel = (Classifier)new LinearRegression();
		// classifier options: Set the attibute selection method.
		String[] options = new String[2];
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
		classifier.buildClassifier(dataset);
	}
	
	/**
	 * combines the weights of a new concept based
	 * @return
	 * @throws Exception 
	 */
	public Double combine(String sentence_id, String ques_id, Double [] weights) 
	throws Exception{
		//declare attributes
		Attribute label = new Attribute("isselected");
		FastVector s_vector = new FastVector(1);
		s_vector.addElement(sentence_id);
		Attribute s_id = new Attribute("sentence_id", s_vector);
		FastVector q_vector = new FastVector(1);
		q_vector.addElement(ques_id);
		Attribute q_id = new Attribute("ques_id", q_vector);
		Attribute q = new Attribute("quality");
		Attribute c = new Attribute("coverage");
		Attribute r = new Attribute("relevance");
		Attribute n = new Attribute("novelty");
		Attribute s = new Attribute("sentence_length");
		// merge attributes
		FastVector fvWekaAttributes = new FastVector(8);
		fvWekaAttributes.addElement(label);
		fvWekaAttributes.addElement(s_id);
		fvWekaAttributes.addElement(q_id);
		fvWekaAttributes.addElement(q);   
		fvWekaAttributes.addElement(c);
		fvWekaAttributes.addElement(r);
		fvWekaAttributes.addElement(n);
		fvWekaAttributes.addElement(s);
		Instances isTrainingSet = new Instances("Rel", fvWekaAttributes, 1);
		isTrainingSet.setClassIndex(0);
		//create new instance
		Instance instance = new Instance(8);
		isTrainingSet.add(instance);
		instance.setDataset(isTrainingSet);
		instance.setValue((Attribute)fvWekaAttributes.elementAt(0), -1.0);
		instance.setValue((Attribute)fvWekaAttributes.elementAt(1), sentence_id);
		instance.setValue((Attribute)fvWekaAttributes.elementAt(2), ques_id);
		instance.setValue((Attribute)fvWekaAttributes.elementAt(3), weights[0]);
		instance.setValue((Attribute)fvWekaAttributes.elementAt(4), weights[1]);
		instance.setValue((Attribute)fvWekaAttributes.elementAt(5), weights[2]);
		instance.setValue((Attribute)fvWekaAttributes.elementAt(6), weights[3]);
		instance.setValue((Attribute)fvWekaAttributes.elementAt(7), weights[4]);
		//classify new instance
		norm.setInputFormat(isTrainingSet);
		isTrainingSet = Filter.useFilter(isTrainingSet, norm);
		Double clsLabel = classifier.classifyInstance(isTrainingSet.firstInstance());
		return clsLabel;
	}
}
