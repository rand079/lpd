/* Code used for Lift-per-drift: an evaluation metric for classification frameworks with concept drift detection
 * 
 * This code is made available for research reproducability. For any other purposes, please contact the author first at rand079 at aucklanduni dot ac dot nz
 */

package ECMLLPDGit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Random;

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

public class lpdVaryingSequentiality {

	static AbstractChangeDetector[] detectors = new AbstractChangeDetector[14];
	static String[] detectorNames = new String[14];
	
	public static void createDetectors() throws Exception{
		//detectors
		detectors = new  AbstractChangeDetector[14];
		detectorNames = new String[14];
		
		HDDM_W_Test ddHDDMwStrict = new HDDM_W_Test();
		ddHDDMwStrict.lambdaOption.setValue(0.05);
		ddHDDMwStrict.driftConfidenceOption.setValue(0.0005);
		detectors[0] = ddHDDMwStrict;
		detectorNames[0] = "ddHDDMwStrict";
		
		HDDM_W_Test ddHDDMwMed = new HDDM_W_Test();
		ddHDDMwMed.lambdaOption.setValue(0.05);
		ddHDDMwMed.driftConfidenceOption.setValue(0.01);
		detectors[1] = ddHDDMwMed;
		detectorNames[1] = "ddHDDMwMed";
		
		HDDM_W_Test ddHDDMwLoose = new HDDM_W_Test();
		ddHDDMwLoose.lambdaOption.setValue(0.05);
		ddHDDMwLoose.driftConfidenceOption.setValue(0.2);
		detectors[2] = ddHDDMwLoose;
		detectorNames[2] = "ddHDDMwLoose";
		
		ADWINChangeDetector ddADWINStrict = new ADWINChangeDetector();
		ddADWINStrict.deltaAdwinOption.setValue(0.005);
		detectors[3] = ddADWINStrict;
		detectorNames[3] = "ddADWINStrict";
		
		ADWINChangeDetector ddADWINMed = new ADWINChangeDetector();
		ddADWINMed.deltaAdwinOption.setValue(0.01);
		detectors[4] = ddADWINMed;
		detectorNames[4] = "ddADWINMed";
		
		ADWINChangeDetector ddADWINLoose = new ADWINChangeDetector();
		ddADWINLoose.deltaAdwinOption.setValue(0.05);
		detectors[5] = ddADWINLoose;
		detectorNames[5] = "ddADWINLoose";
		
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
		
	}
	public static void main(String[] args) throws Exception {
		
		boolean writeMem = false;
		BufferedWriter resultsWriter = new BufferedWriter(new FileWriter("ECMLMetrics/lpd/lpdResults.txt"));
		resultsWriter.write("iteration\tdataset\tconcepts\tclustering\tdetector\tnoise\tuniformity\ttp\tfp\tdelay\taccuracy\tlpd\ttime\tdrifts\ttrainTime\tpredictTime\tclassTime\tbaselineAcc\n");
		BufferedWriter memWriter = new BufferedWriter(new FileWriter("SyntheticMemoryResults.txt"));
		memWriter.write("iteration\tdetector\tpredfreq\tdriftsOccurred\tinstances\tdataset\texpTime\ttrainTime\tpredictTime\tclassTime\n");		
		String[] driftStream = {"Agrawal","Hyperplane", "LED", "RandomRBF", "Waveform"};
		
		for(int i =0; i < 30; i++){ //iteration
			for(String thisStream: driftStream){
				for(int k = 0; k < 1; k = k+2){ //noise 
					for(int c =7; c <= 8; c=c+2){ //concepts 
						for(double sequentiality = 0.5; sequentiality < 1.0; sequentiality=sequentiality + 0.225){

							double noise = 0.125 * (k);

							int meanVolatility = 500;
							int driftsToRun = 500;
							GenerateSequentialConceptStream stream = null;

							stream = new GenerateSequentialConceptStream(c, noise, thisStream, 0, 
									meanVolatility, 0, i*4185, sequentiality);

							
							//Get accuracy without drift framework for lpd
							NaiveBayes learner = new NaiveBayes();
							//HoeffdingTree learner = new HoeffdingTree(); learner.leafpredictionOption.setChosenIndex(1);
							learner.prepareForUse();
							int instCount = 0;
							int correct = 0;
							int driftsOccurred = 0;
							System.out.println("Running accuracy without drift framework");
							while(driftsOccurred <= driftsToRun + 1){
								instCount++;
								Instance inst = stream.nextInstance();
								//System.out.print((int)inst.classValue());
								if(learner.correctlyClassifies(inst)) correct++;
								learner.trainOnInstanceImpl(inst);
								if(stream.isActualDrift()){ 
									driftsOccurred++;
									if(driftsOccurred % 100 == 0) System.out.print(driftsOccurred);
								}
							}
							double baselineAccuracy = (double)correct/(double)instCount;
							System.out.println("Accuracy = " + baselineAccuracy);
						

							for(int j = 6; j < 10; j++){ //drift detector detectors.length detectors.length default 1-2
								
								//if (j != 1 &  j < 6) continue;										meanVolatility, 0, i*4185, sequentiality);
								
								createDetectors();
								detectors[j].prepareForUse();
								System.out.println("Running " + thisStream + " using detector " + detectorNames[j] + " iteration " + i
										+ " with noise " + noise + " and " + c + " concepts");
															
								instCount = 0;
								driftsOccurred = 0;
								correct = 0;
								long experimentRuntime = 0;
								long trainingRuntime = 0;
								long predictingRuntime = 0;
								long classifyingRuntime = 0;
								long runtimeStart = 0;
								double sumClusters = 0;
								
								learner = new NaiveBayes();
								learner.prepareForUse();

								int driftsDetected = 0;
								driftsOccurred = 0;
								correct = 0;
								int warningLength = 0;
								int totalSinceDriftOccurred = 0;
								int totalSinceDriftDetected = 0;
								int errorsSinceDriftDetected = 0;
								boolean prevWarning = false;
								
								int lastConcept = 0;

								double fp = 0;
								double fn = 0;
								double delay = 0;
								boolean thisDriftDetected = false;
								int trueDriftsDetected = 0;
								
								experimentRuntime = System.currentTimeMillis();
								
								while(driftsOccurred < driftsToRun + 1){	
								//while(stream.hasMoreInstances()){
									
									instCount++;
									totalSinceDriftDetected++;
									totalSinceDriftOccurred++;

									if(instCount%10000 == 0) System.out.println(instCount);
									if(instCount%50000 == 0 & writeMem){
											memWriter.write(i + "\t" + detectorNames[j] + "\t" +  driftsOccurred + "\t" + instCount + "\t" 
													+ thisStream + "\t" + (System.currentTimeMillis() - experimentRuntime) + "\t" + trainingRuntime + "\t" + predictingRuntime + "\t" + classifyingRuntime + "\t" 
													+ learner.measureByteSize() + "\t" + detectors[j].measureByteSize() + "\t" + "" +  "\n");
										memWriter.flush();
									}
									
									Instance inst = stream.nextInstance();
									
									runtimeStart = System.currentTimeMillis();
									trainingRuntime += System.currentTimeMillis() - runtimeStart;
									
									//check if actual drift occurred, if so check if was found
									if(stream.isActualDrift()){
										if(driftsOccurred % 100 == 0)System.out.println(instCount + " " + driftsOccurred + " " + driftsToRun);
										driftsOccurred++;
										totalSinceDriftOccurred = 0;
										if(!thisDriftDetected && driftsOccurred > 1){
											fn++;
										} else{
											thisDriftDetected = false;
										}
									}
									
									//check warning before losing through drift state
									if (detectors[j].getWarningZone()){
										warningLength++;
										prevWarning = true;
										
									} else {
										if(prevWarning){ 
											warningLength = 0;
										}
									}
									
									runtimeStart = System.currentTimeMillis();
									
									if(learner.correctlyClassifies(inst)){
										detectors[j].input(0);
										correct++;
									} else {
										detectors[j].input(1);
										errorsSinceDriftDetected++;
									}
									classifyingRuntime += System.currentTimeMillis() - runtimeStart;
																	
									if(detectors[j].getChange()){
									//if(stream.driftPoint){
										//System.out.print("D");
										driftsDetected++;									
										
										learner = new NaiveBayes();
										//learner = new HoeffdingTree(); learner.leafpredictionOption.setChosenIndex(1);
										//learner = new Perceptron();
										learner.prepareForUse();
										runtimeStart = System.currentTimeMillis();
																							
										lastConcept = stream.getCurrentConcept();
										
										if(!thisDriftDetected && driftsOccurred >= 1){
											//System.out.print("\n");
											trueDriftsDetected++;
											delay += totalSinceDriftOccurred;
											thisDriftDetected = true;
										} else {
											fp = fp + 1;
										}
										
										trainingRuntime += System.currentTimeMillis() - runtimeStart;
										prevWarning = false;
										warningLength = 0;
										totalSinceDriftDetected = 0;
										errorsSinceDriftDetected  = 0;

									} else{
										
										runtimeStart = System.currentTimeMillis();
										predictingRuntime += System.currentTimeMillis() - runtimeStart;
									}
									
									learner.trainOnInstanceImpl(inst);

								}

								experimentRuntime = System.currentTimeMillis() - experimentRuntime;
								//System.out.print(driftPredictor.countSkips + "," + instCount);
								delay = delay / (double)trueDriftsDetected;
								//if(driftsOccurredBeforeClustering == 0) driftsOccurredBeforeClustering = 2750;
								
								resultsWriter.write(i + "\t" + thisStream + "\t" + c + "\t" + sequentiality + "\t" + detectorNames[j] + "\t" + noise
										+ "\t" + (double)(driftsOccurred - 2 - fn)/((double)(driftsOccurred - 2)) + "\t" + (double)fp/((double)(fp + trueDriftsDetected)) 
										+ "\t" + delay + "\t" + (double)correct/((double)instCount) + "\t" + ((double)correct/((double)instCount)-baselineAccuracy)/driftsDetected
										+ "\t" + experimentRuntime + "\t" + driftsDetected + "\t" + trainingRuntime + "\t" + predictingRuntime + "\t" + classifyingRuntime 
										+ "\t" + baselineAccuracy + "\n" );
								
								resultsWriter.flush();
							}
						}
					}
				}
			}
		}
		memWriter.close();
		resultsWriter.close();
	}

}
