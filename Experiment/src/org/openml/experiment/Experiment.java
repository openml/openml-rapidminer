package org.openml.experiment;

public class Experiment {
	
	private int taskId;
	private int flowId;
	private int setupId;
	
	public Experiment(int taskId, int flowId, int setupId) {
		
		this.taskId = taskId;
		this.flowId = flowId;
		this.setupId = setupId;
		parseParameters();
		
	}
	
	private parseParameters()
}
