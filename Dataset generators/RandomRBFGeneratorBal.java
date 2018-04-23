/* Code used for Lift-per-drift: an evaluation metric for classification frameworks with concept drift detection
 * 
 * This code is made available for research reproducability. For any other purposes, please contact the author first at rand079 at aucklanduni dot ac dot nz
 */

package moa.streams.generators;

import java.util.Random;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;

import moa.core.InstanceExample;
import moa.core.MiscUtils;
import moa.core.ObjectRepository;
import moa.streams.generators.RandomRBFGenerator.Centroid;
import moa.tasks.TaskMonitor;

public class RandomRBFGeneratorBal extends RandomRBFGenerator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2711164277221299798L;

	public void setMajorityClassProp(Double j){
		this.majorityClassProp = j;
	}

    // Majority class prop
    public FloatOption majorityClassPropOption = new FloatOption("majorityClassProp", 'j',
            "Proportion of instances of class 0",
            0.5, 0, 1);
    
	protected double majorityClassProp;
	
    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
        setMajorityClassProp(majorityClassPropOption.getValue());
    }
    
    protected void generateCentroids() {
        Random modelRand = new Random(this.modelRandomSeedOption.getValue());
        this.centroids = new Centroid[this.numCentroidsOption.getValue()];
        this.centroidWeights = new double[this.centroids.length];
        //code for implementing imbalance included below
        double majorityWeights = 0.0;
        double otherWeights = 0.0;
        for (int i = 0; i < this.centroids.length; i++) {
            this.centroids[i] = new Centroid();
            double[] randCentre = new double[this.numAttsOption.getValue()];
            for (int j = 0; j < randCentre.length; j++) {
                randCentre[j] = modelRand.nextDouble();
            }
            this.centroids[i].centre = randCentre;
            this.centroids[i].classLabel = modelRand.nextInt(this.numClassesOption.getValue());
            this.centroids[i].stdDev = modelRand.nextDouble();
            this.centroidWeights[i] = modelRand.nextDouble();
            if (this.centroids[i].classLabel == 0) majorityWeights += this.centroidWeights[i];
            else otherWeights += this.centroidWeights[i];
        }
        double multiplier = (this.majorityClassProp*otherWeights) / (majorityWeights-this.majorityClassProp*majorityWeights);
        for(int i = 0; i < this.centroids.length; i++){
        	if(this.centroids[i].classLabel == 0) this.centroidWeights[i] = this.centroidWeights[i] * multiplier;
        }
    }
}
