
package ECMLLPDGit;

import java.util.Random;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.streams.generators.CIRCLESGenerator;
import moa.options.AbstractOptionHandler;
import moa.streams.InstanceStream;
import moa.streams.generators.AgrawalGenerator;
import moa.streams.generators.CIRCLESGeneratorMC;
import moa.streams.generators.HyperplaneGenerator;
import moa.streams.generators.LEDGeneratorDrift;
import moa.streams.generators.RandomRBFGenerator;
import moa.streams.generators.SEAGenerator;
import moa.streams.generators.STAGGERGenerator;
import moa.streams.generators.WaveformGeneratorDrift;
import rcdsource.instanceRandomiser;

public class GenerateSequentialConceptStream {

	//stream parameters
	int seed;
	int[][] volatilityMatrix;
	double[][] transitionProbs;
	double[] classBalancePerConcept = null;
	int concepts; //total number of concepts
	double noiseValue = 0; //noise added to numeric instance predictor variables
	instanceRandomiser ir = null; //for noise
	Integer[] driftValue;
	int length;
	String streamDriftSetting;
	int numClasses;
	int numAttributes;
	Random rng;
	double clusterPercentage;
	double lastClass;
	
	AbstractOptionHandler[] streamGen;
	String thisStream;
	InstancesHeader instHeader[];
	
	//tracking current progress
	int currentConcept = 0;
	int nextConcept;
	int lastConcept = 0;
	int instancesGenerated = 0;
	int totalInstancesGenerated = 0;
	int conceptInstancesGenerated = 0;
	int currentVolatility = 0;
	boolean driftPoint = false;
	
	public void createTransitionMatrix(double maxTransitionScale){ //0 makes all transitions equally likely
		this.transitionProbs = new double[concepts][concepts];
		double avTransition = 1.0/(concepts-1);
		
		if(maxTransitionScale > 0) {
			double maxTransition = avTransition * maxTransitionScale;
			if(maxTransition > 1.0) System.out.println("maxTransitionScale too high: " + maxTransition);
			//for each concept
			for(int from = 0; from < concepts; from++){
				double probTotal = 0;
				
				//decide which transition will have max prob
				int maxTransitionIndex = -1;
				if(maxTransition == 1){
					maxTransitionIndex = (from + 1 == concepts ? 0 : from + 1);
				} else {			
					while(maxTransitionIndex == -1 || maxTransitionIndex == from)
						maxTransitionIndex = (int)(rng.nextDouble() * (double)concepts);
				}
				
				//generate transitions that sum to one for this concept
				for(int to = 0; to < concepts; to++){
					if(to == maxTransitionIndex) { 
						transitionProbs[from][to] = maxTransition;
						probTotal += maxTransition;
					} else if (from == to) {
						transitionProbs[from][to] = 0.0;
					} else if (to == concepts - 1 || (to == concepts - 2 && (maxTransitionIndex == concepts  - 1 || from == concepts - 1)) || 
							(to == concepts - 3 && (maxTransitionIndex + from == concepts - 1 + concepts - 2))){
						if (to == concepts - 1) transitionProbs[from][to] = 1-probTotal;
						else transitionProbs[from][to] = 1-probTotal - maxTransition;
					} else{
						transitionProbs[from][to] = rng.nextDouble() * (1 - probTotal) * maxTransition;
						probTotal += transitionProbs[from][to];
					}
				}
			}
		} else {
			for(int from = 0; from < concepts; from++){
				for(int to = 0; to < concepts; to++){
					transitionProbs[from][to] = (from == to) ? 0 : avTransition;
				}
			}
		}

		

	}
	
	//manually specify transition matrix
	public void setTransitionMatrix(double[][] newTransitionMatrix){
		this.transitionProbs = newTransitionMatrix;
	}
	
