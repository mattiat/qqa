package qqa.ml;

import weka.core.Stopwords;

/**
 * Removes punctuation, multiple white spaces and stopwords 
 * @author Mattia Tomasoni
 */
public class Preprocessor {
	public static String removeStopwords(String unprocessed){
		char [] punctuation = {'.',',',';',':','!','?','(',')'};
		// remove punctuation
		for (int i = 0; i < punctuation.length; i++) {
			unprocessed = unprocessed.replace( punctuation[i], ' ' );
		}
		// remove multiple white spaces
		unprocessed = unprocessed.replaceAll("\\b\\s{2,}\\b", " ");
		// remove stopwords
		String[] words = unprocessed.split(" ");
		String processed = null;
		for (int i = 0; i < words.length; i++) {
			if(!Stopwords.isStopword(words[i])){
				if(processed==null)
					processed = new String(words[i]);
				else processed = processed.concat(" " + words[i]);
			}
		}
		return processed;
	}

}
