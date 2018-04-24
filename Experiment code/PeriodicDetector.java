package moa.classifiers.core.driftdetection;

import com.github.javacliparser.IntOption;

import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

public class PeriodicDetector extends AbstractChangeDetector {

	public IntOption driftFreqOption = new IntOption(
	"driftFreq",
	'd',
	"Signals drift after d inputs",
	10000, 1, Integer.MAX_VALUE);
	
	public IntOption warningZoneSizeOption = new IntOption(
	"warningZoneSize",
	'w',
	"Will start warning at driftFreq periods, and change will only be detected once this zone is complete",
	59, 0, Integer.MAX_VALUE);
	
	int instCount = 0;
	int driftFreq = 0;
	int warningZoneSize = 0;
	
	@Override
	public void input(double inputValue) {
		// TODO Auto-generated method stub
		//System.out.println(instCount);
		instCount++;
		if(instCount > warningZoneSize + 1){
			int instSinceDrift = instCount%driftFreq;
			this.isWarningZone = (0 <= instSinceDrift & instSinceDrift < warningZoneSize);
			this.isChangeDetected = (instSinceDrift == warningZoneSize);
			//System.out.println("instance " + instCount + "change " + this.isChangeDetected + "warning: " + this.isWarningZone);
		}
	}

	@Override
	public void getDescription(StringBuilder sb, int indent) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
		int instCount = 0;
		driftFreq = driftFreqOption.getValue();
		warningZoneSize = warningZoneSizeOption.getValue();
	}

}
