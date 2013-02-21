package qqa.ml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import qqa.be.BETable;
import qqa.db.MysqlConnect;
import qqa.db.dataset.Interface;

/**
 * Contains the tools to rank sentences according to their relevance to a query.
 * Used to generate a question-biased summary of the answers to a specific 
 * question
 * @author Mattia Tomasoni
 *
 */
public class CoverageRanker extends Ranker{
	/**
	 * Vector containing for the current answer the set of words of each 
	 * similar answer retrieved through the API
	 */
	Vector<HashSet<String>> similarAnswers;

	/**
	 * Hashtable containing for the current answer the term frequency of each
	 * non-stopword term
	 */
	Hashtable<String,Integer> termFrequency;

	/**
	 * calls constructor superclass, initializes similarAnswers and 
	 * termFrequency structures and build and trains the classifier
	 * @param datasetInterfacePar
	 * @param bETablePar
	 * @param trainingSizePar
	 * @param testSizePar
	 * @throws Exception 
	 */
	public CoverageRanker(Interface datasetInterface, BETable bETable, 
			MysqlConnect database, int trainingSize, int testSize) 
	throws Exception{
		super(datasetInterface, bETable, trainingSize, testSize);
		similarAnswers = new Vector<HashSet<String>>();
		termFrequency = new Hashtable<String, Integer>();
		//classifier = new FilteredClassifier();
	}

	/**
	 * called when the ranker is not going to be implemented by a classifier.
	 * Simply sets trainingSize and testSize to 0
	 * @param datasetInterface
	 * @param bETable
	 * @param database
	 */
	public CoverageRanker(Interface datasetInterface, BETable bETable, 
			MysqlConnect database){
		super(datasetInterface, bETable, 0, 0);
		similarAnswers = new Vector<HashSet<String>>();
		termFrequency = new Hashtable<String, Integer>();
	}

	/**
	 * computes coverage values on test set and stores them in the BETable with 
	 * the method explained in Essential Pages.
	 * Uses answers to similar questions retrieved via Yahoo! API as body of 
	 * knowledge
	 * @throws SQLException 
	 * @throws SAXException 
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 */
	public void rank() throws SQLException, IOException, 
	ParserConfigurationException, SAXException{
		// storing classification in BETable if it is contained in the 
		// Interface to the database (otherwise there will be no entry in
		// the bETable)
		datasetInterface.questions.beforeFirst();
		//for all questions
		while(datasetInterface.questions.next()){
			String ques_id = 
				datasetInterface.questions.getString("ques_id");
			String ques_subject = 
				datasetInterface.questions.getString("subject_unprocessed");
			int ques_length = ques_subject.split(" ").length;
			// if subject is too short, append question content
			if(ques_length <= 3) ques_subject +=
				datasetInterface.questions.getString("content_unprocessed");
			datasetInterface.answers.beforeFirst();
			// get similar answers via Yahoo API
			getSimilarAnswers(ques_subject);
			// for all corresponding answers
			while(datasetInterface.answers.next()){
				String id = datasetInterface.answers.getString("ques_id");				
				// if corresponding question had been found
				if(id.equalsIgnoreCase(ques_id)){
					String answer_id = datasetInterface.answers.
					getString("answer_id");
					String answer_content = datasetInterface.answers.
					getString("content_unprocessed");
					// remove stopwords
					answer_content = 
						Preprocessor.removeStopwords(answer_content);
					// for each word in the answer compute term frequency
					computeTermFrequency(answer_content);
					// compute coverage
					Double cvgLabel = 
						computeCoverageScore(answer_content)/
						answer_content.split(" ").length;
					// update corresponding bETable entry
					bETable.table.get(ques_id + "." + answer_id).
					docCoverage = cvgLabel;
				}
			}
		}
	}

