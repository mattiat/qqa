package qqa.ml;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import qqa.be.DocumentBEs;
import qqa.be.SentenceBEs;

import lpsolve.*;

/**
 * Class to solve the Sentence selection problem with ILP (Integer Linear 
 * Programming) using term score to avoid redundancy as explained in "A Scalable
 * Global Model for Summarization" by Dan Gillick and Benoit Favre 
 * @author Jinfeng Feng (adapted by Mattia Tomasoni)
 */
public class ILP {
	public double optimum;
	public double [] variables;
	public int numConcept;
	
	int lengthLimit;
	double simLimit;	
	
	
	public ILP(int ll) {
		this.lengthLimit = ll;
	}
	
	/**
	 * solves the sentence selection problem using BEs as terms.
	 * @param answers
	 * @throws LpSolveException 
	 * @throws IOException 
	 * @throws Exception
	 */
	public String solveBE(Vector<DocumentBEs> answers, DocumentBEs question) 
	throws LpSolveException, 
	IOException {
		// output BE weights to file
		String fileName = "data/BEweights.txt";
		new File(fileName).delete();
		FileWriter fw = new FileWriter(fileName, true);
		// initialization steps
		// store all bes and sentences in separate structure
		HashMap<String, Double> BEScore = new HashMap<String, Double>();
		ArrayList<SentenceBEs> sentList = new ArrayList<SentenceBEs>();
		// for each answer
		for (DocumentBEs answer: answers) {
			// for each sentence
			for (SentenceBEs sentence : answer.sentences) {
				// add all BEs in the sentence
				BEScore.putAll(sentence.weights);
				// add sentence itself (if greater then threshold)
				// TODO find something smarter! sometimes this eliminates all
				// the sentences!
//				if(sentence.avgBeWeight> 0.1)
					sentList.add(sentence);
				//output to file
				fw.write(sentence.content.toUpperCase() + "\n");
				fw.write("*** avg(WEIGHT) = " + sentence.avgBeWeight + "***\n");
				fw.write(sentence.weights.toString() + "\n");
			}
			fw.write("\n\n");
		}
		fw.close();
		// calculate average be weight
		Double totWeight = 0.0;
		for(String term : BEScore.keySet()) {
			totWeight+=BEScore.get(term);
		}
		// keep only bes with weight above threshold
		HashMap<String, Double> filteredBEScore = new HashMap<String, Double>();
		Double threshold = totWeight/BEScore.size();
		// set threshold: half of average weight
		// (get rid of approximately 25% of concepts)
		threshold = threshold/2;
		for(String term : BEScore.keySet()) {
			if(BEScore.get(term) > threshold)
				filteredBEScore.put(term, BEScore.get(term));
		}
		
		
		// begin ILP
		LpSolve lp;
		this.numConcept = filteredBEScore.size();
		int numSent = sentList.size();
		int ncol = numConcept + numSent;
		// coefficients 
		double [] row = new double[ncol + 1];
		double [] rowConst = new double[ncol + 1];
		
		lp = LpSolve.makeLp(0, ncol);
        if(lp.getLp() == 0) {
        	String error = "Error when getLP.";
        	System.err.println(error);
        	return error;
        }
        
        lp.setAddRowmode(true);
        for(int i = 0; i < ncol; i++)
        	lp.setBinary(i+1, true);
        
        // add constraints, length constraint
        setZero(row);
        for(int i = numConcept + 1; i <= ncol; i++)
        	row[i] = sentList.get(i - numConcept - 1).getNumWords();  // notice that index of variables start from 1
        lp.addConstraint(row, LpSolve.LE, lengthLimit);    
        
        // add constraints, concept - sentence constraints
        int iconcept = 1;
        for(String term : filteredBEScore.keySet()) {
        	setZero(rowConst);
        	rowConst[iconcept] = -1;
        	
        	for(int j = 0; j < sentList.size(); j++) {
        		setZero(row);
        		row[iconcept] = -1;
        		if(sentList.get(j).eqClassesSet.contains(term)) {
        			row[numConcept + j + 1] = 1;
        			rowConst[numConcept + j + 1] = 1;
        			
        			//lp.addConstraint(row, LpSolve.LE, 0); // constraint (1)  
        		}
        		else {
        			rowConst[numConcept + j + 1] = 0;
        		}
        		
        		lp.addConstraint(row, LpSolve.LE, 0); // constraint (1) 		
        	} // end j
        	
        	lp.addConstraint(rowConst, LpSolve.GE, 0);
        	iconcept++;
        }
              
        lp.setAddRowmode(false);
        
        // set object function
        setZero(rowConst);
        iconcept = 1;
        for(String term : filteredBEScore.keySet()) {
        	rowConst[iconcept] = filteredBEScore.get(term);
        	iconcept++;
        }        
        lp.setObjFn(rowConst);    
        lp.setMaxim();       
        
        //lp.writeLp("f:\\model.lp");
        lp.setVerbose(LpSolve.IMPORTANT);
    	String summary= new String();
        try {
			if(lp.solve() == LpSolve.OPTIMAL) {
				this.optimum = lp.getObjective();
				this.variables = new double[ncol];
				lp.getVariables(variables);
				
				// get question id
				String[] ids = question.doc_id.split("\\.");
				// get question content
				String content = new String();
				for(SentenceBEs sentence : question.sentences){ 
					content+=sentence.content;
				}
				// update groups.txt file with ques_id and content
				// this is a loop, do not delete previously existing copies of 
				// this file! it is done in E_AnswerQuestions already!
				FileWriter gw = 
					new FileWriter("data/annotation/groups.txt",true);
				gw.write(ids[0] + "\t" + content + "\n");
				gw.close();
				// create directory named after ques_id
				new File("data/annotation/" + ids[0]).mkdir();
				
				// create summary.txt file: contain sentence id and sentence
				// for every sentence in the summary
				new File("data/annotation/" + ids[0] + "/summary.txt").delete();
				FileWriter su = new FileWriter("data/annotation/" + ids[0] + 
						"/summary.txt",true);
				// total number of sentences selected in the summary
				int tot = 0;
				for(int tt = numConcept; tt < variables.length; tt++) 
					if(variables[tt] == 1) tot ++;
				su.write(tot + "\n");
				summary+= "***SUMMARIZED ANSWER***\n";
				for(int tt = numConcept; tt < variables.length; tt++){
					if(variables[tt] == 1){
						// store summary to String. To be returned by method
						summary+= sentList.get(tt-numConcept).content;
						// store sentence_id to file
						su.write(sentList.get(tt-numConcept).sentence_id + '\n');
					}
				}
				su.close();
				
				// create sentences.txt file: contains sentence id and sentence 
				// content for every sentence in each answer
				new File("data/annotation/" + ids[0] + "/sentences.txt").delete();
				FileWriter se = new FileWriter("data/annotation/" + ids[0] + 
						"/sentences.txt",true);
				summary+= "\n\n\n***ORIGINAL ANSWERS***\n";
				// for each answer
				for (DocumentBEs answer: answers) {
					summary+= answer.doc_id + "\n";
					summary+= "(quality = " + answer.docQuality + ")\n";
					// for each sentence
					for (SentenceBEs sentence : answer.sentences) {
						// add original answers at the bottom of the summary
						summary+= sentence.content;
						// add original answer sentence to sentences.txt file
						se.write(sentence.sentence_id + "\t" + sentence.content 
								+ "\n");
					}
					summary+= "\n";
				}
				se.close();
				
				// create rouge files: contains simply the sentences, one per line
				new File("../ROUGE-1.5.5/peers/" + ids[0] + ".000.spl").delete();
				FileWriter rw = new FileWriter("../ROUGE-1.5.5/peers/" + 
						ids[0] + ".000.spl",true);
				for(int tt = numConcept; tt < variables.length; tt++){
					if(variables[tt] == 1){
						// store summary to file, one sentence per line
						rw.write(sentList.get(tt-numConcept).content + '\n');
					}
				}
				rw.close();

			} else {
				System.err.println("ILP cannot find optimal solution.");
				return "empty";
			}
		} catch (LpSolveException e) {
			e.printStackTrace();
		}
    
        // free memory
        if(lp.getLp() != 0)
            lp.deleteLp();
        
        // return string containing the summary
        return summary;
	}
	
	void setZero(double [] arr) {
		for(int i = 0; i < arr.length; i++)
			arr[i] = 0;
	}
}
