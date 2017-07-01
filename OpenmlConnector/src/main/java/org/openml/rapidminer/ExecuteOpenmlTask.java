package org.openml.rapidminer;

import java.io.BufferedReader;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.Input;
import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.xml.EvaluationScore;
import org.openml.apiconnector.xml.Task.Input.Data_set;
import org.openml.rapidminer.models.OpenmlExecutedTask;
import org.openml.rapidminer.models.OpenmlTask;

import weka.core.Instances;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.Partition;
import com.rapidminer.example.set.SplittedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.preprocessing.MaterializeDataInMemory;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.WekaTools;

public class ExecuteOpenmlTask extends OperatorChain {
	
	// input
	protected InputPort taskInput = getInputPorts().createPort("task", OpenmlTask.class);
	
	// training
	protected final OutputPort trainingProcessExampleSetOutput = getSubprocess(0).getInnerSources().createPort("example set");
	protected final InputPort trainingProcessModelInput = getSubprocess(0).getInnerSinks().createPort("model", Model.class);
    
    // output
	protected OutputPort predictionSetOutput = getOutputPorts().createPort("prediction set");
	
	// misc
    protected final boolean canMeasureCPUTime;
    protected final ThreadMXBean thMonitor;
	
	public ExecuteOpenmlTask(OperatorDescription description) {
		super(description, "Task Execution");
		
		getTransformer().addGenerationRule(predictionSetOutput, OpenmlExecutedTask.class);
		getTransformer().addGenerationRule(trainingProcessExampleSetOutput, ExampleSet.class);
		
	    thMonitor = ManagementFactory.getThreadMXBean();
	    canMeasureCPUTime = thMonitor.isThreadCpuTimeSupported();
	    if (canMeasureCPUTime && !thMonitor.isThreadCpuTimeEnabled()) {
	      thMonitor.setThreadCpuTimeEnabled(true);
	    }
	}
	
