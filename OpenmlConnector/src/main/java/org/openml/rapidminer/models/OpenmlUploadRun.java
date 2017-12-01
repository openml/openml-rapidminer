package org.openml.rapidminer.models;

import org.openml.apiconnector.xml.UploadRun;

import com.rapidminer.operator.ResultObjectAdapter;

public class OpenmlUploadRun extends ResultObjectAdapter{

	private static final long serialVersionUID = 4579412591607961239L;
	protected final int runId;
	
	public OpenmlUploadRun(UploadRun uploadRun) {
		this.runId = uploadRun.getRun_id();
	}
	
	public int getRunId() {
		return runId;
	}
	
	@Override
	public String toString() {
		return "" + runId;
	}
}