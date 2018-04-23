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
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class HyperplaneGeneratorBal extends HyperplaneGenerator{

	/**
	 * 
	 */
	private static final long serialVersionUID = 7894454498551416994L;
	protected double majorityClassProp;
	Random rng = new Random(); 
	
	public void setMajorityClassProp(Double j){
		this.majorityClassProp = j;
	}
	
    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        monitor.setCurrentActivity("Preparing hyperplane...", -1.0);
        generateHeader();
        restart();
        setMajorityClassProp(majorityClassPropOption.getValue());
        rng.setSeed(this.instanceRandomSeedOption.getValue()*3);
    }
    
    // Majority class prop
    public FloatOption majorityClassPropOption = new FloatOption("majorityClassProp", 'j',
            "Proportion of instances of class 0",
            0.5, 0, 1);
	

    @Override
    public InstanceExample nextInstance() {
    	boolean nextClassShouldBeZero = rng.nextDouble() < majorityClassProp;
        int numAtts = this.numAttsOption.getValue();
        double[] attVals = new double[numAtts + 1];
        boolean desiredClassFound = false;
        int classLabel = 0;
        while (!desiredClassFound) {
            double sum = 0.0;
            double sumWeights = 0.0;
	        for (int i = 0; i < numAtts; i++) {
	            attVals[i] = this.instanceRandom.nextDouble();
	            sum += this.weights[i] * attVals[i];
	            sumWeights += this.weights[i];
	        }
	        if (sum >= sumWeights * 0.5) {
	            classLabel = 1;
	        } else {
	            classLabel = 0;
	        }
            if ((nextClassShouldBeZero && (classLabel == 0))
                    || (!nextClassShouldBeZero && (classLabel == 1))) {
                desiredClassFound = true;
            } // else keep searching
        }
        //Add Noise
        if ((1 + (this.instanceRandom.nextInt(100))) <= this.noisePercentageOption.getValue()) {
            classLabel = (classLabel == 0 ? 1 : 0);
        }

        Instance inst = new DenseInstance(1.0, attVals);
        inst.setDataset(getHeader());
        inst.setClassValue(classLabel);
        addDrift();
        return new InstanceExample(inst);
    }
	
    private void addDrift() {
        for (int i = 0; i < this.numDriftAttsOption.getValue(); i++) {
            this.weights[i] += (double) ((double) sigma[i]) * ((double) this.magChangeOption.getValue());
            if (//this.weights[i] >= 1.0 || this.weights[i] <= 0.0 ||
                    (1 + (this.instanceRandom.nextInt(100))) <= this.sigmaPercentageOption.getValue()) {
                this.sigma[i] *= -1;
            }
        }
    }

}