	//Generate a matrix of mean volatility for concept transitions
	public void createVolatilityMatrix(int mean, double scale){
		this.volatilityMatrix = new int[concepts][concepts];
		for(int from = 0; from < concepts; from++){
			for(int to = 0; to < concepts; to++){
				if (from == to) {
					volatilityMatrix[from][to] = 0;
				} else {
					int thisVolatility = -1;
					while(thisVolatility < 0)
						thisVolatility = (int)((double)mean + (mean * scale * (rng.nextDouble() * 2  - 1)));
					volatilityMatrix[from][to] = thisVolatility;
				}
			}
		}
	}
	
	//manually specify volatility matrix
	public void setVolatilityMatrix(int[][] newVolatilityMatrix){
		this.volatilityMatrix = newVolatilityMatrix;
	}
	
	//manually specify balance matrix
	public void setClassBalancePerConcept(double[] newClassBalanceMatrix){
		this.classBalancePerConcept = newClassBalanceMatrix;
	}
	
	//build a new stream with minimum parameters
	public GenerateSequentialConceptStream(int numConcepts, double noise, String thisStream, double maxTransitionScale, int meanVolatility, double volatilityScale, int thisSeed,
			double clusterPerc){
		seed = thisSeed;
		noiseValue = noise;
		rng = new Random(seed);
		concepts = numConcepts;
		createTransitionMatrix(maxTransitionScale);
		createVolatilityMatrix(meanVolatility, volatilityScale);
		numClasses = thisStream == "LED" ? 10: (thisStream == "Waveform" ? 3 : 2);
		streamGen = new AbstractOptionHandler[concepts]; //have a stream generator per concept
		instHeader = new InstancesHeader[concepts];
		clusterPercentage = clusterPerc;
		
		for(int i = 0; i < concepts; i++){
			switch(thisStream){
			case "LED":
				streamGen[i] = new LEDGeneratorDrift();
				((LEDGeneratorDrift)streamGen[i]).instanceRandomSeedOption.setValue((seed*i*10000000) + 0);
				if (noiseValue != 0.0) ((LEDGeneratorDrift)streamGen[i]).noisePercentageOption.setValue((int)(noiseValue*100));
				streamDriftSetting = "numberAttributesDrift";
				driftValue = new Integer[]{1, 3, 5, 7, 2, 4, 6};
				streamGen[i].prepareForUse();
				break;
		
			case "Agrawal":
				streamGen[i] = new AgrawalGenerator();
				((AgrawalGenerator)streamGen[i]).instanceRandomSeedOption.setValue((seed*i*10000000) + 0);
				streamDriftSetting = "function";
				driftValue = new Integer[]{1, 3, 5, 7, 9, 2, 4, 6, 8};
				break;
		
			case "SEA":
				streamGen[i] = new SEAGenerator();
				((SEAGenerator)streamGen[i]).instanceRandomSeedOption.setValue((seed*i*10000000) + 0);
				streamDriftSetting = "function";
				driftValue = new Integer[]{1, 3, 2, 4};
				break;
				
			case "STAGGER":
				streamGen[i] = new STAGGERGenerator();
				((STAGGERGenerator)streamGen[i]).instanceRandomSeedOption.setValue((seed*i*10000000) + 0);
				streamDriftSetting = "function";
				driftValue = new Integer[]{1, 2, 3};
				break;
				
			case "Waveform":
				streamGen[i] = new WaveformGeneratorDrift();
				((WaveformGeneratorDrift)streamGen[i]).instanceRandomSeedOption.setValue((seed*i*10000000) + 0);
				streamDriftSetting = "numberAttributesDrift";
				driftValue = new Integer[]{1, 3, 5, 7, 2, 4, 6};
				break;
				
			case "CIRCLES":
				streamGen[i] = new CIRCLESGenerator();
				((CIRCLESGenerator)streamGen[i]).instanceRandomSeedOption.setValue((seed*i*10000000) + 0);
				streamDriftSetting = "startCircle";
				((CIRCLESGenerator)streamGen[i]).driftPointOption.setValue(999999999);
				driftValue = new Integer[]{0, 2, 4, 1, 3};
				break;
				
			case "Hyperplane":
				streamGen[i] = new HyperplaneGenerator();
				((HyperplaneGenerator)streamGen[i]).instanceRandomSeedOption.setValue((seed*i*10000000) + 0);
				streamDriftSetting = "numDriftAtts";
				((HyperplaneGenerator)streamGen[i]).noisePercentageOption.setValue(0);
				driftValue = new Integer[]{1, 3, 5, 7, 2, 4, 6};
				break;
				
			case "RandomRBF":
				streamGen[i] = new RandomRBFGenerator();
				((RandomRBFGenerator)streamGen[i]).instanceRandomSeedOption.setValue((seed*i*10000000) + 0);
				((RandomRBFGenerator)streamGen[i]).numCentroidsOption.setValue(10);
				streamDriftSetting = "numCentroids";
				driftValue = new Integer[]{10, 30, 50, 70, 20, 40, 60};
				((RandomRBFGenerator)streamGen[i]).modelRandomSeedOption.setValue(driftValue[0]+seed*i*1000);
				break;
				
			}
			streamGen[i].getOptions().getOption(streamDriftSetting).setValueViaCLIString("" + driftValue[i]);
			streamGen[i].prepareForUse();
			instHeader[i] = ((InstanceStream)streamGen[i]).getHeader();
		}
		
		if (noise > 0) ir = new instanceRandomiser(0.0, noise, instHeader[0], seed);
		numAttributes = instHeader[0].numAttributes();
		findNextConcept();
	}
	