	@Override
	public void doWork() throws OperatorException {
		try {
		//	Example example = exampleSet.getExample(0);
			Conversion.log( "OK", "Processfile", getProcess().toString() );
			OpenmlTask task = taskInput.getData(OpenmlTask.class);
			
			OpenmlExecutedTask executedTask = executeOpenmlTask( task );
			
			predictionSetOutput.deliver(executedTask);
		} catch( Exception e ) {
			System.out.println("Exception: " + e.getMessage() );
			e.printStackTrace();
			throw new OperatorException(e.getMessage() + " " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e));
		}
	}
	
	private OpenmlExecutedTask executeOpenmlTask( OpenmlTask openmlTask ) throws Exception {
		Data_set sourceData = TaskInformation.getSourceData(openmlTask.getTask());
		String splitsUrl = TaskInformation.getEstimationProcedure(openmlTask.getTask()).getData_splits_url();
		
		// TODO: update dependency correctly!
		Instances datasetOriginal = new Instances( new BufferedReader( Input.getURL( new URL(openmlTask.getDatasetDescription().getUrl() ) ) ) );
		datasetOriginal.insertAttributeAt(new weka.core.Attribute("rapidminer_row_id"), 0);
		for(int i = 0; i < datasetOriginal.size(); ++i) {
			datasetOriginal.get(i).setValue(0, i);
		}
		
		ExampleSet dataset = WekaTools.toRapidMinerExampleSet( datasetOriginal );
		dataset.getAttributes().setLabel(dataset.getAttributes().get(sourceData.getTarget_feature()));
		dataset.getAttributes().setId(dataset.getAttributes().get("rapidminer_row_id"));
		ExampleSet splits = WekaTools.toRapidMinerExampleSet( new Instances( new BufferedReader( Input.getURL( new URL(splitsUrl) ) ) ) );
		
		int repeats = TaskInformation.getNumberOfRepeats(openmlTask.getTask());
		int folds = TaskInformation.getNumberOfFolds(openmlTask.getTask());
		int samples = 1;
		try { samples = TaskInformation.getNumberOfSamples(openmlTask.getTask()); } catch(Exception e) { }

		List<EvaluationScore> evaluationMeasures = new ArrayList<EvaluationScore>();
		List<Attribute> predictionSetAttributes = predictionSetAttributes(dataset.getAttributes().getLabel());
		MemoryExampleTable table = new MemoryExampleTable(predictionSetAttributes);
		
		for( int r = 0; r < repeats; ++r ) {
			for( int f = 0; f < folds; ++f ) {
				for( int s = 0; s < samples; ++s ) {
					long threatID = Thread.currentThread().getId();
					long trainCPUStartTime = -1;
					Double trainCPUTimeElapsed = Double.NaN;
					long testCPUStartTime = -1;
					Double testCPUTimeElapsed = Double.NaN;
					
					
					SplittedExampleSet splittedES = getSubsample(dataset, splits, r, f, s);
					// 1 means training, 2 means testing
					splittedES.selectSingleSubset(1);
					// Materialize = clean copy
					ExampleSet trainingSet = MaterializeDataInMemory.materializeExampleSet(splittedES, DataRowFactory.TYPE_DOUBLE_ARRAY);
					splittedES.selectSingleSubset(2);
					// Materialize = clean copy
					ExampleSet testSet = MaterializeDataInMemory.materializeExampleSet(splittedES, DataRowFactory.TYPE_DOUBLE_ARRAY);
					
					trainingProcessExampleSetOutput.deliver(trainingSet);
					
					if (canMeasureCPUTime) {trainCPUStartTime = thMonitor.getThreadUserTime(threatID);}
					getSubprocess(0).execute();
					if (canMeasureCPUTime) {trainCPUTimeElapsed = new Double((thMonitor.getThreadUserTime(threatID) - trainCPUStartTime) / 1000000.0);}
					
					Model model = trainingProcessModelInput.getData(Model.class);
					

					if (canMeasureCPUTime) {testCPUStartTime = thMonitor.getThreadUserTime(threatID);}
					ExampleSet results = model.apply(testSet);
					if (canMeasureCPUTime) {testCPUTimeElapsed = new Double((thMonitor.getThreadUserTime(threatID) - testCPUStartTime) / 1000000.0);}
					
					for(int i = 0; i < results.size(); ++i ) {
						double[] data = new double[predictionSetAttributes.size()];
						data[0] = r;
						data[1] = f;
						data[2] = s;
						data[3] = results.getExample(i).getId();
						data[4] = results.getExample(i).getValue(results.getAttributes().get("prediction"));
						data[5] = results.getExample(i).getValue(results.getAttributes().getLabel());
						
						if (dataset.getAttributes().getLabel().isNominal()) {
							int j = 5;
							
							List<String> values = dataset.getAttributes().getLabel().getMapping().getValues();
							for(String value : values) {
								Double confidence = results.getExample(i).getValue(results.getAttributes().get("confidence_"+value));
								if (confidence.isNaN()) {
									data[++j] = 0;
								} else {
									data[++j] = confidence;
								}
								
							}
						}
						table.addDataRow(new DoubleArrayDataRow(data));
					}
					
					
					if(canMeasureCPUTime) {
						// TODO: revise mapping
						Integer samplenr = s;
						if (samples == 1) {
							samplenr = null;
						}
						
						EvaluationScore trainingTime = new EvaluationScore(
							"usercpu_time_millis_training",
							trainCPUTimeElapsed + "", "", 
							r, f, samplenr, trainingSet.size() );
						
						EvaluationScore testTime = new EvaluationScore(
							"usercpu_time_millis_testing",
							testCPUTimeElapsed + "", "", 
							r, f, samplenr, trainingSet.size() ); // training set size = ok
						
						EvaluationScore totalTime = new EvaluationScore(
							"usercpu_time_millis",
							(testCPUTimeElapsed + trainCPUTimeElapsed) + "", "", 
							r, f, samplenr, trainingSet.size() ); // training set size = ok
						
						
						
						evaluationMeasures.add(trainingTime);
						evaluationMeasures.add(testTime);
						evaluationMeasures.add(totalTime);
					}
				}
			}
		}
		
		OpenmlExecutedTask oet = new OpenmlExecutedTask(openmlTask.getTask().getTask_id(), table.createExampleSet(), evaluationMeasures);
		return oet;
	}
	
	private SplittedExampleSet getSubsample( ExampleSet dataset, ExampleSet splits, int repeat, int fold, int sample ) {
		// TODO: this does not support weighted sampling and train / test combinations
		int[] elements = new int[dataset.size()];
		
		Conversion.log("OK", "getSubsample", repeat + ", " + fold  + ", " + sample);
		
		Attributes attributes = splits.getAttributes();
		Attribute attRepeat = attributes.get("repeat");
		Attribute attFold = attributes.get("fold");
		Attribute attSample = attributes.get("sample");
		Attribute attRowid = attributes.get("rowid");
		Attribute attType = attributes.get("type");
		
		for (int i = 0; i < splits.size(); ++i ) {
			double instRepeat = splits.getExample(i).getValue(attRepeat);
			double instFold = splits.getExample(i).getValue(attFold);
			double instSample = 0;
			if(attSample != null) {
				instSample = splits.getExample(i).getValue(attSample);
			}

		//	Conversion.log("OK", "getSubsample", "FOUND " + instRepeat + ", " + instFold  + ", " + instSample);
			if (repeat == instRepeat && fold == instFold && sample == instSample) {
				int instRowid = (int) splits.getExample(i).getValue(attRowid);
				String instType = splits.getExample(i).getValueAsString(attType);
				
				if (instType.equals("TRAIN")) {
					elements[instRowid] = 1;
				} else {
					elements[instRowid] = 2;
				}
			} 
		}
		
		SplittedExampleSet splittedES = new SplittedExampleSet(dataset, new Partition(elements, 3) );
	
		return splittedES;
	}
	
	
	
	private static List<Attribute> predictionSetAttributes(Attribute label) {
		// TODO: make it work for regression
		List<Attribute> attributes = new LinkedList<Attribute>();
		Attribute prediction = (Attribute) label.clone();
		Attribute correct = (Attribute) label.clone();
		prediction.setName("prediction");
		correct.setName("correct");
		
		attributes.add(AttributeFactory.createAttribute("repeat", Ontology.INTEGER));
		attributes.add(AttributeFactory.createAttribute("fold", Ontology.INTEGER));
		attributes.add(AttributeFactory.createAttribute("sample", Ontology.INTEGER));
		attributes.add(AttributeFactory.createAttribute("row_id", Ontology.INTEGER));
		attributes.add(prediction);
		attributes.add(correct);
		
		if (label.isNominal()) {
			List<String> values = label.getMapping().getValues();
			for(String value : values) {
				attributes.add(AttributeFactory.createAttribute("confidence."+value, Ontology.NUMERICAL));
			}
		}
		return attributes;
	}
}
