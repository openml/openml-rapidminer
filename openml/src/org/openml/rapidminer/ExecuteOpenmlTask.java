package org.openml.rapidminer;

import java.util.LinkedList;
import java.util.List;

import org.openml.apiconnector.algorithms.TaskInformation;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Settings;
import org.openml.apiconnector.xml.Task;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DoubleArrayDataRow;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.tools.Ontology;

public class ExecuteOpenmlTask extends Operator {
	
	private InputPort exampleSetInput = getInputPorts().createPort("example set");
	private OutputPort predictionSetOutput = getOutputPorts().createPort("prediction set");
	
	private OpenmlConnector openmlConnector;
	
	public ExecuteOpenmlTask(OperatorDescription description) {
		super(description);
		openmlConnector = new OpenmlConnector("http://www.openml.org/");
		Settings.API_VERBOSE_LEVEL = 2;
	}
	
	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Attributes attributes = exampleSet.getAttributes();
		Attribute taskIdList = attributes.get("task_id");
		
		try {
			Example example = exampleSet.getExample(0);
			ExampleSet current = executeOpenmlTask( (int) example.getNumericalValue(taskIdList) );
			
			predictionSetOutput.deliver(current);
		} catch( Exception e ) {
			System.out.println("Exception: " + e.getMessage() );
			e.printStackTrace();
		}
	}
	
	private ExampleSet executeOpenmlTask( int openmlTaskId ) throws Exception {
		List<Attribute> predictionSetAttributes = predictionSetAttributes();
		MemoryExampleTable table = new MemoryExampleTable(predictionSetAttributes);
		
		Task openmlTask = openmlConnector.openmlTaskSearch( openmlTaskId );
		
		int repeats = TaskInformation.getNumberOfRepeats(openmlTask);
		int folds = TaskInformation.getNumberOfFolds(openmlTask);
		int samples = TaskInformation.getNumberOfSamples(openmlTask);
		
		for( int r = 0; r < repeats; ++r ) {
			for( int f = 0; f < folds; ++f ) {
				for( int s = 0; s < samples; ++s ) {
					double[] data = new double[predictionSetAttributes.size()];
					data[0] = r;
					data[1] = f;
					data[2] = s;
					table.addDataRow(new DoubleArrayDataRow(data));
				}
			}
		}
		return table.createExampleSet();
	}
	
	private static List<Attribute> predictionSetAttributes() {
		List<Attribute> attributes = new LinkedList<Attribute>();
		attributes.add( AttributeFactory.createAttribute("repeat", Ontology.INTEGER) );
		attributes.add( AttributeFactory.createAttribute("fold", Ontology.INTEGER) );
		attributes.add( AttributeFactory.createAttribute("sample", Ontology.INTEGER) );
		return attributes;
	}
}
