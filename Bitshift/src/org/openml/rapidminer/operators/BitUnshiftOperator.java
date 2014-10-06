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

public class BitUnshiftOperator extends Operator {

	private static Attribute combined = AttributeFactory.createAttribute("index_shifted", Ontology.INTEGER);
	
	private InputPort exampleSetInput = getInputPorts().createPort("example set", ExampleSet.class);
	private OutputPort exampleSetOutput = getOutputPorts().createPort("exampleset");
	
	private final String[] unshiftFields = {"run_id", "repeat", "fold", "sample"};
	private final Integer[] bitFields = { 16, 4, 4, 8};

	public BitUnshiftOperator(OperatorDescription description) {
		super(description);
		
		getTransformer().addRule(
			new ExampleSetPassThroughRule(exampleSetInput,
				exampleSetOutput, SetRelation.EQUAL) {
				@Override
				public ExampleSetMetaData modifyExampleSet(
					ExampleSetMetaData metaData)
					throws UndefinedParameterError {
					for( String field : unshiftFields ) {
						metaData.addAttribute( new AttributeMetaData( AttributeFactory.createAttribute( field, Ontology.INTEGER ) ) );
					}
					metaData.removeAttribute(new AttributeMetaData(combined));
					return metaData;
				}
			});
	}
	
	@Override
	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exampleSetInput.getData(ExampleSet.class);
		Attributes attributes = exampleSet.getAttributes();
		Attribute index_shifted = attributes.get("index_shifted");
		List<Attribute> unshiftAttributes = new ArrayList<Attribute>();
		for( String unshiftField : unshiftFields ) {
			Attribute unshifted = AttributeFactory.createAttribute(unshiftField, Ontology.INTEGER);
			unshiftAttributes.add( unshifted );
			exampleSet.getAttributes().addRegular(unshifted);
			exampleSet.getExampleTable().addAttribute(unshifted);
		}
		
		for( Example example : exampleSet ) {
			int index_shifted_value = (int) example.getValue(index_shifted);
			for( int i = 0; i < unshiftAttributes.size(); ++i ) {
				example.setValue(unshiftAttributes.get(i), index_shifted_value % Math.pow(2, bitFields[i]));
				index_shifted_value >>= bitFields[i];
			}
		}
		
		exampleSet.getExampleTable().removeAttribute(index_shifted);
		exampleSet.getAttributes().remove(index_shifted);
		
		exampleSetOutput.deliver(exampleSet);
	}
}
