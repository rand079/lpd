/* Code used for Lift-per-drift: an evaluation metric for classification frameworks with concept drift detection
 * 
 * This code is made available for research reproducability. For any other purposes, please contact the author first at rand079 at aucklanduni dot ac dot nz
 */

package ECMLLPDGit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Random;

import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.classifiers.bayes.NaiveBayes;
import moa.classifiers.core.driftdetection.ADWINChangeDetector;
import moa.classifiers.core.driftdetection.AbstractChangeDetector;
import moa.classifiers.core.driftdetection.CusumDM;
import moa.classifiers.core.driftdetection.DDM;
import moa.classifiers.core.driftdetection.EDDM;
import moa.classifiers.core.driftdetection.HDDM_W_Test;
import moa.classifiers.core.driftdetection.PageHinkleyDM;
import moa.classifiers.core.driftdetection.PeriodicDetector;
import moa.streams.ArffFileStream;
import moa.streams.InstanceStream;
import rcdsource.instanceRandomiser;

public class lpdRealWorldExperiments {

	static AbstractChangeDetector[] detectors = new AbstractChangeDetector[14];
	static String[] detectorNames = new String[14];
	static String[] args = new String[0];

	public static void createDetectors() throws Exception{ //Use first set of detectors and change to PageHinkley tests for second set of experiments on real-world data
		//first real-world experiment detectors
		detectors = new  AbstractChangeDetector[14];
		detectorNames = new String[14];

		ADWINChangeDetector ddADWIN = new ADWINChangeDetector();
		detectors[0] = ddADWIN;
		detectorNames[0] = "ddADWIN";
		
		CusumDM ddCUSUM = new CusumDM();
		detectors[1] = ddCUSUM;
		detectorNames[1] = "ddCUSUM";
		
		DDM ddDDM = new DDM();
		detectors[2] = ddDDM;
		detectorNames[2] = "ddDDM";
		
		EDDM ddEDDM = new EDDM();
		detectors[3] = ddEDDM;
		detectorNames[3] = "ddEDDM";
		
		PageHinkleyDM ddPageHinkley = new PageHinkleyDM();
		detectors[4] = ddPageHinkley;
		detectorNames[4] = "ddPageHinkley";
		
		HDDM_W_Test ddHDDMw = new HDDM_W_Test();
		detectors[5] = ddHDDMw;
		detectorNames[5] = "ddHDDMw";
		
		PeriodicDetector ddNoChangeTight = new PeriodicDetector();
		ddNoChangeTight.warningZoneSizeOption.setValue(0);
		ddNoChangeTight.driftFreqOption.setValue(60);
		detectors[6] = ddNoChangeTight;
		detectorNames[6] = "ddNoChangeTight";
		
		PeriodicDetector ddNoChangeMed = new PeriodicDetector();
		ddNoChangeMed.warningZoneSizeOption.setValue(0);
		ddNoChangeMed.driftFreqOption.setValue(250);
		detectors[7] = ddNoChangeMed;
		detectorNames[7] = "ddNoChangeMed";
		
		PeriodicDetector ddNoChangePerfect = new PeriodicDetector();
		ddNoChangePerfect.warningZoneSizeOption.setValue(0);
		ddNoChangePerfect.driftFreqOption.setValue(500);
		detectors[8] = ddNoChangePerfect;
		detectorNames[8] = "ddNoChangePerfect";
		
		PeriodicDetector ddNoChangeLoose = new PeriodicDetector();
		ddNoChangeLoose.warningZoneSizeOption.setValue(0);
		ddNoChangeLoose.driftFreqOption.setValue(1000);
		detectors[9] = ddNoChangeLoose;
		detectorNames[9] = "ddNoChangeLoose";
		
		//second real-world experiment detectors
		
		/*
		PageHinkleyDM ddPageHinkleyLowestest = new PageHinkleyDM();
		ddPageHinkleyLowestest.lambdaOption.setValue(1);
		detectors[0] = ddPageHinkleyLowestest;
		detectorNames[0] = "ddPageHinkleyLowestest";
		
		PageHinkleyDM ddPageHinkleyLowest = new PageHinkleyDM();
		ddPageHinkleyLowest.lambdaOption.setValue(20);
		detectors[1] = ddPageHinkleyLowest;
		detectorNames[1] = "ddPageHinkleyLowest";
		
		PageHinkleyDM ddPageHinkleyLow = new PageHinkleyDM();
		ddPageHinkleyLow.lambdaOption.setValue(35);
		detectors[2] = ddPageHinkleyLow;
		detectorNames[2] = "ddPageHinkleyLow";
		
		PageHinkleyDM ddPageHinkley = new PageHinkleyDM();
		ddPageHinkley.lambdaOption.setValue(50);
		detectors[3] = ddPageHinkley;
		detectorNames[3] = "ddPageHinkley";

		PageHinkleyDM ddPageHinkleyHigh = new PageHinkleyDM();
		ddPageHinkleyHigh.lambdaOption.setValue(65);
		detectors[4] = ddPageHinkleyHigh;
		detectorNames[4] = "ddPageHinkleyHigh";
		
		PageHinkleyDM ddPageHinkleyHighest = new PageHinkleyDM();
		ddPageHinkleyHighest.lambdaOption.setValue(80);
		detectors[5] = ddPageHinkleyHighest;
		detectorNames[5] = "ddPageHinkleyHighest";
		*/
		
	}
	
