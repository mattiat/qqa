package qqa.ml;

import qqa.be.BETable;
import qqa.db.dataset.Interface;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Instances;


/**
 * contains the tools to asses quality and create a ranking of answer for
 * summarization
 * @author Mattia Tomasoni
 *
 */
public abstract class Ranker {
	
	/**
	 * dataset (database version)
	 */
	Interface datasetInterface;
	
	/**
	 * dataset (BE version)
	 */
	BETable bETable;

	/**
	 * Weka classifier
	 */
	FilteredClassifier classifier = null;

	/**
	 * options for the Weka classifier
	 */
	String[] options = null;

	/**
	 * training set
	 */
	Instances training = null;

	/**
	 * training set size (number of question-answer pairs)
	 */
	int trainingSize;

	/**
	 * test set
	 */
	Instances test = null;

	/**
	 * test set size
	 */
	int testSize;
	
	/**
	 * constructor initializes datasets and training and test set sizes
	 * @param datasetInterfacePar
	 * @param bETablePar
	 * @param trainingSizePar
	 * @param testSizePar
	 */
	public Ranker(Interface datasetInterfacePar, BETable bETablePar, 
			int trainingSizePar, int testSizePar){
		datasetInterface = datasetInterfacePar;
		bETable = bETablePar;
		classifier = null;
		trainingSize = trainingSizePar;
		testSize = testSizePar;
	}

	abstract public void rank() throws Exception;
}
