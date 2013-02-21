package qqa.ml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classifies opinion questions and definition questions
 * @author Tang Yang
 */
public class OpinionDefinitionQuestions {
	private static Pattern definitionPattern = Pattern.compile(
			"([Ww]ho|[Ww]hat)\\s*" +
			"(is|'s|was)\\s*" +
			"([A-Z][a-z]*\\s*)+" + 
			"\\?"
			);
	
	private static Pattern opinionPattern = Pattern.compile(
			"(" +
			"(which\\s+one\\s+|how)?do\\s+you\\s+(hate|like|love)\\s+" +
			"|your\\s+(opinion|idea)\\s+about\\s+" +
			"|how\\s+do\\s+you\\s+think\\s+about\\s+" +
			"|why\\s+do\\s+you\\s+(support|hate)\\s+" +
			"[A-Z][a-z]*\\s*" +
			")"
			);
	
	
	public static boolean isOpinionQuestion(String question){
		Matcher m = opinionPattern.matcher(question);
		if(m.find()){
			return true;
		}
		return false;
	}
	
	public static boolean isDefinitionQuestion(String question){
		Matcher m = definitionPattern.matcher(question);
		if(m.find()){
			return true;
		}
		return false;
	}
	
	/*
	public static void main(String args[]){
		String input = "./yahoo.txt";
		String opinionquestions = "./opinion.txt";
		String definitionquestions = "./definition.txt";
		try{
			BufferedReader reader = new BufferedReader(new FileReader(new File(input)));
			BufferedWriter ow = new BufferedWriter (new FileWriter (new File(opinionquestions)));
			BufferedWriter dw = new BufferedWriter (new FileWriter (new File(definitionquestions)));
			String line;
			while((line = reader.readLine()) != null){
				line = line.replaceAll("<.*?>", "");
				if(isOpinionQuestion(line)){
					ow.write(line + "\r\n");
				}
				if(isDefinitionQuestion(line)){
					dw.write(line + "\r\n");
				}
			}
			ow.close();
			dw.close();
			reader.close();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	*/
}
