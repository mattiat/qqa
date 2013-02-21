package qqa.be;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Vector;

/**
 * for a given document, stores, the content, parsed content, BEs and 
 * equivalence classes of each sentence
 * @author Mattia Tomasoni
 *
 */
public class DocumentBEs {
	/**
	 * document id: corresponds to ques_id + answer_id separated by a dot,
	 * which is the name used to store to file the output of BEwT-E
	 */
	public String doc_id;
	
	/**
	 * document quality: field that stores the value outputed by the quality
	 * classifier
	 */
	public Double docQuality;
	
	/**
	 * document coverage: field that stores the value outputed by the coverage
	 * classifier
	 */
	public Double docCoverage;

	/**
	 * contains for each sentence, the content, parsed content, BEs and 
	 * equivalence classes
	 */
	public Vector<SentenceBEs> sentences;

	/**
	 * create and store SentenceBEs object for designed doc_id
	 * @param doc_id_par
	 * @throws IOException 
	 * @throws Exception 
	 * @throws Exception 
	 */
	public DocumentBEs(String doc_id_par) throws IOException{
		doc_id = doc_id_par;
		sentences = new Vector<SentenceBEs>();
		// fetch document with corresponding to doc_id
		String fileName = ("data/BE/final/BEs/QQA." + doc_id);
		BufferedReader input =  new BufferedReader(new FileReader(fileName));
		// read one sentence at a time and build corresponding SentenceBEs
		String line = input.readLine();
		// while there is a new sentence
		// @lemmas denotes EOF
		while (!line.startsWith("@lemmas")){
			// save its content
			String content = line.substring(1);
			// read the parsed sentence
			line = input.readLine();
			// store the parsed sentence
			String parsed = line.substring(1);
			SentenceBEs sentence = new SentenceBEs(content,parsed);
			// while there a new line
			NewSentence: while (( line = input.readLine()) != null){
				// ignore empty separator lines 
				// ignore Rule labels lines
				if(!line.isEmpty() && !line.startsWith("#Rule:")){
					if(line.charAt(0)!='#' // BE do not begin with '#'
						&& !line.startsWith("@lemmas")){ // @lemmas denotes EOF
						//remove rule label
						int index = line.indexOf(
								bewte.BEConstants.BE_SEPARATOR_CHAR);
						// add BE to sentence
						String BE = line.substring(index+1) + 
						bewte.BEConstants.BE_SEPARATOR_CHAR;
						BE = BE.toLowerCase();
						sentence.addBE(BE,doc_id);
					}
					else{
						// assign sentence id: set to doc_id plus the position 
						// the answer will get in the sentences vector separated
						// by a dot
						sentence.sentence_id = doc_id + "." +
							Integer.toString(sentences.size());
						// store sentenceBes object
						sentences.add(sentence);
						
						// prevent first line from being read twice by outer 
						// loop!
						// process new sentence!
						break NewSentence;
					}
				}
			}
		}
		input.close();
	}
	
	/**
	 * returns the number of words in the document.
	 * @return
	 */
	public int getNumWords() {
		int length = 0;
		// sum of all number of words in all sentences
		for (SentenceBEs sentence : sentences) {
			length += sentence.getNumWords();
		}
		return length;
	}
}
