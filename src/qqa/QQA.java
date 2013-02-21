/**
 * Qualitative Question Answering
 */
package qqa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import qqa.be.BETable;
import qqa.be.DocumentBEs;
import qqa.be.SentenceBEs;
import qqa.db.MysqlConnect;
import qqa.db.dataset.Interface;
import qqa.ml.CoverageRanker;
import qqa.ml.ILP;
import qqa.ml.QualityExperiments;
import qqa.ml.QualityRanker;
import qqa.ml.Ranker;
import qqa.ml.WeightsCombiner;


/**
 * Qualitative Question and Answering package
 * 
 * @author Mattia Tomasoni
 *
 */
public class QQA {
	/** 
	 *  connection to database
	 */
	private static MysqlConnect database = null;
	
	/**
	 * maximum length of final summary to be outputed
	 */
	static int MaxSummaryLength = 250;

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static String main(String[] args) throws Exception {
		// keep track of execution time
		long start = System.currentTimeMillis();

		// connecting to database
		database = new MysqlConnect();

		// question to be summarized
		String ques_id = args[0];
		String SystemVersion = args[1];

		// retrieve all answers
		String query = "SELECT answer_id, ques_id, content, " +
		"content_unprocessed, user_id, isbest FROM FilteredAnswers WHERE " +
		"ques_id = \'" + ques_id + "\' ORDER BY ques_id";
		ResultSet filteredAnswers = database.ask(query);
		// retrieve all questions
		query = "SELECT ques_id, subject, content, content_unprocessed, " +
		"subject_unprocessed FROM FilteredQuestions WHERE ques_id = " +
		"\'" + ques_id + "' ORDER BY ques_id";
		ResultSet filteredQuestions = database.ask(query);
		// construct interface to the dataset in the database
		Interface filteredInterface = new Interface(filteredAnswers, 
				filteredQuestions, null);
	
		///////////////////////////////////////////////////////////////////////
		// building BE representation
		///////////////////////////////////////////////////////////////////////
		// create a BE table to store BEs
		// builds a BETable object and fills it with the information contained
		// in the files created by extractBEs.
		System.out.println("building BE representation of dataset...");
		BETable bETable = new BETable(filteredInterface);
		bETable.build();
		System.out.println("Successfully retrieved.");

		// creating pointer to question in the BETable
		DocumentBEs question = bETable.table.get(ques_id + ".000");

		// retrieving corresponding answers
		Vector<DocumentBEs> answers = new Vector<DocumentBEs>();
		bETable.answers.beforeFirst();
		while (bETable.answers.next()){
			if(bETable.answers.getString("ques_id").equalsIgnoreCase(ques_id)) {
				String answer_id = bETable.answers.getString("answer_id");
				// creating pointer to answer in the BETable
				answers.add(bETable.table.get(ques_id + "." + answer_id));
			}
		}
		
		// print number of answers to file
		// FileWriter hhw = new FileWriter("data/tmp.txt",true);
		// hhw.write(answers.size()+"\n");
		// hhw.close();
		
		///////////////////////////////////////////////////////////////////////
		// Best Answer SYSTEM ... ?
		///////////////////////////////////////////////////////////////////////
		// if the BA is being used instead of scoring function, then the 
		// summary to be returned in simply the Best Answer
		String BA_id = null;
		if(args[1].equals("BA") ){
			// look for Best Answer among answers in filteredAnswers
			filteredAnswers.beforeFirst();
			while (filteredAnswers.next()) {
				String isbest = filteredAnswers.getString("isbest");
				if(isbest.equals("1")){
					BA_id = filteredAnswers.getString("answer_id");
					break; // no need to process the rest of the answers
				}
			}
			// if no Best Answer is available for the currently selected 
			// question, just return the first answer available. This simulates
			// the behavior of a user that would read the first answer she or he
			// finds scrolling down the page
			if(BA_id == null) {
				filteredAnswers.absolute(1);
				BA_id = filteredAnswers.getString("answer_id");
			}
			
			//get a pointer to Best Answer
			DocumentBEs BA = null;
			for (DocumentBEs answer : answers)
				if(answer.doc_id.contains(BA_id)) BA = answer;
			
			// get question content
			String questionContent = "";
			for(SentenceBEs sentence : question.sentences)
				questionContent += sentence.content + " ";
			// update groups.txt file with ques_id and content
			// this is a loop, do not delete previously existing copies of 
			// this file! it is done in E_AnswerQuestions already!
			FileWriter gw = 
				new FileWriter("data/annotation/groups.txt",true);
			gw.write(ques_id + "\t" + questionContent + "\n");
			gw.close();
			// create directory named after ques_id
			new File("data/annotation/" + ques_id).mkdir();
			
			// create summary.txt file: contain sentence id and sentence
			// for every sentence in the summary
			new File("data/annotation/" + ques_id + "/summary.txt").delete();
			FileWriter su = new FileWriter("data/annotation/" + ques_id + 
					"/summary.txt",true);
			// total number of sentences selected in the summary
			int tot = BA.sentences.size();			
			//create summary string; the summary will be the Best Answer itself
			su.write(tot + "\n");
			String summary = "***SUMMARIZED ANSWER***\n";
			for(SentenceBEs sentence : BA.sentences){
				// store summary to String. To be returned by method
				summary += sentence.content;
				// store sentence_id to file
				su.write(sentence.sentence_id + '\n');
			}
			su.close();
			
			// create rouge file: contains simply the sentences, one per line
			int lengthSoFar = 0;
			new File("../ROUGE-1.5.5/peers/" + ques_id + ".000.spl").delete();
			FileWriter rw = new FileWriter("../ROUGE-1.5.5/peers/" + 
					ques_id + ".000.spl",true);
			for(SentenceBEs sentence : BA.sentences){
				lengthSoFar += sentence.getNumWords();
				// stop when MaxSummaryLength has been reached
				if(lengthSoFar >= MaxSummaryLength) break;
				// append one sentence per line
				rw.write(sentence.content + '\n');
			}
			rw.close();
			
			// return Best Answer as final summary
			return summary;
		}

		///////////////////////////////////////////////////////////////////////
		// assessing quality
		///////////////////////////////////////////////////////////////////////
		System.out.println("Assessing quality...");
		// number of features
		query = "SELECT count(*) FROM QualityFeatures";
		ResultSet r = database.ask(query); r.first();
		int featureNumber = Integer.parseInt(r.getString("count(*)"));
		// number of answers
		query = "SELECT count(*) FROM FilteredAnswers";
		r = database.ask(query); r.first();
		int answerNumber = Integer.parseInt(r.getString("count(*)"));
		// the answers will be the test set
		int testSize = answerNumber;
		// the remaining features will serve as training set
		int trainSize = featureNumber-answerNumber;
		Ranker testRank = new QualityExperiments(filteredInterface, 
				bETable, database, trainSize, testSize);
		//((QualityExperiments) testRank).testLinearRegression(filteredQuestions);

		// assess quality of each answer and stores it in the BETable
		Ranker qualityRank = (QualityRanker) testRank;
		qualityRank.rank();
		System.out.println("...DONE");

		///////////////////////////////////////////////////////////////////////
		// assessing coverage
		///////////////////////////////////////////////////////////////////////
		System.out.println("Assessing coverage...");
		// assess coverage of each answer and stores it in the BETable
		Ranker coverageRank = 
			new CoverageRanker(filteredInterface, bETable, database);
		coverageRank.rank();
		System.out.println("...DONE");

		///////////////////////////////////////////////////////////////////////
		// Sentence selection
		///////////////////////////////////////////////////////////////////////
		// calculating BE weights
		// calculate max quality and min quality of the answers to the question
		Double minQuality = 1.0; // 1.0 is the higher quality possible
		Double maxQuality = 0.0; // 0.0 is the lower quality possible
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			Double quality = a.next().docQuality;
			if(quality != null){
				if(quality < minQuality) minQuality = quality;
				if(quality > maxQuality) maxQuality = quality;
			} else return "Not in the FilteredQuestions table. " +
			"No Quality estimate!";
		}