	/**
	 * Temporarily populates the similarAnswers Vector.
	 * Uses the Yahoo! API to retrieve a set of similar answers, saves them to
	 * an xml file then parses its content and adds it (in the form of a set of
	 * words) in the similarAnswers Vector
	 * @param ques_content
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	private void getSimilarAnswers(String ques_content) throws 
	IOException, ParserConfigurationException, SAXException{
		similarAnswers = new Vector<HashSet<String>>();
		// create request String for similar questions
		String query = ques_content.replace(" ", "+");
		String request = 
			"http://answers.yahooapis.com/AnswersService/V1/" +
			"questionSearch?" +
			"appid=9hvrzMnV34GYkuIVZAaU9UnKalPvHbyKpPaXqYiXtiFX2RuzTKJcpyASebBlPaEpRw8qF0M-" +
			"&query=" + query +
			"&type=resolved" +
			"&results=10";
		// delete file from previous run in case the system failed to do so
		// due to an exception.
		new File("data/similarAnswers").delete();
		// create new empty file
		FileWriter fw = new FileWriter("data/similarAnswers", true);
		// append similar answers to file
		URL url = new URL(request);
		InputStream in = url.openStream();
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			for (int i = 0; i < len; i++) {
				fw.write((char) buf[i]);
			}
		}
		in.close();
		fw.close();
		// open file with similar answers
		File file = new File("data/similarAnswers");
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		try{
			Document doc = db.parse(file);
			doc.getDocumentElement().normalize();
			NodeList nodeLst = doc.getElementsByTagName("Question");
			// for each similar answer (node) in the (xml) file
			for (int s = 0; s < nodeLst.getLength(); s++) {
				Node fstNode = nodeLst.item(s);
				if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
					Element fstElmnt = (Element) fstNode;
					NodeList fstNmElmntLst = 
						fstElmnt.getElementsByTagName("ChosenAnswer");
					Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
					NodeList fstNm = fstNmElmnt.getChildNodes();
					// TODO: why does this NULL shit happen?? 
					if(fstNm.item(0)!= null){
						// get answer and remove stopwords
						String processed = Preprocessor.removeStopwords(
								((Node) fstNm.item(0)).getNodeValue());
						// build a set representation of the words in the 
						// answer will be needed to check whether a word is
						// contained in it
						HashSet<String> answer = new HashSet<String>();
						// TODO: why does this NULL shit happen?? 
						if (processed != null &&
								! processed.equalsIgnoreCase("null")){
							String[] words = processed.split(" ");
							for (int i = 0; i < words.length; i++) {
								answer.add(words[i]);
							}
							// save the set in the similarAnswers vector
							similarAnswers.add(answer);
						}
					}
				}
			}
		}catch(org.xml.sax.SAXParseException e) {
			// TODO: and what about this?? 
			System.out.println("WARNING: An invalid XML character (Unicode: 0x0)" +
					" was found while retrieving similar documents in assessing " +
					"Coverage.");
		}
		//delete file: similar answers have been saved in similarAnswers vector
		new File("data/similarAnswers").delete();
	}

	/**
	 * Temporarily populates the termFrequency table.
	 * for each term in the current answer, adds an entry in the termFrequency
	 * table with its corresponding number of occurrences.
	 * TODO: consider normalizing term frequency in the length of the answer!
	 * @param answer_content
	 */
	private void computeTermFrequency(String answer_content){
		termFrequency = new Hashtable<String, Integer>();
		String[] words = answer_content.split(" ");
		// for each term in the answer
		for (int i = 0; i < words.length; i++) {
			// if term is not in termFrequency table add it with occurrence 1
			if (! termFrequency.containsKey(words[i])){
				termFrequency.put(words[i], 1);
			}
			// if term is already present update its number of occurrences
			else{
				int occurrences = termFrequency.get(words[i]);
				termFrequency.put(words[i], occurrences+1);
			}
		} 
	}

	/**
	 * Computes the coverage score for the current answer as explained in 
	 * Essential Pages.
	 * For each term computes the term relevance and from it the term 
	 * importance. All term importance scores multiplied by their term 
	 * frequencies are summed. The result is returned as the coverage.
	 * @param answer_content
	 */
	private Double computeCoverageScore(String answer_content){
		Double totalScore = 0.0;
		String[] words = answer_content.split(" ");
		// for each term in the answer
		for (int i = 0; i < words.length; i++) {
			// compute term relevance: percentage of similar answers containing
			// the term
			Double occurrences = 0.0;
			for (HashSet<String> answer : similarAnswers) {
				if(answer.contains(words[i])) occurrences++;
			}

			Double termRelevance = 0.0;
			if(similarAnswers.size() != 0)
				termRelevance = occurrences/similarAnswers.size();
			Double termImportance = 0.0;
			// computer term importance as a function of term relevance
			if(termRelevance!=0)
				termImportance = termRelevance*
				(Math.log(1/termRelevance)/Math.log(2));
			// update total coverage score
			totalScore += termImportance * termFrequency.get(words[i]);
		}
		return totalScore;
	}

}