	public Instance nextInstance(){
		totalInstancesGenerated++;
		conceptInstancesGenerated++;
		if(conceptInstancesGenerated == volatilityMatrix[currentConcept][nextConcept]){ //transition point
			driftPoint = true;
			conceptInstancesGenerated = 0;
			lastConcept = currentConcept;
			//currentConcept = ((currentConcept + 1) == concepts ? 0 : currentConcept + 1);
			currentConcept = nextConcept;
			findNextConcept();
			//System.out.println(instancesGenerated +"," + currentConcept);
		} else{
			driftPoint = false;
		}
		
		
		boolean correctClass = false;
		boolean diffClass = false;
		
		Instance inst = ((InstanceStream)streamGen[currentConcept]).nextInstance().getData();
		
		if(clusterPercentage > 0){
			if(Math.random() > clusterPercentage) diffClass = true;
			while(correctClass == false){
				inst = ((InstanceStream)streamGen[currentConcept]).nextInstance().getData();
				if((inst.classValue() == lastClass & !diffClass) || (inst.classValue() != lastClass & diffClass)) correctClass = true;
			}
		}
		
		if (noiseValue > 0.00)
			inst = ir.perturbInstance(inst);		
		
		this.lastClass = inst.classValue();
		//System.out.println(inst.classValue());
		 
		return inst;
	}
	
	
	public int getConcept(){
		return currentConcept;
	}
	
	public int getCurrentVolatility(){
		return volatilityMatrix[currentConcept][nextConcept];
	}
	
	public int getNumClasses(){
		return numClasses;
	}
	
	public int getNumAttributes(){
		return numAttributes;
	}
	
	public boolean isActualDrift(){
		return driftPoint;
	}
	
	//Based on current concept and transition probabilities, find next concept
	public void findNextConcept(){
		nextConcept = -1;
		double conceptChoice = rng.nextDouble();
		double cumProb = 0;
		for(int i = 0; i < concepts; i++){
			if(i == currentConcept) continue;
			double thisProb = transitionProbs[currentConcept][i];
			if (cumProb < conceptChoice && conceptChoice < thisProb + cumProb){
				nextConcept = i;
				break;
			}
			cumProb += thisProb;
		}
		if(nextConcept < 0)	System.out.println("No next concept selected, problem with transition matrix");
		if(nextConcept == currentConcept)	System.out.println("Next and current concept are the same");
	}
	
	public int getLastConcept(){
		return this.lastConcept;
	}
	
	public int getCurrentConcept(){
		return this.currentConcept;
	}
	
	public int getNextConcept(){
		return this.getNextConcept();
	}
}
