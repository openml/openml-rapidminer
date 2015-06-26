/*
 *  RapidMiner
 *
 *  Copyright (C) 2001-2009 by Rapid-I and the contributors
 *
 *  Complete list of developers available at our web site:
 *
 *       http://rapid-i.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools;

import java.util.Enumeration;
import java.util.Iterator;

import weka.core.DenseInstance;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.FastExample2SparseTransform;
import com.rapidminer.example.Statistics;
import com.rapidminer.operator.OperatorException;

/**
 * This class extends the Weka class Instances and overrides all methods needed
 * to directly use a RapidMiner {@link ExampleSet} as source for Weka instead of
 * copying the complete data.
 * 
 * @author Ingo Mierswa
 */
public class WekaInstancesAdaptor extends Instances {

	private static final long serialVersionUID = 99943154106235423L;

	public static final int LEARNING = 0;

	public static final int PREDICTING = 1;

	public static final int CLUSTERING = 2;

	public static final int ASSOCIATION_RULE_MINING = 3;

    public static final int WEIGHTING = 4;
    
	/**
	 * This enumeration implementation uses an ExampleReader (Iterator) to enumerate the
	 * instances.
	 */
	@SuppressWarnings("unchecked")
	private class InstanceEnumeration implements Enumeration {

		private Iterator<Example> reader;

		public InstanceEnumeration(Iterator<Example> reader) {
			this.reader = reader;
		}

		public Object nextElement() {
			return toWekaInstance(reader.next());
		}

		public boolean hasMoreElements() {
			return reader.hasNext();
		}
	}

	/** The example set which backs up the Instances object. */
	private ExampleSet exampleSet;

    /** This transformation might help to speed up the creation of sparse examples. */
    private transient FastExample2SparseTransform exampleTransform;
    
    /** The most frequent nominal values (only used for association rule mining, null otherwise).
     *  -1 if attribute is numerical. */
    private int[] mostFrequent = null;
    
	/**
	 * The task type for which this instances object is used. Must be one out of
	 * LEARNING, PREDICTING, CLUSTERING, ASSOCIATION_RULE_MINING, or WEIGHTING. For the
	 * latter cases the original label attribute will be omitted.
	 */
	private int taskType = LEARNING;

	/** The label attribute or null if not desired (depending on task). */
	private Attribute labelAttribute = null;

	/** The weight attribute or null if not available. */
	private Attribute weightAttribute = null;
	
	/** Creates a new Instances object based on the given example set. */
	public WekaInstancesAdaptor(String name, ExampleSet exampleSet, int taskType) throws OperatorException {
		super(name, getAttributeVector(exampleSet, taskType), 0);
		this.exampleSet = exampleSet;
		this.taskType = taskType;
		this.weightAttribute = exampleSet.getAttributes().getWeight();
        this.exampleTransform = new FastExample2SparseTransform(exampleSet);
		switch (taskType) {
			case LEARNING:
				labelAttribute = exampleSet.getAttributes().getLabel();
				setClassIndex(exampleSet.getAttributes().size());
				break;
			case PREDICTING:
				labelAttribute = exampleSet.getAttributes().getPredictedLabel();
				setClassIndex(exampleSet.getAttributes().size());
				break;
			case CLUSTERING:
				labelAttribute = null;
				setClassIndex(-1);
				break;
			case ASSOCIATION_RULE_MINING:
				// in case of association learning the most frequent attribute
				// is needed to set to "unknown"
				exampleSet.recalculateAllAttributeStatistics();
                this.mostFrequent = new int[exampleSet.getAttributes().size()];
                int i = 0;
                for (Attribute attribute : exampleSet.getAttributes()) {
                    if (attribute.isNominal()) {
                        this.mostFrequent[i] = (int)exampleSet.getStatistics(attribute, Statistics.MODE);
                    } else {
                        this.mostFrequent[i] = -1;
                    }
                    i++;
                }
                labelAttribute = null;
                setClassIndex(-1);
				break;
            case WEIGHTING:
                labelAttribute = exampleSet.getAttributes().getLabel();
                if (labelAttribute != null)
                    setClassIndex(exampleSet.getAttributes().size());
                break;
		}
	}

