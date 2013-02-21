package qqa.ml.listwise;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class PredictLinearNeuralModel {

	//output: the top 1 object's position in given list with the weight vector w
	//input: weight vector w, the list of feature vector features 
	public static int predict(double[] w, List<double[]> features){
		NormalizeData(features);
		TreeMap<Double,Integer> rank = new TreeMap<Double,Integer>();
		for(int i = 0; i < features.size(); i++){
			double[] feature = features.get(i);
			double score = 0;
			for(int k = 0; k < w.length; k++){
				score += w[k]*feature[k];
			}
			rank.put(score, i);
		}
		int maxId = -1;
		if(rank.size() != 0)
			maxId = rank.lastEntry().getValue();
		return maxId;
	}
	
	//output: the ranking list of given list with the weight vector w
	//input: weight vector w, the list of feature vector features 
	public static List<Integer> rank(double[] w, List<double[]> features){
		NormalizeData(features);
		Random r = new Random();
		TreeMap<Double,Integer> rank = new TreeMap<Double,Integer>();
		for(int i = 0; i < features.size(); i++){
			double[] feature = features.get(i);
			double score = 0;
			for(int k = 0; k < w.length; k++){
				score += w[k]*feature[k];
			}
			rank.put(score+r.nextDouble()*0.0000001, i);
		}
		List<Integer> ranklist = new ArrayList<Integer>();
		for(double key : rank.descendingKeySet()){
			ranklist.add(rank.get(key));
		}
		return ranklist;
	}
	
	//this is a function for test, you don't need to use it
	public static List<List<double[]>> genData(double[] w, int N, int m){
		List<List<double[]>> data = new ArrayList<List<double[]>>();
		Random random = new Random();
		for(int i = 0; i < N; i ++){
			List<double[]> group = new ArrayList<double[]>();
			double maxScore = 0;
			for(int j = 0; j < m; j++){
				double[] feature = new double[w.length];
				double score = 0;
				for(int k = 0; k < w.length; k++){
					feature[k] = random.nextDouble();
					score += w[k]*feature[k];
				}
				if(score >= maxScore){
					maxScore = score;
					group.add(0,feature);
				}else{
					group.add(feature);
				}
			}
			NormalizeData(group);
			data.add(group);
		}
		return data;
	}
	
	//the function will normalize the list of feature vectors, for each feature in the vector, the value of the feature would be set during 0 to 1
	//you can change the function if you don't want to do normalize, just comment all the code in the function
	public static void NormalizeData(List<double[]> x){
		double[] max = new double[x.get(0).length];
		for(int i = 0; i < max.length; i++)
			max[i] = 0;
		for(int i = 0; i < x.size(); i++){
			for(int k = 0; k < max.length; k++){
				if(max[k] < x.get(i)[k])
					max[k] = x.get(i)[k];
			}
		}
		for(int i = 0; i < x.size(); i++){
			for(int k = 0; k < max.length; k++){
				if(max[k]!=0)
					x.get(i)[k] = x.get(i)[k]/max[k];
			}
		}
	}
	
	//a function for test, you don't need to use this.
	public static void Test(){
		double[] w = {2,-1,3,4};
		int N = 100;
		int m = 5;
		double ita = 0.3;
		int T = 1000;
		List<List<double[]>> trainData = genData(w,N,m);
		double[] tmpW = TrainLinearNeuralModel.train(trainData);
		for(int i = 0; i < w.length; i++){
			System.out.print(tmpW[i]+"\t");
		}
		System.out.println();
		for(int i = 0; i < trainData.size(); i++){
			//System.out.println(predict(tmpW,trainData.get(i)));
		}
	}
	
	public static double main(String[] args){
		//this is an example for training and predict
		double[] w = TrainLinearNeuralModel.train("data/qualityRanking/trainingSetWEKA.dat");
		double acc = TrainLinearNeuralModel.test("data/qualityRanking/testSetWEKA.dat",w);
		System.out.println();
		return acc;
	}
}
