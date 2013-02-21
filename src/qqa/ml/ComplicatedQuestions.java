package qqa.ml;

/**
 * Classifies complicated questions that are suitable for summarization
 * @author Mattia Tomasoni
 *
 */
public class ComplicatedQuestions {
	/**
	 * Returns true if a question in complicated according to a selection of
	 * TREC patterns. Returns false for trivial, uninteresting factoid questions
	 * @return true if a question in complicated
	 */
	public static Boolean isComplicatedQuestion(String question){
		try {
			//splitting question into words
			String[] parts = question.split(" ");
			// read through the question
			for(int i = 0; i < parts.length; i++){
				// case 1.	How (do|does|did)
				// case 2.	How (is|are|were|was|will)
				// case 3.	How (could|can|would|should)
				// case 4.	How to
				if(parts[i].equalsIgnoreCase("how")) {
					for (int j = i+1; j < parts.length; j++) {
						// how to 
						if(j == i+1 && parts[j].equalsIgnoreCase("to"))
							return true;
						// how do|does|did...
						if(
								parts[j].equalsIgnoreCase("do") ||
								parts[j].equalsIgnoreCase("does") ||
								parts[j].equalsIgnoreCase("did") ||
								parts[j].equalsIgnoreCase("is") ||
								parts[j].equalsIgnoreCase("are") ||
								parts[j].equalsIgnoreCase("were") ||
								parts[j].equalsIgnoreCase("was") ||
								parts[j].equalsIgnoreCase("will") ||
								parts[j].equalsIgnoreCase("could") ||
								parts[j].equalsIgnoreCase("can") ||
								parts[j].equalsIgnoreCase("would") ||
								parts[j].equalsIgnoreCase("should")
						)
							return true;
					}
				}
				// case 11.	Why
				if(parts[i].equalsIgnoreCase("why"))
					return true;
				// case 12.	What is the reason
				if(parts[i].equalsIgnoreCase("what") &&
						parts[i+1].equalsIgnoreCase("is") &&
						parts[i+2].equalsIgnoreCase("the") &&
						parts[i+3].equalsIgnoreCase("reason"))
					return true;
			}

			return false;
		}
		// I sometimes look a few words ahead in the sentence and might cause
		// exceptions. In that case just return question as uninteresting
		catch (ArrayIndexOutOfBoundsException e) {
			return false;
		}
	}
}
