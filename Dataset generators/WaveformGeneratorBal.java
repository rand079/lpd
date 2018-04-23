/* Code used for Lift-per-drift: an evaluation metric for classification frameworks with concept drift detection
 * 
 * This code is made available for research reproducability. For any other purposes, please contact the author first at rand079 at aucklanduni dot ac dot nz
 */

package moa.streams.generators;

import java.util.Random;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.Attribute;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.Instances;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.FastVector;
import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.streams.InstanceStream;
import moa.tasks.TaskMonitor;

public class WaveformGeneratorBal extends WaveformGeneratorDrift{

	/**
	 * 
	 */
	private static final long serialVersionUID = -4445204244421134634L;

    // Majority class prop
    public FloatOption majorityClassPropOption = new FloatOption("majorityClassProp", 'j',
            "Proportion of instances of class 0",
            0.5, 0, 1);
	protected double majorityClassProp;
	protected double minorityClassProp;
	protected Random rng = new Random();
	

	public void setMajorityClassProp(Double j){
		this.majorityClassProp = j;
		this.minorityClassProp = (1 - j)/2;
	}

    
    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        super.prepareForUseImpl(monitor, repository);
        int numAtts = this.addNoiseOption.isSet() ? TOTAL_ATTRIBUTES_INCLUDING_NOISE
                : NUM_BASE_ATTRIBUTES;
        this.numberAttribute = new int[numAtts];
        for (int i = 0; i < numAtts; i++) {
            this.numberAttribute[i] = i;
        }
        //Change atributes
        int randomInt = this.instanceRandom.nextInt(numAtts);
        int offset = this.instanceRandom.nextInt(numAtts);
        int swap;
        for (int i = 0; i < this.numberAttributesDriftOption.getValue(); i++) {
            swap = this.numberAttribute[(i + randomInt) % numAtts];
            this.numberAttribute[(i + randomInt) % numAtts] = this.numberAttribute[(i + offset) % numAtts];
            this.numberAttribute[(i + offset) % numAtts] = swap;
        }
        
        setMajorityClassProp(majorityClassPropOption.getValue());
        rng.setSeed(this.instanceRandomSeedOption.getValue()*3);
    }

    @Override
    public InstanceExample nextInstance() {
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setDataset(header);
        double rand = rng.nextDouble();
        int waveform;
        if(rand < majorityClassProp)
        	waveform = 0;
        else if(rand < majorityClassProp + minorityClassProp)
        	waveform = 2;
        else
        	waveform = 1;
        int choiceA = 0, choiceB = 0;
        switch (waveform) {
            case 0:
                choiceA = 0;
                choiceB = 1;
                break;
            case 1:
                choiceA = 0;
                choiceB = 2;
                break;
            case 2:
                choiceA = 1;
                choiceB = 2;
                break;

        }
        double multiplierA = this.instanceRandom.nextDouble();
        double multiplierB = 1.0 - multiplierA;
        for (int i = 0; i < NUM_BASE_ATTRIBUTES; i++) {
            inst.setValue(this.numberAttribute[i], (multiplierA * hFunctions[choiceA][i])
                    + (multiplierB * hFunctions[choiceB][i])
                    + this.instanceRandom.nextGaussian());
        }
        if (this.addNoiseOption.isSet()) {
            for (int i = NUM_BASE_ATTRIBUTES; i < TOTAL_ATTRIBUTES_INCLUDING_NOISE; i++) {
                inst.setValue(this.numberAttribute[i], this.instanceRandom.nextGaussian());
            }
        }
        inst.setClassValue(waveform);
        return new InstanceExample(inst);
    }
    
}
