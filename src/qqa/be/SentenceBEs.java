package qqa.be;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Collection;

/**
 * stores a sentence's content, parsing tree, BEs and corresponding equivalence
 * classes
 * @author Mattia Tomasoni
 *
 */
public class SentenceBEs {
	/**
	 * sentence id
	 */
	public String sentence_id;
	
	/**
	 * stores the content
	 */
	public String content;
	
	/**
	 * stores the parsing tree
	 */
	public String parsed;
	
	/**
	 * stores the average weight of the Bes in the sentence
	 */
	public Double avgBeWeight;

	/**
	 * hash table that contains sentence BEs with corresponding weight
	 */
	public Hashtable<String, Double> weights;

	/**
	 * hash table that contains sentence BEs with corresponding equivalence 
	 * classes
	 */
	public Hashtable<String, Set<String>> eqClasses;
	
	/**
	 * set that contains all sentence equivalence classes of eqClasses in one 
	 * unique set. 
	 * Useful to verify the presence of a BE in one of the equivalence classes
	 * of the Sentence without having to go through the eqClasses table and its
	 * subsets recursively
	 */
	public Set<String> eqClassesSet;

	/**
	 * initializes content, parsing tree and eqClasses fields
	 * @param content
	 * @param parsed
	 */
	public SentenceBEs(String content_par, String parsed_par){
		content = new String(content_par);
		parsed = new String(parsed_par);
		eqClasses = new Hashtable<String, Set<String>>();
		weights = new Hashtable<String, Double>();
		eqClassesSet = new HashSet<String>();
	}

	/**
	 * adds an entry for the specified BE in the equivalence classes table
	 * @param BE
	 * @param doc_id 
	 * @throws IOException 
	 */
	public void addBE(String BE, String doc_id) throws IOException{
		Set<String> eqClass = new HashSet<String>();
		// add BE itself
		eqClass.add(BE);
		eqClassesSet.add(BE);
		// fetch equivalence class file with corresponding to doc_id
		String fileName = ("data/BE/final/eqClasses/QQA." + doc_id);
		BufferedReader input = new BufferedReader(new FileReader(fileName));
		String line;
		while (( line = input.readLine()) != null){
			// ignore empty separator lines 
			if(!line.isEmpty()){
				// ignore "BE: " label at beginning of line
				String tmp = line.substring(4);
				if(tmp.equalsIgnoreCase(BE)){
					// ignore "EQ class: " label
					// get equivalence class from file
					String cl = input.readLine().substring(10).toLowerCase();
					// if equivalence class is non-empty
					if(!cl.equalsIgnoreCase("")){
						// brake line into the equivalent BEs
						String[] cl_parts = cl.split("\t");
						for (int i = 0; i < cl_parts.length; i++) {
							// add equivalent BEs to equivalence classes
							eqClass.add(cl_parts[i]);
							eqClassesSet.add(cl_parts[i]);
						}
					}
				}
			}
		}
		input.close();
		// add equivalence class
		eqClasses.put(BE, eqClass);
	}

	/**
	 * returns the number of words in the sentence.
	 * @return
	 */
	public int getNumWords() {
		return content.split(" ").length;
	}
	
	/**
	 * calculates the average weight of the Bes in the sentence
	 */
	public void calculateAvgBeWeight(){
		Collection<Double> w = weights.values();
		Double totWeightSoFar = 0.0;
		for (Double weight : w) totWeightSoFar+= weight;
		avgBeWeight = totWeightSoFar/weights.size();
	}
}
