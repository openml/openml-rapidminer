package org.openml.rapidminer.operators;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.tools.Ontology;

public class BitshiftOperator extends Operator {

	private static Attribute combined = AttributeFactory.createAttribute("index_shifted", Ontology.INTEGER);
	
	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class );
	private OutputPort exampleSetOutput = getOutputPorts().createPort("exampleset");
	
	private final String[] shiftFields = {"run_id", "repeat", "fold", "sample"};
	private final Integer[] bitFields = { 16, 4, 4, 8};
	
	public BitshiftOperator(OperatorDescription description) {
		super(description);
		
		getTransformer().addRule(
			new ExampleSetPassThroughRule(exampleSetInput,
				exampleSetOutput, SetRelation.EQUAL) {
				@Override
				public ExampleSetMetaData modifyExampleSet(
					ExampleSetMetaData metaData) throws UndefinedParameterError {
					for( String field : shiftFields ) {
						metaData.removeAttribute( metaData.getAttributeByName( field ) );
					}
					metaData.addAttribute(new AttributeMetaData(combined));
					return metaData;
				}
			});
	}
	
	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Attributes attributes = exampleSet.getAttributes();
		List<Attribute> shiftAttributes = new ArrayList<Attribute>();
		for( String shiftField : shiftFields ) {
			shiftAttributes.add( attributes.get(shiftField) );
		}
		
		exampleSet.getAttributes().addRegular(combined);
		exampleSet.getExampleTable().addAttribute(combined);
		
		for( Example example : exampleSet ) {
			int shifted = 0;
			int bitsDone = 0;
			for( int i = 0; i < shiftAttributes.size(); ++i ) {
				int currentValue = (int) example.getValue(shiftAttributes.get(i));
				shifted += currentValue << bitsDone;
				bitsDone += bitFields[i];
			}
			example.setValue(combined, shifted);
		}
		
		for( Attribute attribute : shiftAttributes ) {
			exampleSet.getExampleTable().removeAttribute(attribute);
			exampleSet.getAttributes().remove(attribute);
		}
		
		exampleSetOutput.deliver(exampleSet);
	}

}