		// for each answer
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			DocumentBEs answer = a.next();
			// for each sentence
			for (Iterator<SentenceBEs> s = answer.sentences.iterator(); 
			s.hasNext();) {
				SentenceBEs sentence = s.next();
				// for each BE
				for (Enumeration<String> e = sentence.eqClasses.keys(); 
				e.hasMoreElements() ;) {
					String be = e.nextElement();
					// QUALITY
					Double quality = 
						assessQuality(answer, minQuality, maxQuality);
					// COVERAGE
					Double coverage = 0.001 +
					assessCoverage(answer);
					// if the API in assessCoverage retrieves an empty 
					// ResultSet, coverage will be NaN
					//					if(Double.isNaN(coverage)) coverage = 0.0;
					// RELEVANCE
					Double relevance = 0.001 +
					assessRelevance(be, sentence, question);
					// NOVELTY
					Double novelty = 0.001 +
					assessNovelty(be, sentence, answer, answers);
					// compute BE weight
					Double weight = computeTermWeight(
							quality, 
							coverage,
							relevance, 
							novelty, 
							sentence.getNumWords(),
							SystemVersion);
					// store weight
					sentence.weights.put(be, weight);
				}
				// calculate the average weight of the Bes in the sentence
				sentence.calculateAvgBeWeight();
			}
		}

		// solving ILP problem
		// compute length limit for the answer
		int SummaryLength = 0;

		// summary length proportional to answer quality
		//		for (DocumentBEs answer : answers) {
		//			// proportional to answer quality
		//			SummaryLength += answer.getNumWords() * 
		//			assessQuality(answer, minQuality, maxQuality);
		//		}
		//		// limit to 800 words
		//		if(SummaryLength > 800) SummaryLength = 800;

		// summary length proportional to best answer length
		Double bestQualitySofar = 0.0;
		for (DocumentBEs answer : answers) {
			Double currentQuality = assessQuality(answer, minQuality, maxQuality);
			if(currentQuality > bestQualitySofar){
				bestQualitySofar = currentQuality;
				// proportional to double best answer quality
				SummaryLength = answer.getNumWords() * 3;
			}
		}
		// limit to 250 words
		if(SummaryLength > MaxSummaryLength) SummaryLength = MaxSummaryLength;



		ILP sentenceSelector = new ILP(SummaryLength);
		// solve ILP problem
		String summary = sentenceSelector.solveBE(answers, question);
		System.out.print(summary);

		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");

		return summary;
	}






	/**
	 * returns the quality of a be, based on the quality of its answer and the
	 * quality of the best worst answer to the same question
	 * @param answer
	 * @param minQuality
	 * @param maxQuality
	 * @return
	 */
	private static Double assessQuality(DocumentBEs answer, Double minQuality,
			Double maxQuality) {
		//		if(minQuality != maxQuality)
		//			return (answer.docQuality-minQuality) / (maxQuality-minQuality);
		//		else 
		//			// with only one answer quality is of no use in the choice of what
		//			// content to summarize (there is no choice!)
		//			return 0.5;
		return answer.docQuality;
	}


	/**
	 * returns the coverage of a be, based on the coverage of its answer
	 * @param answer
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private static Double assessCoverage(DocumentBEs answer) 
	throws IOException, ParserConfigurationException, SAXException {
		return answer.docCoverage;
	}


	/**
	 * returns the relevance of a be to its question
	 * @param be
	 * @param question
	 */
	private static Double assessRelevance(String be, SentenceBEs sBE, 
			DocumentBEs question){
		Double relSentences = 0.0;
		// for each sentence in the question
		for (Iterator<SentenceBEs> s = question.sentences.iterator(); 
		s.hasNext();) {
			SentenceBEs sentence = s.next();
			// for all the elements in the equivalence class of the be 
			Set<String> eqClass = sBE.eqClasses.get(be);
			for (String eqBE : eqClass) {
				// if the sentence contains the element
				if(sentence.eqClassesSet.contains(eqBE)) {
					// update number of relevant sentences
					relSentences++;
					// jump to next sentence
					break;
				}
			}
		}
		if(relSentences != 0)
			return relSentences/question.sentences.size();
		else return 0.0;
	}

	/**
	 * returns the novelty of the be, that is the percentage of sentences
	 * among all sentences in all answers to the questions (except the sentence
	 * itself) that do not contain the be. This value is logarithmically 
	 * weighted to avoid irrelevant, very rare words.
	 * @param be
	 * @param question
	 * @param answer
	 * @return
	 */
	private static Double assessNovelty(String be, SentenceBEs sBE, 
			DocumentBEs aBE, Vector<DocumentBEs> answers){
		// number of sentences containing the be
		Double relSentences = 0.0;
		// total number of sentences
		Double totSentences = 0.0;
		// for each answer (but the one the BE comes from)
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			DocumentBEs answer = a.next();
			if(!answer.equals(aBE)){
				// for each sentence
				for (Iterator<SentenceBEs> s = answer.sentences.iterator(); 
				s.hasNext();) {
					totSentences++;
					SentenceBEs sentence = s.next();
					// for all the elements in the equivalence class of the be 
					Set<String> eqClass = sBE.eqClasses.get(be);
					for (String eqBE : eqClass) {
						// if the sentence contains the element
						if(sentence.eqClassesSet.contains(eqBE)) {
							// update number of relevant sentences
							relSentences++;
							// jump to next sentence
							break;
						}
					}
				}
			}
		}
		// calculate Novelty of the term
		Double n = 1 - (relSentences/totSentences);
		if(n!=0)
			return n*(Math.log(1/n)/Math.log(2));
		else 
			return 0.0;
	}

	/**
	 * combines relevance, coverage and quality of a be to compute its weight.
	 * Penalizes bes coming from short sentences .
	 * @param quality
	 * @param relevance
	 * @param coverage
	 * @param answerLength
	 * @return
	 */
	private static Double computeTermWeight(Double quality, Double coverage,
			Double relevance, Double novelty, int sentenceLength, 
			String SystemVersion) {
		Double weight = 1.0;
		// combine relevance and coverage
		//		if(quality!=0.0) weight= weight*quality;
		//		if(coverage!=0.0) weight= weight*coverage;
		//		if(relevance!=0.0) weight= weight*relevance;
		//		if(novelty!=0.0) weight= weight*novelty;
		//		if (quality==0.0 || coverage==0.0 || relevance==0.0 || novelty==0.0)
		//			weight= weight*0.001;
		// combine scores according to the kind of scoring function required
		if(SystemVersion.equals("R")) 
			weight = relevance;
		if(SystemVersion.equals("Q")) 
			weight = quality;
		if(SystemVersion.equals("RQ")) 
			weight = relevance*quality;
		if(SystemVersion.equals("RC")) 
			weight = relevance*coverage;
		if(SystemVersion.equals("RN")) 
			weight = relevance*novelty;
		if(SystemVersion.equals("RQC")) 
			weight = relevance*quality*coverage;
		if(SystemVersion.equals("RQN")) 
			weight = relevance*quality*novelty;
		if(SystemVersion.equals("RCN")) 
			weight = relevance*coverage*novelty;
		if(SystemVersion.equals("RQCN")) 
			weight = relevance*quality*coverage*novelty;
		// penalize short sentences (shorter then the base of the logarithm)
		weight = (Math.log(sentenceLength)/Math.log(20)) * weight;
		return weight;
	}

	/**
	 * This method is a clone of the main method that computes quality, coverage,
	 * relevance and novelty for each concept, but instead of using them to 
	 * generate a summary, it stores them in the database. They will be used in
	 * step F_Learn_Weights
	 * @throws Exception 
	 */
	public static void storeWeights(String[] args) throws Exception{
		// keep track of execution time
		long start = System.currentTimeMillis();

		// connecting to database
		database = new MysqlConnect();

		// question to be summarized
		String ques_id = args[0];

		// retrieve all answers
		String query = "SELECT answer_id, ques_id, content, " +
		"content_unprocessed, user_id, isbest FROM FilteredAnswers WHERE " +
		"ques_id = \'" + ques_id + "\' ORDER BY ques_id";
		ResultSet filteredAnswers = database.ask(query);
		// retrieve all questions
		query = "SELECT ques_id, subject, content, content_unprocessed, " +
		"subject_unprocessed FROM FilteredQuestions WHERE ques_id = " +
		"\'" + ques_id + "' ORDER BY ques_id";
		ResultSet filteredQuestions = database.ask(query);
		// construct interface to the dataset in the database
		Interface filteredInterface = new Interface(filteredAnswers, 
				filteredQuestions, null);

		///////////////////////////////////////////////////////////////////////
		// building BE representation
		///////////////////////////////////////////////////////////////////////
		// create a BE table to store BEs
		// builds a BETable object and fills it with the information contained
		// in the files created by extractBEs.
		System.out.println("building BE representation of dataset...");
		BETable bETable = new BETable(filteredInterface);
		bETable.build();
		System.out.println("Successfully retrieved.");

		// creating pointer to question in the BETable
		DocumentBEs question = bETable.table.get(ques_id + ".000");

		// retrieving corresponding answers
		Vector<DocumentBEs> answers = new Vector<DocumentBEs>();
		bETable.answers.beforeFirst();
		while (bETable.answers.next()){
			if(bETable.answers.getString("ques_id").equalsIgnoreCase(ques_id)) {
				String answer_id = bETable.answers.getString("answer_id");
				// creating pointer to answer in the BETable
				answers.add(bETable.table.get(ques_id + "." + answer_id));
			}
		}

		///////////////////////////////////////////////////////////////////////
		// assessing quality
		///////////////////////////////////////////////////////////////////////
		System.out.println("Assessing quality...");
		// number of features
		query = "SELECT count(*) FROM QualityFeatures";
		ResultSet r = database.ask(query); r.first();
		int featureNumber = Integer.parseInt(r.getString("count(*)"));
		// number of answers
		query = "SELECT count(*) FROM FilteredAnswers";
		r = database.ask(query); r.first();
		int answerNumber = Integer.parseInt(r.getString("count(*)"));
		// the answers will be the test set
		int testSize = answerNumber;
		// the remaining features will serve as training set
		int trainSize = featureNumber-answerNumber;
		Ranker testRank = new QualityExperiments(filteredInterface, 
				bETable, database, trainSize, testSize);
		((QualityExperiments) testRank). 
		testLinearRegression(filteredQuestions);

		// assess quality of each answer and stores it in the BETable
		Ranker qualityRank = (QualityRanker) testRank;
		qualityRank.rank();
		System.out.println("...DONE");

		///////////////////////////////////////////////////////////////////////
		// assessing coverage
		///////////////////////////////////////////////////////////////////////
		System.out.println("Assessing coverage...");
		// assess coverage of each answer and stores it in the BETable
		Ranker coverageRank = 
			new CoverageRanker(filteredInterface, bETable, database);
		coverageRank.rank();
		System.out.println("...DONE");

		///////////////////////////////////////////////////////////////////////
		// Sentence selection
		///////////////////////////////////////////////////////////////////////
		// calculating BE weights
		// calculate max quality and min quality of the answers to the question
		Double minQuality = 1.0; // 1.0 is the higher quality possible
		Double maxQuality = 0.0; // 0.0 is the lower quality possible
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			Double quality = a.next().docQuality;
			if(quality != null){
				if(quality < minQuality) minQuality = quality;
				if(quality > maxQuality) maxQuality = quality;
			} else System.out.println("question with on ly one answer!!");//throw new Exception();
		}

		// for each answer
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			DocumentBEs answer = a.next();
			// for each sentence
			for (Iterator<SentenceBEs> s = answer.sentences.iterator(); 
			s.hasNext();) {
				SentenceBEs sentence = s.next();
				// for each BE
				for (Enumeration<String> e = sentence.eqClasses.keys(); 
				e.hasMoreElements() ;) {
					String be = e.nextElement();
					// QUALITY
					Double quality = 
						assessQuality(answer, minQuality, maxQuality);
					// COVERAGE
					Double coverage = 0.001 +
					assessCoverage(answer);
					// if the API in assessCoverage retrieves an empty 
					// ResultSet, coverage will be NaN
					//					if(Double.isNaN(coverage)) coverage = 0.0;
					// RELEVANCE
					Double relevance = 0.001 +
					assessRelevance(be, sentence, question);
					// NOVELTY
					Double novelty = 0.001 +
					assessNovelty(be, sentence, answer, answers);
					// store weights to database
					query = "INSERT Weights " +
					"VALUES(" + 
					0 + "," +
					"\'" + sentence.sentence_id + "\'," +
					"\'" + ques_id + "\'," +
					quality + "," +
					coverage + "," +
					relevance + "," +
					novelty + "," +
					sentence.getNumWords() +
					")";
					database.execute(query);
				}


			}
		}
		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");
	}

	/**
	 * stores human summary supervision for each concept: this corresponds to whether
	 * the sentence was selected, weighted by the human's vote of the summary 
	 * @throws SQLException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @throws IOException 
	 */
	static void storeWeightSupervision(String[] args) throws InstantiationException, 
	IllegalAccessException, ClassNotFoundException, SQLException, IOException{
		// connecting to database
		database = new MysqlConnect();

		// question id
		String ques_id = args[0];
		System.out.println("storing weight supervision for " + ques_id + "...");

		// filter names of supervision files
		File dir = new File("data/annotation_result/" + ques_id);
		FilenameFilter select = new FilenameFilter() {
			public boolean accept(File dir, String name) {
				if(name.contains("summary") && !name.contains("auto")) return true;
				else return false;
			}
		};
		File[] supervisors = dir.listFiles(select);
		for (int i = 0; i < supervisors.length; i++) {
			System.out.println(supervisors[i].getName());
			// open supervision files
			String fileName = supervisors[i].getAbsolutePath();
			BufferedReader input = new BufferedReader(new FileReader(fileName));
			// throw away first line (only contains number of lines in the file)
			String line = input.readLine();
			while (( line = input.readLine()) != null){
				String query = "UPDATE Weights SET isselected = isselected+" +
				// if all supervisors select a sentence, then the isselected
				// value of its concepts will be 1
				(float) 1/supervisors.length +
				" WHERE sentence_id=\'" + line + "\'";
				database.execute(query);
			}
		}
		System.out.println("...done");
	}




	/**
	 * This method is a clone of the main method that computes quality, coverage,
	 * relevance and novelty for each concept, but instead of multiplying them it uses
	 * ML learning techniques to combine them. This will be used in step 
	 * G_Answer_Questions_ML
	 * @throws Exception 
	 */
	public static String mainML(String[] args) throws Exception {
		// keep track of execution time
		long start = System.currentTimeMillis();

		// connecting to database
		database = new MysqlConnect();

		// question to be summarized
		String ques_id = args[0];
		String SystemVersion = args[1];

		// retrieve all answers
		String query = "SELECT answer_id, ques_id, content, " +
		"content_unprocessed, user_id, isbest FROM FilteredAnswers WHERE " +
		"ques_id = \'" + ques_id + "\' ORDER BY ques_id";
		ResultSet filteredAnswers = database.ask(query);
		// retrieve all questions
		query = "SELECT ques_id, subject, content, content_unprocessed, " +
		"subject_unprocessed FROM FilteredQuestions WHERE ques_id = " +
		"\'" + ques_id + "' ORDER BY ques_id";
		ResultSet filteredQuestions = database.ask(query);
		// construct interface to the dataset in the database
		Interface filteredInterface = new Interface(filteredAnswers, 
				filteredQuestions, null);

		///////////////////////////////////////////////////////////////////////
		// building BE representation
		///////////////////////////////////////////////////////////////////////
		// create a BE table to store BEs
		// builds a BETable object and fills it with the information contained
		// in the files created by extractBEs.
		System.out.println("building BE representation of dataset...");
		BETable bETable = new BETable(filteredInterface);
		bETable.build();
		System.out.println("Successfully retrieved.");

		// creating pointer to question in the BETable
		DocumentBEs question = bETable.table.get(ques_id + ".000");

		// retrieving corresponding answers
		Vector<DocumentBEs> answers = new Vector<DocumentBEs>();
		bETable.answers.beforeFirst();
		while (bETable.answers.next()){
			if(bETable.answers.getString("ques_id").equalsIgnoreCase(ques_id)) {
				String answer_id = bETable.answers.getString("answer_id");
				// creating pointer to answer in the BETable
				answers.add(bETable.table.get(ques_id + "." + answer_id));
			}
		}

		///////////////////////////////////////////////////////////////////////
		// assessing quality
		///////////////////////////////////////////////////////////////////////
		System.out.println("Assessing quality...");
		// number of features
		query = "SELECT count(*) FROM QualityFeatures";
		ResultSet r = database.ask(query); r.first();
		int featureNumber = Integer.parseInt(r.getString("count(*)"));
		// number of answers
		query = "SELECT count(*) FROM FilteredAnswers";
		r = database.ask(query); r.first();
		int answerNumber = Integer.parseInt(r.getString("count(*)"));
		// the answers will be the test set
		int testSize = answerNumber;
		// the remaining features will serve as training set
		int trainSize = featureNumber-answerNumber;
		Ranker testRank = new QualityExperiments(filteredInterface, 
				bETable, database, trainSize, testSize);
		((QualityExperiments) testRank). 
		testLinearRegression(filteredQuestions);

		// assess quality of each answer and stores it in the BETable
		Ranker qualityRank = (QualityRanker) testRank;
		qualityRank.rank();
		System.out.println("...DONE");
		

		///////////////////////////////////////////////////////////////////////
		// assessing coverage
		///////////////////////////////////////////////////////////////////////
		System.out.println("Assessing coverage...");
		// assess coverage of each answer and stores it in the BETable
		Ranker coverageRank = 
			new CoverageRanker(filteredInterface, bETable, database);
		coverageRank.rank();
		System.out.println("...DONE");

		///////////////////////////////////////////////////////////////////////
		// Sentence selection
		///////////////////////////////////////////////////////////////////////
		// calculating BE weights
		// calculate max quality and min quality of the answers to the question
		Double minQuality = 1.0; // 1.0 is the higher quality possible
		Double maxQuality = 0.0; // 0.0 is the lower quality possible
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			Double quality = a.next().docQuality;
			if(quality != null){
				if(quality < minQuality) minQuality = quality;
				if(quality > maxQuality) maxQuality = quality;
			} else return "Not in the FilteredQuestions table. " +
			"No Quality estimate!";
		}

		// create WeightCombiner
		WeightsCombiner combiner = new WeightsCombiner(database);

		// for each answer
		for (Iterator<DocumentBEs> a = answers.iterator(); a.hasNext();) {
			DocumentBEs answer = a.next();
			// for each sentence
			for (Iterator<SentenceBEs> s = answer.sentences.iterator(); 
			s.hasNext();) {
				SentenceBEs sentence = s.next();
				// for each BE
				for (Enumeration<String> e = sentence.eqClasses.keys(); 
				e.hasMoreElements() ;) {
					String be = e.nextElement();
					// QUALITY
					Double quality = 
						assessQuality(answer, minQuality, maxQuality);
					// COVERAGE
					Double coverage = 0.001 +
					assessCoverage(answer);
					// if the API in assessCoverage retrieves an empty 
					// ResultSet, coverage will be NaN
					//					if(Double.isNaN(coverage)) coverage = 0.0;
					// RELEVANCE
					Double relevance = 0.001 +
					assessRelevance(be, sentence, question);
					// NOVELTY
					Double novelty = 0.001 +
					assessNovelty(be, sentence, answer, answers);
					// compute BE weight
					Double weight = computeTermWeightML(
							quality, 
							coverage,
							relevance, 
							novelty, 
							sentence.getNumWords(),
							sentence.sentence_id,
							ques_id,
							combiner);
					// store weight
					sentence.weights.put(be, weight);
				}
				// calculate the average weight of the Bes in the sentence
				sentence.calculateAvgBeWeight();
			}
		}

		// solving ILP problem
		// compute length limit for the answer
		int SummaryLength = 0;

		// summary length proportional to answer quality
		//		for (DocumentBEs answer : answers) {
		//			// proportional to answer quality
		//			SummaryLength += answer.getNumWords() * 
		//			assessQuality(answer, minQuality, maxQuality);
		//		}
		//		// limit to 800 words
		//		if(SummaryLength > 800) SummaryLength = 800;

		// summary length proportional to best answer length
		Double bestQualitySofar = 0.0;
		for (DocumentBEs answer : answers) {
			Double currentQuality = assessQuality(answer, minQuality, maxQuality);
			if(currentQuality > bestQualitySofar){
				bestQualitySofar = currentQuality;
				// proportional to double best answer quality
				SummaryLength = answer.getNumWords() * 3;
			}
		}
		// limit to 250 words
		if(SummaryLength > 250) SummaryLength = 250;



		ILP sentenceSelector = new ILP(SummaryLength);
		// solve ILP problem
		String summary = sentenceSelector.solveBE(answers, question);
		System.out.print(summary);

		// disconnecting from database 
		database.disconnect();
		System.out.println("...FINISHED...");
		float elapsedTime = (System.currentTimeMillis()-start)/60000F;
		System.out.println("elapsed time " + elapsedTime + " min");

		return summary;
	}


	/**
	 * uses ML techniques to combines relevance, coverage and quality of a be to 
	 * compute its weight.
	 * @param quality
	 * @param relevance
	 * @param coverage
	 * @param answerLength
	 * @return
	 * @throws Exception 
	 */
	private static Double computeTermWeightML(Double quality, Double coverage,
			Double relevance, Double novelty, int sentenceLength, 
			String sentence_id, String ques_id, WeightsCombiner combiner) 
	throws Exception {
		Double[] weights = {quality,coverage,relevance,novelty, 
				new Double(sentenceLength)};
		Double weight = combiner.combine(sentence_id, ques_id, weights);
		return weight;
	}



}
