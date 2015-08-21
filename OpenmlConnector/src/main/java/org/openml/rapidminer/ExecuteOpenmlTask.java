package org.openml.rapidminer;

import java.io.BufferedReader;
import java.util.LinkedList;
import java.util.List;

import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.algorithms.Input;
import org.openml.apiconnector.algorithms.TaskInformation;
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
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.OperatorChain;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
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
	
	public ExecuteOpenmlTask(OperatorDescription description) {
		super(description, "Task Execution");
		
		getTransformer().addGenerationRule(predictionSetOutput, OpenmlExecutedTask.class);
		getTransformer().addGenerationRule(trainingProcessExampleSetOutput, ExampleSet.class);
	}
	
	@Override
	public void doWork() throws OperatorException {
		try {
		//	Example example = exampleSet.getExample(0);
			Conversion.log( "OK", "Processfile", getProcess().toString() );
			OpenmlTask task = taskInput.getData(OpenmlTask.class);
			ExampleSet predictions = executeOpenmlTask( task );
			
			predictionSetOutput.deliver(new OpenmlExecutedTask(task.getTask().getTask_id(), predictions));
		} catch( Exception e ) {
			System.out.println("Exception: " + e.getMessage() );
			e.printStackTrace();
			throw new OperatorException(e.getMessage());
		}
	}
	
	private ExampleSet executeOpenmlTask( OpenmlTask openmlTask ) throws Exception {
		// TODO: make it work for regression tasks
		
		Data_set sourceData = TaskInformation.getSourceData(openmlTask.getTask());
		String splitsUrl = TaskInformation.getEstimationProcedure(openmlTask.getTask()).getData_splits_url();
		
		// TODO: update dependency correctly!
		Instances datasetOriginal = new Instances( new BufferedReader( Input.getURL( openmlTask.getDatasetDescription().getUrl() ) ) );
		datasetOriginal.insertAttributeAt(new weka.core.Attribute("rapidminer_row_id"), 0);
		for(int i = 0; i < datasetOriginal.size(); ++i) {
			datasetOriginal.get(i).setValue(0, i);
		}
		
		ExampleSet dataset = WekaTools.toRapidMinerExampleSet( datasetOriginal );
		dataset.getAttributes().setLabel(dataset.getAttributes().get(sourceData.getTarget_feature()));
		dataset.getAttributes().setId(dataset.getAttributes().get("rapidminer_row_id"));
		ExampleSet splits = WekaTools.toRapidMinerExampleSet( new Instances( new BufferedReader( Input.getURL( splitsUrl ) ) ) );
		
		int repeats = TaskInformation.getNumberOfRepeats(openmlTask.getTask());
		int folds = TaskInformation.getNumberOfFolds(openmlTask.getTask());
		int samples = 1;
		try { samples = TaskInformation.getNumberOfSamples(openmlTask.getTask()); } catch(Exception e) { }

		List<Attribute> predictionSetAttributes = predictionSetAttributes(dataset.getAttributes().getLabel());
		MemoryExampleTable table = new MemoryExampleTable(predictionSetAttributes);
		
		System.out.println(predictionSetAttributes);
		for( int r = 0; r < repeats; ++r ) {
			for( int f = 0; f < folds; ++f ) {
				for( int s = 0; s < samples; ++s ) {
					SplittedExampleSet splittedES = getSubsample(dataset, splits, r, f, s);
					// 1 means training, 2 means testing
					splittedES.selectSingleSubset(1);
					trainingProcessExampleSetOutput.deliver(splittedES);
					
					getSubprocess(0).execute();
					
					Model model = trainingProcessModelInput.getData(Model.class);
					splittedES.selectSingleSubset(2);
					
					ExampleSet results = model.apply(splittedES);
					
					for(int i = 0; i < results.size(); ++i ) {
						double[] data = new double[predictionSetAttributes.size()];
						data[0] = r;
						data[1] = f;
						data[2] = s;
						data[3] = results.getExample(i).getId();
						data[4] = results.getExample(i).getValue(results.getAttributes().get("prediction"));
						data[5] = results.getExample(i).getValue(results.getAttributes().getLabel());
						int j = 5;
						
						List<String> values = dataset.getAttributes().getLabel().getMapping().getValues();
						for(String value : values) {
							data[++j] = results.getExample(i).getValue(results.getAttributes().get("confidence_"+value));;
						}
						
						table.addDataRow(new DoubleArrayDataRow(data));
					}
				}
			}
		}
		return table.createExampleSet();
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
		
		Conversion.log("OK", "getSubsample", attRepeat.getName() );
		
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
		
		List<String> values = label.getMapping().getValues();
		for(String value : values) {
			attributes.add(AttributeFactory.createAttribute("confidence."+value, Ontology.NUMERICAL));
		}
		
		return attributes;
	}
}
