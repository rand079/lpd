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

public class AgrawalGeneratorBal extends AgrawalGenerator{

    // Majority class prop
    public FloatOption majorityClassPropOption = new FloatOption("majorityClassProp", 'j',
            "Proportion of instances of class 0",
            0.5, 0, 1);
	
	protected double majorityClassProp;
    protected Random rng = new Random();
	
    @Override
    public void restart() {
        this.instanceRandom = new Random(this.instanceRandomSeedOption.getValue());
        setMajorityClassProp(majorityClassPropOption.getValue());
        rng.setSeed(this.instanceRandomSeedOption.getValue()*3);
        this.balanceClassesOption.set();
        this.nextClassShouldBeZero = false;
    }
    
	public void setMajorityClassProp(Double j){
		this.majorityClassProp = j;
	}
    
    @Override
    public InstanceExample nextInstance() {
        double salary = 0, commission = 0, hvalue = 0, loan = 0;
        int age = 0, elevel = 0, car = 0, zipcode = 0, hyears = 0, group = 0;
        this.nextClassShouldBeZero = rng.nextDouble() < majorityClassProp;
        boolean desiredClassFound = false;
        while (!desiredClassFound) {
            // generate attributes
            salary = 20000.0 + 130000.0 * this.instanceRandom.nextDouble();
            commission = (salary >= 75000.0) ? 0
                    : (10000.0 + 65000.0 * this.instanceRandom.nextDouble());
            // true to c implementation:
            // if (instanceRandom.nextDouble() < 0.5 && salary < 75000.0)
            // commission = 10000.0 + 65000.0 * instanceRandom.nextDouble();
            age = 20 + this.instanceRandom.nextInt(61);
            elevel = this.instanceRandom.nextInt(5);
            car = this.instanceRandom.nextInt(20);
            zipcode = this.instanceRandom.nextInt(9);
            hvalue = (9.0 - zipcode) * 100000.0
                    * (0.5 + this.instanceRandom.nextDouble());
            hyears = 1 + this.instanceRandom.nextInt(30);
            loan = this.instanceRandom.nextDouble() * 500000.0;
            // determine class
            group = classificationFunctions[this.functionOption.getValue() - 1].determineClass(salary, commission, age, elevel, car,
                    zipcode, hvalue, hyears, loan);
            if (!this.balanceClassesOption.isSet()) {
                desiredClassFound = true;
            } else {
                // balance the classes
                if ((this.nextClassShouldBeZero && (group == 0))
                        || (!this.nextClassShouldBeZero && (group == 1))) {
                    desiredClassFound = true;
                } // else keep searching
            }
        }
        // perturb values
        if (this.peturbFractionOption.getValue() > 0.0) {
            salary = perturbValue(salary, 20000, 150000);
            if (commission > 0) {
                commission = perturbValue(commission, 10000, 75000);
            }
            age = (int) Math.round(perturbValue(age, 20, 80));
            hvalue = perturbValue(hvalue, (9.0 - zipcode) * 100000.0, 0, 135000);
            hyears = (int) Math.round(perturbValue(hyears, 1, 30));
            loan = perturbValue(loan, 0, 500000);
        }
        // construct instance
        InstancesHeader header = getHeader();
        Instance inst = new DenseInstance(header.numAttributes());
        inst.setValue(0, salary);
        inst.setValue(1, commission);
        inst.setValue(2, age);
        inst.setValue(3, elevel);
        inst.setValue(4, car);
        inst.setValue(5, zipcode);
        inst.setValue(6, hvalue);
        inst.setValue(7, hyears);
        inst.setValue(8, loan);
        inst.setDataset(header);
        inst.setClassValue(group);
        return new InstanceExample(inst);
    }
}
