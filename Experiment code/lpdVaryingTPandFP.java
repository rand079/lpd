/* Code used for Lift-per-drift: an evaluation metric for classification frameworks with concept drift detection
 * 
 * This code is made available for research reproducability. For any other purposes, please contact the author first at rand079 at aucklanduni dot ac dot nz
 */

package ECMLLPDGit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import RobAdaptiveDriftDetectors.AdaptiveDetector;
import RobAdaptiveDriftDetectors.adaptiveADWIN;
import RobAdaptiveDriftDetectors.adaptiveDDM;
import RobAdaptiveDriftDetectors.adaptiveHDDM_A;
import RobAdaptiveDriftDetectors.adaptiveHDDM_W;
import RobAdaptiveDriftDetectors.adaptiveMagSEED;
import RobAdaptiveDriftDetectors.adaptiveSEED;
import RobCode.GenerateSequentialConceptStream;
import RobCode.GenerateDensityConceptStream;
import moa.classifiers.AbstractClassifier;
import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.driftdetection.ADWIN;
import moa.classifiers.core.driftdetection.ADWINChangeDetector;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.core.driftdetection.HDDM_W_Test;
import moa.classifiers.core.driftdetection.PeriodicDetector;
import moa.classifiers.functions.Perceptron;
import moa.classifiers.functions.SGD;
import moa.classifiers.rules.core.changedetection.NoChangeDetection;
import moa.classifiers.trees.HoeffdingTree;
import moa.streams.ArffFileStream;
import moa.streams.InstanceStream;
import rcdsource.instanceRandomiser;

public class lpdVaryingTPandFP {