	protected Object readResolve() {
		try {
			this.exampleTransform = new FastExample2SparseTransform(this.exampleSet);
		} catch (OperatorException e) {
			// do nothing
		}
		return this;
	}
	
	// ================================================================================
	// Overriding some Weka methods
	// ================================================================================

	/** Returns an instance enumeration based on an ExampleReader. */
	@SuppressWarnings("unchecked")
	@Override
	public Enumeration enumerateInstances() {
		return new InstanceEnumeration(exampleSet.iterator());
	}

	/** Returns the i-th instance. */
	@Override
	public Instance instance(int i) {
		return toWekaInstance(exampleSet.getExample(i));
	}

	/** Returns the number of instances. */
	@Override
	public int numInstances() {
		return exampleSet.size();
	}

	// ================================================================================
	// Transforming examples into Weka instances
	// ================================================================================

	/** Gets an example and creates a Weka instance. */
	private Instance toWekaInstance(Example example) {
		int numberOfRegularValues = example.getAttributes().size();
		int numberOfValues = numberOfRegularValues + (labelAttribute != null ? 1 : 0);
		double[] values = new double[numberOfValues];

		// set regular attribute values
        if (taskType == ASSOCIATION_RULE_MINING) {
        	int a = 0;
        	for (Attribute attribute : exampleSet.getAttributes()) {
                double value = example.getValue(attribute);
                if (attribute.isNominal()) {
                    if (value == mostFrequent[a])
                        value = Double.NaN; 
                    // sets the most frequent value to missing
                    // for association learning
                }
                values[a] = value;
                a++;
            }            
        } else {
            int[] nonDefaultIndices = exampleTransform.getNonDefaultAttributeIndices(example);
            double[] nonDefaultValues = exampleTransform.getNonDefaultAttributeValues(example, nonDefaultIndices);
            for (int a = 0; a < nonDefaultIndices.length; a++) {
                values[nonDefaultIndices[a]] = nonDefaultValues[a];
            }
        }

		// set label value if necessary
		switch (taskType) {
			case LEARNING:
                values[values.length - 1] = example.getValue(labelAttribute); 
				break;
			case PREDICTING:
				values[values.length - 1] = Double.NaN;
				break;
            case WEIGHTING:
                if (labelAttribute != null)
                    values[values.length - 1] = example.getValue(labelAttribute);                 
                break;
			default:
				break;
		}

		// get instance weight
		double weight = 1.0d;
		if (this.weightAttribute != null)
			weight = example.getValue(this.weightAttribute);

		// create new instance
		Instance instance = new DenseInstance(weight, values);
		instance.setDataset(this);
		return instance;
	}

	// ================================================================================

	private static FastVector getAttributeVector(ExampleSet exampleSet, int taskType) {
		// determine label
		Attribute label = null;
		switch (taskType) {
			case LEARNING:
            case WEIGHTING:
				label = exampleSet.getAttributes().getLabel();
				break;
			case PREDICTING:
				label = exampleSet.getAttributes().getPredictedLabel();
				break;
			default:
				break;
		}

		// add regular attributes
		FastVector attributeVector = new FastVector(exampleSet.getAttributes().size() + (label != null ? 1 : 0));
		for (Attribute attribute : exampleSet.getAttributes()) {
			attributeVector.addElement(toWekaAttribute(attribute));
		}

		// add label
		if (label != null)
			attributeVector.addElement(toWekaAttribute(label));

		return attributeVector;
	}

	/** Converts an {@link Attribute} to a Weka attribute. */
	private static weka.core.Attribute toWekaAttribute(Attribute attribute) {
		if (attribute == null)
			return null;
		weka.core.Attribute result = null;
		if (Ontology.ATTRIBUTE_VALUE_TYPE.isA(attribute.getValueType(), Ontology.NOMINAL)) {
			FastVector nominalValues = new FastVector(attribute.getMapping().getValues().size());
			for (int i = 0; i < attribute.getMapping().getValues().size(); i++) {
				nominalValues.addElement(attribute.getMapping().mapIndex(i));
			}
			result = new weka.core.Attribute(attribute.getName(), nominalValues);
		} else {
			result = new weka.core.Attribute(attribute.getName());
		}
		return result;
	}
}
