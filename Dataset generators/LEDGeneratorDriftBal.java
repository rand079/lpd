/* Code used for Lift-per-drift: an evaluation metric for classification frameworks with concept drift detection
 * 
 * This code is made available for research reproducability. For any other purposes, please contact the author first at rand079 at aucklanduni dot ac dot nz
 */

package moa.streams.generators;

import java.util.Random;

import com.github.javacliparser.FloatOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import com.yahoo.labs.samoa.instances.InstancesHeader;

import moa.core.InstanceExample;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class LEDGeneratorDriftBal extends LEDGeneratorDrift {

	/**
	 * 
	 */
	private static final long serialVersionUID = 4480153429694433396L;


	public void setMajorityClassProp(Double j){
		this.majorityClassProp = j;
	}

    // Majority class prop
    public FloatOption majorityClassPropOption = new FloatOption("majorityClassProp", 'j',
            "Proportion of instances of class 0",
            0.5, 0, 1);
    
	protected double majorityClassProp;
	protected Random rng = new Random();
	
    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
            ObjectRepository repository) {
        super.prepareForUseImpl(monitor, repository);
        this.numberAttribute = new int[7 + NUM_IRRELEVANT_ATTRIBUTES];
        for (int i = 0; i < 7 + NUM_IRRELEVANT_ATTRIBUTES; i++) {
            this.numberAttribute[i] = i;
        }
        //Change atributes
        if (!this.suppressIrrelevantAttributesOption.isSet() && this.numberAttributesDriftOption.getValue() > 0) {
            int randomInt = 0;//this.instanceRandom.nextInt(7);
            int offset = 0;//this.instanceRandom.nextInt(NUM_IRRELEVANT_ATTRIBUTES);
            for (int i = 0; i < this.numberAttributesDriftOption.getValue(); i++) {
                int value1 = (i + randomInt) % 7;
                int value2 = 7 + ((i + offset) % (NUM_IRRELEVANT_ATTRIBUTES));
                this.numberAttribute[value1] = value2;
                this.numberAttribute[value2] = value1;
            }
        }
        setMajorityClassProp(majorityClassPropOption.getValue());
        rng.setSeed(this.instanceRandomSeedOption.getValue()*3);
    }
	

    @Override
    public InstanceExample nextInstance() {
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setDataset(header);
        int selected = (rng.nextDouble() < this.majorityClassProp) ? 0 : this.instanceRandom.nextInt(9) + 1;
        for (int i = 0; i < 7; i++) {
            if ((1 + (this.instanceRandom.nextInt(100))) <= this.noisePercentageOption.getValue()) {
                inst.setValue(this.numberAttribute[i], originalInstances[selected][i] == 0 ? 1 : 0);
            } else {
                inst.setValue(this.numberAttribute[i], originalInstances[selected][i]);
            }
        }
        if (!this.suppressIrrelevantAttributesOption.isSet()) {
            for (int i = 0; i < NUM_IRRELEVANT_ATTRIBUTES; i++) {
                inst.setValue(this.numberAttribute[i + 7], this.instanceRandom.nextInt(2));
            }
        }
        inst.setClassValue(selected);
        return new InstanceExample(inst);
    }

}