	public static void main(String[] args) throws Exception {

		BufferedWriter resultsWriter = new BufferedWriter(new FileWriter("ECMLMetrics/lpd/lpdRealWorldResults.txt"));
		resultsWriter.write("iteration\tdataset\tdetector\tnoise\trespNoise\tlength\taccuracy\tlpd\tbalAcc\ttime\tdrifts\ttrainTime\tpredictTime\tbaselineAcc\tbalAcc\n");
		String[] driftStream = {"Airlines", "Covtype","Electricity", "Intrusion", "Poker"};
		
		for(int i = 0; i < 1; i++){ //iteration
			for(String thisStream: driftStream){
				for(int k = 0; k < 1; k = k+2){ //predictor noise level
					for(int r = 0; r < 1; r = r + 1){ //response noise
						
						double noise = 0.125 * (k);
						double responseNoise = 0.125 * r;
						
						//Set up stream
						ArffFileStream stream = null;
						String path = "H:/PhD stuff/Uni/Eclipse workspace/RCD/Realworld datasets/";
						InstancesHeader instHeader = null;

						switch(thisStream){

							case "Electricity":
								stream = new ArffFileStream(path + "elecNormNew.arff", -1);
								break;
						
							case "Poker":
								stream = new ArffFileStream(path + "poker-lsn.arff", -1);
								break;
						
							case "Intrusion":
								stream = new ArffFileStream(path + "kddcup99_10percent.arff", -1);
								break;
						
							case "Airlines":
								stream = new ArffFileStream(path + "airlines.arff", -1);
								break;
								
							case "Covtype":
								stream = new ArffFileStream(path + "covtypeNorm.arff", -1);
								break;			

						};
						
						stream.prepareForUse();
						instHeader = ((InstanceStream)stream).getHeader();
						int numClasses = instHeader.attribute(instHeader.classIndex()).numValues();
						
						//Get accuracy without drift framework for lpd
		
						NaiveBayes learner = new NaiveBayes();
						learner.prepareForUse();
						int instCount = 0;
						int correct = 0;
						ArrayList<Integer> balSeen = new ArrayList();
						ArrayList<Integer> balCorrect = new ArrayList();
						for(int x = 0; x < numClasses; x++){
							balSeen.add(0);
							balCorrect.add(0);
						}
						System.out.println("Running accuracy without drift framework");
						
						Random responseNoiseRandom = new Random(345 * i);
						while(stream.hasMoreInstances()){
							instCount++;
							Instance inst = stream.nextInstance().getData();
							double origClass = inst.classValue();
							balSeen.set((int)origClass, balSeen.get((int)origClass) + 1);
							
							//Add response noise
							if(r > 0 && responseNoiseRandom.nextDouble() < responseNoise){
								while(inst.classValue() == origClass)
										inst.setClassValue(responseNoiseRandom.nextInt(numClasses));
							};
							
							if(learner.correctlyClassifies(inst)){
								correct++;
								balCorrect.set((int)origClass, balCorrect.get((int)origClass) + 1);
							}
							learner.trainOnInstanceImpl(inst);
						}
						double baselineAccuracy = (double)correct/(double)instCount;
						double baselineBalancedAccuracy = 0.0;
						for(int x = 0; x < numClasses; x++){
							baselineBalancedAccuracy += 1.0/(double)numClasses * (double)balCorrect.get(x)/(double)balSeen.get(x);
						}
								
						
						for(int j = 0; j < 7; j=j+6){ //drift detector
							
							System.out.println("Running " + thisStream + " using detector " + detectorNames[j] + " iteration " + i
									+ " with noise " + noise);
							stream.restart();
							createDetectors();
							detectors[j].prepareForUse();
							
							long experimentRuntime = 0;
							long trainingRuntime = 0;
							long predictingRuntime = 0;
							long runtimeStart = 0;			
							
							balSeen = new ArrayList<Integer>();
							balCorrect = new ArrayList<Integer>();
							for(int x = 0; x < numClasses; x++){
								balSeen.add(0);
								balCorrect.add(0);
							}
							
							correct = 0;
							instCount = 0;
							int drifts = 0;
							boolean prevWarning = false;
	
							experimentRuntime = System.currentTimeMillis();

							//create noise
							instanceRandomiser ir = null;
							if (noise != 0.0) ir = new instanceRandomiser(0, noise, instHeader, i * 748);
							responseNoiseRandom = new Random(345 * i);
							
							learner = new NaiveBayes();
							//learner = new HoeffdingTree();
							learner.prepareForUse();
							
							//while(instCount < length){	
							while(stream.hasMoreInstances()){
								if(instCount % 5000 == 0)System.out.println(instCount);
								instCount++;
								if (!stream.hasMoreInstances()) stream.restart();
								Instance inst = stream.nextInstance().getData();
								
								double origClass = inst.classValue();
								balSeen.set((int)origClass, balSeen.get((int)origClass) + 1);
								//Add response noise
								if(r > 0 && responseNoiseRandom.nextDouble() < responseNoise){
									while(inst.classValue() == origClass)
									inst.setClassValue(responseNoiseRandom.nextInt(numClasses));
								};
								
								
						
								if (noise > 0.00)
									inst = ir.perturbInstance(inst);
								
								//System.out.println(inst.toString());
								
								//check warning before losing through drift state
								if (detectors[j].getWarningZone()){
									prevWarning = true;
									
								} else {
									if(prevWarning){
									}
								}
								
								if(learner.correctlyClassifies(inst)){
									detectors[j].input(0);
									correct++;
									balCorrect.set((int)origClass, balCorrect.get((int)origClass) + 1);
								} else {
									detectors[j].input(1);
								}
																
								if(detectors[j].getChange()){
									drifts++;
									learner = new NaiveBayes();
									learner.prepareForUse();
									runtimeStart = System.currentTimeMillis();
	
									trainingRuntime += System.currentTimeMillis() - runtimeStart;
									prevWarning = false;
									
									
								} else{
									
									runtimeStart = System.currentTimeMillis();
									predictingRuntime += System.currentTimeMillis() - runtimeStart;
								}
								
								learner.trainOnInstanceImpl(inst);
								
							}
							
							double balancedAccuracy = 0.0;
							for(int x = 0; x < numClasses; x++){
								balancedAccuracy += 1.0/(double)numClasses * (double)balCorrect.get(x)/(double)balSeen.get(x);
							}
							
							
							experimentRuntime = System.currentTimeMillis() - experimentRuntime;
															
							resultsWriter.write(i + "\t" + thisStream + "\t" + detectorNames[j] + "\t" + noise + "\t" + responseNoise
									+ "\t" + instCount + "\t" + (double)correct/((double)instCount) 
									+ "\t" + ((double)correct/((double)instCount)-baselineAccuracy)/drifts + "\t" + balancedAccuracy + "\t" + experimentRuntime + "\t" + drifts 
									+ "\t" + + trainingRuntime + "\t" + predictingRuntime 
									+ "\t" + baselineAccuracy + "\t" + baselineBalancedAccuracy + "\n");
	
							
							resultsWriter.flush();

						}
					}
				}
			}
		}
		
		resultsWriter.close();
		
	}

}