	public static void main(String[] args) throws Exception {

		BufferedWriter resultsWriter = new BufferedWriter(new FileWriter("ECMLMetrics/lpd/lpdResults.txt"));
		resultsWriter.write("iteration\tdataset\tconcepts\tgoalTP\tgoalFP\tgoaldelay\tnoise\tuniformity\ttp\tfp\tdelay\taccuracy\tlpd\ttime\tdriftsOccurred\tdriftsDetected\ttrainTime\tpredictTime\tclassTime\tbaselineAcc\n");
		BufferedWriter memWriter = new BufferedWriter(new FileWriter("SyntheticMemoryResults.txt"));	
		String[] driftStream = {"Agrawal","Hyperplane", "LED", "RandomRBF", "Waveform"};
		
		for(int i =0; i < 30; i++){ //iteration
			for(String thisStream: driftStream){
				for(int k = 0; k < 1; k = k+2){ //noise 
					//Run approach without drift detection for lpd measure

					for(int c =7; c <= 8; c=c+2){ 
						double noise = 0.125 * (k);


						double clustering = -1.0;
						int meanVolatility = 500;
						int driftsToRun = 500;
						GenerateSequentialConceptStream stream = null;


						stream = new GenerateSequentialConceptStream(c, noise, thisStream, 0, 
								meanVolatility, 0, i*4185, clustering); 
						
						//Get accuracy without drift framework for lpd
						NaiveBayes learner = new NaiveBayes();
						learner.prepareForUse();
						int instCount = 0;
						int correct = 0;
						int driftsOccurred = 0;
						System.out.println("Running accuracy without drift framework");
						while(driftsOccurred <= driftsToRun + 1){
							instCount++;
							Instance inst = stream.nextInstance();
							if(learner.correctlyClassifies(inst)) correct++;
							learner.trainOnInstanceImpl(inst);
							if(stream.isActualDrift()){ 
								driftsOccurred++;
								if(driftsOccurred % 100 == 0) System.out.print(driftsOccurred);
							}
						}
						double baselineAccuracy = (double)correct/(double)instCount;
						System.out.println("Accuracy = " + baselineAccuracy);
						
						for(double goal_TP = 1.0; goal_TP <= 1.0 ; goal_TP = goal_TP + 0.1){ 
							for(double goal_FP = 0; goal_FP <= 0.9 ; goal_FP = goal_FP + 0.1){
									
								stream = new GenerateSequentialConceptStream(c, noise, thisStream, 0, 
										meanVolatility, 0, i*4185, clustering);
								
								System.out.println("Running " + thisStream + " iteration " + i
										+ " with noise " + noise + " and " + c + " concepts and TP:" + goal_TP + "FP:" + goal_FP);
									instCount = 0;	
									int goal_delay = 30; //delay between drift occurring and FP detection
									
									List<Integer> TPoccurances = new ArrayList<Integer>();
									boolean finished = false;
									for(int h = 0; finished == false; h++){
										while(h < driftsToRun * goal_TP){
											TPoccurances.add((int) (Math.random() * driftsToRun + 1));
											h++;
										}
										TPoccurances = TPoccurances.stream().distinct().collect(Collectors.toList());
										if(TPoccurances.size() < driftsToRun * goal_TP) {
											TPoccurances.add((int) (Math.random() * driftsToRun + 1));
										} else {
											finished = true;
										}
									}
									
									int FP_drifts = (int)((driftsToRun * goal_TP)/(1 -goal_FP) - (driftsToRun * goal_TP));
									List<Integer> FPoccurances = new ArrayList<Integer>();
									finished = false;
									int FP_interval_start = 100;
									int FP_interval_end = meanVolatility - 1;
									for(int h = 0; finished == false; h++){
										while(h < FP_drifts * goal_FP){
											FPoccurances.add(((int)(Math.random()*driftsToRun)*meanVolatility) 
													+ (int)(Math.random()*(FP_interval_end-FP_interval_start))+FP_interval_start); //drift of FP + instance of FP
											h++;
										}
										FPoccurances = FPoccurances.stream().distinct().collect(Collectors.toList());
										if(FPoccurances.size() < FP_drifts) {
											FPoccurances.add((int) ((Math.random()*driftsToRun*meanVolatility) 
													+ (Math.random()*(FP_interval_end-FP_interval_start))+FP_interval_start));
										} else {
											finished = true;
										}
									}
									
									long experimentRuntime = 0;
									long trainingRuntime = 0;
									long predictingRuntime = 0;
									long classifyingRuntime = 0;
									long runtimeStart = 0;
									learner = new NaiveBayes();
									learner.prepareForUse();
		
									int driftsDetected = 0;
									driftsOccurred = 0;
									correct = 0;
									int totalSinceDriftOccurred = 0;
									double fp = 0;
									double fn = 0;
									double delay = 0;
									boolean thisDriftDetected = false;
									int trueDriftsDetected = 0;
									
									experimentRuntime = System.currentTimeMillis();
									
									while(driftsOccurred <= driftsToRun + 1){	
										
										instCount++;
										totalSinceDriftOccurred++;											
										Instance inst = stream.nextInstance();
										
										runtimeStart = System.currentTimeMillis();
										trainingRuntime += System.currentTimeMillis() - runtimeStart;
										
										//check if actual drift occurred, if so check if previous was 'found'
										if(stream.isActualDrift()){
											if(driftsOccurred % 100 == 0)System.out.println(instCount + " " + driftsOccurred + " " + driftsToRun);
											driftsOccurred++;
											totalSinceDriftOccurred = 0;
											if(!thisDriftDetected && driftsOccurred > 1 && driftsOccurred <= driftsToRun + 1){
												fn++;
											} else{
												thisDriftDetected = false;
											}
										}
		
										runtimeStart = System.currentTimeMillis();
										
										if(learner.correctlyClassifies(inst)){
											correct++;
										} else {
										}
										classifyingRuntime += System.currentTimeMillis() - runtimeStart;
										
										if((totalSinceDriftOccurred == goal_delay & TPoccurances.contains(driftsOccurred)) | //TPs occur goal_delay after drift
												(FPoccurances.contains(instCount))){ //list of instances 100-499 after each drift
											driftsDetected++;									
											
											learner = new NaiveBayes();
											learner.prepareForUse();
											runtimeStart = System.currentTimeMillis();
																								
											stream.getCurrentConcept();
											
											if(totalSinceDriftOccurred == goal_delay){
												//System.out.print("\n");
												trueDriftsDetected++;
												delay += totalSinceDriftOccurred;
												thisDriftDetected = true;
											} else {
												fp = fp + 1;
											}
											
											trainingRuntime += System.currentTimeMillis() - runtimeStart;
		
										} else{
											
											runtimeStart = System.currentTimeMillis();
											predictingRuntime += System.currentTimeMillis() - runtimeStart;
										}
										
										learner.trainOnInstanceImpl(inst);
		
									}
		
									experimentRuntime = System.currentTimeMillis() - experimentRuntime;
									delay = delay / (double)trueDriftsDetected;
									
									resultsWriter.write(i + "\t" + thisStream + "\t" + c + "\t" + goal_TP + "\t" + goal_FP + "\t" + goal_delay + "\t" + noise 
											+ "\t" + (double)(driftsOccurred - 2 - fn)/((double)(driftsOccurred - 2)) + "\t" + (double)fp/((double)(fp + trueDriftsDetected)) 
											+ "\t" + delay + "\t" + (double)correct/((double)instCount) + "\t" + ((double)correct/((double)instCount)-baselineAccuracy)/driftsDetected
											+ "\t" + experimentRuntime + "\t" + driftsOccurred + "\t" + driftsDetected + "\t" + trainingRuntime + "\t" + predictingRuntime + "\t" + classifyingRuntime 
											+ "\t" + baselineAccuracy + "\n" );
									
									resultsWriter.flush();

							}
						}
					}
				}
			}
		}
		resultsWriter.close();
	}

}
