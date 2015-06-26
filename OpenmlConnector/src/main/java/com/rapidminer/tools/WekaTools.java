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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.jar.JarFile;
import java.util.logging.Level;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.example.table.DataRow;
import com.rapidminer.example.table.DataRowFactory;
import com.rapidminer.example.table.ListDataRowReader;
import com.rapidminer.example.table.MemoryExampleTable;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCreationException;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeString;
import com.rapidminer.tools.plugin.Plugin;

/**
 * This class contains static methods for converting <a
 * href="http://www.cs.waikato.ac.nz/ml/weka/">Weka</a> Instances to RapidMiner
 * ExampleSet and vice versa.
 * 
 * @author Ingo Mierswa
 */
public class WekaTools {

	/** This prefix indicates all Weka operators. It also allows RapidMiner operators with the
	 *  same name. */
	public static final String WEKA_OPERATOR_PREFIX = "W-";

	// ================================================================================
	// Conversion: Weka Instances --> RapidMiner ExampleSet
	// ================================================================================

	/**
	 * Invokes toRapidMinerExampleSet(instances, null,
	 * DataRowFactory.TYPE_DOUBLE_ARRAY).
	 */
	public static ExampleSet toRapidMinerExampleSet(Instances instances) {
		return toRapidMinerExampleSet(instances, null, DataRowFactory.TYPE_DOUBLE_ARRAY);
	}

	/**
	 * Invokes toRapidMinerExampleSet(instances, attributeNamePrefix,
	 * DataRowFactory.TYPE_DOUBLE_ARRAY).
	 */
	public static ExampleSet toRapidMinerExampleSet(Instances instances, String attributeNamePrefix) {
		return toRapidMinerExampleSet(instances, attributeNamePrefix, DataRowFactory.TYPE_DOUBLE_ARRAY);
	}

	/**
	 * Creates a RapidMiner example set from Weka instances. Only a label can be used
	 * as special attributes, other types of special attributes are not
	 * supported. If <code>attributeNamePrefix</code> is not null, the given
	 * string prefix plus a number is used as attribute names.
	 */
	public static ExampleSet toRapidMinerExampleSet(Instances instances, String attributeNamePrefix, int datamanagement) {
		int classIndex = instances.classIndex();

		// create example table

		// 1. Extract attributes
		List<Attribute> attributes = new ArrayList<Attribute>();
		int number = 1; // use for attribute names
		for (int i = 0; i < instances.numAttributes(); i++) {
			weka.core.Attribute wekaAttribute = instances.attribute(i);
			int rapidMinerAttributeValueType = Ontology.REAL;
			if (wekaAttribute.isNominal())
				rapidMinerAttributeValueType = Ontology.NOMINAL;	
			else if (wekaAttribute.isString())
				rapidMinerAttributeValueType = Ontology.STRING;
			Attribute attribute = AttributeFactory.createAttribute(wekaAttribute.name(), rapidMinerAttributeValueType);
			if ((i != classIndex) && (attributeNamePrefix != null) && (attributeNamePrefix.length() > 0)) {
				attribute.setName(attributeNamePrefix + "_" + (number++));
			}
			if (wekaAttribute.isNominal()) {
				for (int a = 0; a < wekaAttribute.numValues(); a++) {
					String nominalValue = wekaAttribute.value(a);
					attribute.getMapping().mapString(nominalValue);
				}
			}
			attributes.add(attribute);
		}

		Attribute label = null;
		if (classIndex >= 0) {
			label = attributes.get(classIndex);
			label.setName("label");
		}

		// 2. Guarantee alphabetical mapping to numbers
		for (int j = 0; j < attributes.size(); j++) {
			Attribute attribute = attributes.get(j);
			if (attribute.isNominal())
				attribute.getMapping().sortMappings();
		}

		// 3. Read data
		MemoryExampleTable table = new MemoryExampleTable(attributes);
		DataRowFactory factory = new DataRowFactory(datamanagement, '.');
		// create data
		List<DataRow> dataList = new LinkedList<DataRow>();
		int numberOfRapidMinerAttributes = instances.numAttributes();
		for (int i = 0; i < instances.numInstances(); i++) {
			Instance instance = instances.instance(i);
			DataRow dataRow = factory.create(numberOfRapidMinerAttributes);
			for (int a = 0; a < instances.numAttributes(); a++) {
				Attribute attribute = table.getAttribute(a);
				double wekaValue = instance.value(a);
				if (attribute.isNominal()) {
					String nominalValue = instances.attribute(a).value((int) wekaValue);
					dataRow.set(attribute, attribute.getMapping().mapString(nominalValue));
				} else {
					dataRow.set(attribute, wekaValue);
				}
			}
			dataRow.trim();
			dataList.add(dataRow);
		}

		// handle label extra
		table.readExamples(new ListDataRowReader(dataList.iterator()));

		// create and return example set
		return table.createExampleSet(label);
	}

	// ================================================================================
	// Conversion: RapidMiner ExampleSet --> Weka Instances
	// ================================================================================

	/**
	 * Creates Weka instances with the given name from the given example set.
	 * The taskType defines for which task the instances object should be used.
	 */
	public static Instances toWekaInstances(ExampleSet exampleSet, String name, int taskType) throws OperatorException {
		return new WekaInstancesAdaptor(name, exampleSet, taskType);
	}

	// ================================================================================
	// Parameter handling
	// ================================================================================

	/** Returns the Weka parameters for a RapidMiner parameter list. */
	public static String[] getWekaParametersFromList(List<Object[]> rapidMinerParameters) {
		String[] parameters = new String[rapidMinerParameters.size() * 2];
		Iterator<Object[]> i = rapidMinerParameters.iterator();
		int j = 0;
		while (i.hasNext()) {
			Object[] parameter = i.next();
			parameters[j++] = "-" + (String) parameter[0];
			parameters[j++] = (String) parameter[1];
		}
		return parameters;
	}

	/**
	 * Returns all Weka parameters as String array from the given list of
	 * parameter types.
	 */
	public static String[] getWekaParametersFromTypes(Operator operator, List<ParameterType> parameterTypes) {
		List<String> parameterStrings = new LinkedList<String>();
		Iterator<ParameterType> i = parameterTypes.iterator();
		while (i.hasNext()) {
			ParameterType type = i.next();
			try {
				if (type instanceof ParameterTypeBoolean) {
					if (!(Boolean.valueOf(operator.getParameterAsBoolean(type.getKey())).equals(type.getDefaultValue())))
						parameterStrings.add("-" + type.getKey());
				} else if (type instanceof ParameterTypeDouble) {
					double value = operator.getParameterAsDouble(type.getKey());
					if (!Double.isNaN(value)) {
						double defaultValue = (Double)type.getDefaultValue();
						if ((Double.isNaN(defaultValue)) || (defaultValue != value)) {
							parameterStrings.add("-" + type.getKey());
							String valueString = Tools.formatIntegerIfPossible(value);
							parameterStrings.add(valueString);
						}
					}                    
				} else {
					String value = operator.getParameterAsString(type.getKey());
					if (value != null) {
						String defaultValue = (String) type.getDefaultValue();
						if ((defaultValue == null) || (!defaultValue.equals(value))) {
							parameterStrings.add("-" + type.getKey());
							if (value.indexOf("--") >= 0) {
								StringTokenizer tokenizer = new StringTokenizer(value);
								while (tokenizer.hasMoreTokens()) {
									String token = tokenizer.nextToken();
									parameterStrings.add(token);
								}
							} else {
								parameterStrings.add(value);
							}
						}
					}
				}
			} catch (Exception e) {
				throw new RuntimeException("Cannot use parameter " + type.getKey() + ": " + e.getMessage());
			}
		}

		String[] result = new String[parameterStrings.size()];
		parameterStrings.toArray(result);
		return result;
	}

	/**
	 * Tries to guess the type of the given option. If the number of arguments
	 * is zero, than a boolean type is assumed. In other cases it will be tried
	 * to parse the default value in the options array as a number and on
	 * success a Double type is returned. If this fails, a ParameterTypeString
	 * is returned.
	 */
	public static ParameterType guessParameterType(Option option, String[] options) {
		if (option.numArguments() == 0) {
			String defaultString = getStringDefault(option.name(), options);
			if (defaultString == null) {
				return new ParameterTypeBoolean(option.name(), option.description(), getBooleanDefault(option.name(), options));
			} else {
				return new ParameterTypeString(option.name(), option.description(), defaultString);
			}
		} else {
			String defaultString = getStringDefault(option.name(), options);
			if (defaultString == null) {
				return new ParameterTypeString(option.name(), option.description());
			} else {
				try {
					double defaultValue = Double.parseDouble(defaultString);
					return new ParameterTypeDouble(option.name(), option.description(), Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, defaultValue);
				} catch (NumberFormatException e) {
					return new ParameterTypeString(option.name(), option.description(), defaultString);
				}
			}
		}
	}

	/** Returns the default value for a boolean parameter. */
	private static boolean getBooleanDefault(String key, String[] options) {
		for (int i = 0; i < options.length; i++) {
			if (options[i].equals("-" + key))
				return true;
		}
		return false;
	}

	/** Returns the default value for a string parameter. */
	private static String getStringDefault(String key, String[] options) {
		for (int i = 0; i < options.length; i++) {
			if ((options[i].equals("-" + key)) && (i + 1 < options.length))
				return options[i + 1];
		}
		return null;
	}

	/**
	 * Removes all parameters from the given Weka options which are part of the
	 * inner learner of a meta learning scheme.
	 */
	/*
	private static String[] removeMetaOptions(String[] options) {
		int index = -1;
		for (int i = 0; i < options.length; i++) {
			if (options[i].trim().equals("--")) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return options;
		} else {
			String[] result = new String[index];
			System.arraycopy(options, 0, result, 0, index);
			return result;
		}
	}
	 */

	/** Add the parameter type for the options of a Weka option handler. */
	@SuppressWarnings("unchecked")
	public static void addParameterTypes(OptionHandler handler, List<ParameterType> types, List<ParameterType> wekaParameters, boolean meta, String metaParameter) {
		//String[] defaultOptions = removeMetaOptions(handler.getOptions());
		String[] defaultOptions = handler.getOptions();
		Enumeration<Option> options = handler.listOptions();
		while (options.hasMoreElements()) {
			Option option = options.nextElement();
			if (option.name().trim().length() == 0)
				break; // necessary to prevent adding of parameters of children
			// of meta learners

			// prevent adding the meta learning scheme options
			if (meta && option.name().trim().toLowerCase().equals(metaParameter.toLowerCase())) {
				continue;
			}

			ParameterType type = guessParameterType(option, defaultOptions);
			type.setShowRange(false); // do not show the range of Weka parameters
			type.setExpert(false); // all Weka paras as non expert paras
			types.add(type);
			wekaParameters.add(type);
		}
	}

	// ================================================================================
	// Misc
	// ================================================================================

	public static String[] getWekaClasses(JarFile jar, Class<?> superclass) {
		return getWekaClasses(jar, superclass, (String)null, true);
	}

	public static String[] getWekaClasses(JarFile jar, Class<?> superclass, String seachConstraint, boolean includeConstraint) {
		if (seachConstraint != null)
			return getWekaClasses(jar, superclass, new String[] { seachConstraint }, includeConstraint);
		else
			return getWekaClasses(jar, superclass, (String[])null, includeConstraint);
	}

	public static String[] getWekaClasses(JarFile jar, Class<?> superclass, String[] searchConstraints, boolean positive) {
		if (positive) {
			return getWekaClasses(jar, superclass, searchConstraints, null);
		} else {
			return getWekaClasses(jar, superclass, null, searchConstraints);
		}
	}

	/** If Weka is not found, this method silently returns an empty string array. */
	public static String[] getWekaClasses(JarFile jarFile, Class<?> superclass, String[] positiveSearchConstraints, String[] negativeSearchConstraints) {
		List<String> classes = new LinkedList<String>();

		Tools.findImplementationsInJar(WekaTools.class.getClassLoader(), jarFile, superclass, classes);
		Iterator<String> i = classes.iterator();
		while (i.hasNext()) {
			String name = i.next();
			boolean removed = false;
			if (positiveSearchConstraints != null) {
				boolean shouldRemove = true;
				for (String constraint : positiveSearchConstraints) {
					if (name.indexOf(constraint) != -1) {
						shouldRemove = false;
						break;
					}
				}
				if (shouldRemove) {
					i.remove();
					removed = true;
				}
			}
			if ((!removed) && (negativeSearchConstraints != null)) {
				for (String constraint : negativeSearchConstraints) {
					if (name.indexOf(constraint) != -1) {
						i.remove();
						break;
					}
				}
			} 
		}
		String[] names = new String[classes.size()];
		classes.toArray(names);
		return names;
	}

	/**
	 * Registers all given Weka operators. The parameter firstDescription will
	 * be prepended to the name and firstGroup should have its last point if the
	 * last package should form a subgroup and no ending point if the given
	 * group should be definitely the group of the operators. Invokes the method
	 * without deprecated operators.
	 */
	public static void registerWekaOperators(ClassLoader classLoader, String[] classNames, String operatorClass, String firstDescription, String firstGroup, String icon, Plugin plugin) {
		registerWekaOperators(classLoader, classNames, new HashMap<String,String>(), operatorClass, firstDescription, firstGroup, icon, plugin);
	}

	/**
	 * Registers all given Weka operators. The parameter firstDescription will
	 * be prepended to the name and firstGroup should have its last point if the
	 * last package should form a subgroup and no ending point if the given
	 * group should be definitely the group of the operators.
	 */
	@SuppressWarnings("deprecation")
	public static void registerWekaOperators(ClassLoader classLoader, String[] classNames, Map<String,String> deprecationInfos, String operatorClass, String firstDescription, String firstGroup, String icon, Plugin plugin) {
		for (int i = 0; i < classNames.length; i++) {
			String infoString = null;
			try {
				Class<?> clazz = Class.forName(classNames[i], true, classLoader);
				Object wekaObject = clazz.newInstance();
				Method method = clazz.getMethod("globalInfo", new Class[0]);
				infoString = (String)method.invoke(wekaObject, new Object[0]);

				if (infoString != null) {		
					// replaces ampers and by a word, necessary for automatic doc generation
					// infoString.replaceAll("&", "and"); --> does not work here!
					infoString = htmlEscape(infoString, "&", "and");
					infoString = htmlEscape(infoString, "_", "");
					infoString = htmlEscape(infoString, "#", "number");

					// remove physical markup
					infoString = htmlEscape(infoString, "<i>", "");
					infoString = htmlEscape(infoString, "</i>", "");
					infoString = htmlEscape(infoString, "<b>", "");
					infoString = htmlEscape(infoString, "</b>", "");
					infoString = htmlEscape(infoString, "<tt>", "");
					infoString = htmlEscape(infoString, "</tt>", "");
					infoString = htmlEscape(infoString, "<num>", "");
					infoString = htmlEscape(infoString, "</num>", "");

					// if the description probably contains some formula which
					// cannot be used in documentation or tooltips
					// --> use only the first sentence which probably does not
					// contain the formula
					// if the first contains another "^" discard the description...
					if (infoString.indexOf("^") >= 0) {
						infoString = infoString.substring(0, infoString.indexOf(".") + 1).trim();
					}
					if (infoString.indexOf("^") >= 0) {
						infoString = null; // still contains formula (probably) -->
						// discard info text
					}
				} else { 
					LogService.getRoot().warning("Delivered infoString from Weka is empty for '" + classNames[i] + "': using default short description.");
				}
			} catch (ClassNotFoundException e) { 
				// cannot create info string from weka -->
				// use simple description
				LogService.getRoot().log(Level.WARNING, "Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			} catch (InstantiationException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			} catch (IllegalAccessException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			} catch (ExceptionInInitializerError e) {
				LogService.getRoot().log(Level.WARNING, "Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			} catch (NoClassDefFoundError e) {
				LogService.getRoot().log(Level.WARNING, "Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			} catch (SecurityException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			} catch (NoSuchMethodException e) {
				// no global info method? Do nothing but simply use simple description
				//LogService.logMessage("Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			} catch (IllegalArgumentException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			} catch (InvocationTargetException e) {
				LogService.getRoot().log(Level.WARNING, "Cannot retrieve operator information from Weka for '"+classNames[i] + "': " +e, e);
			}

			int lastIndex = classNames[i].lastIndexOf(".");
			String name = WEKA_OPERATOR_PREFIX + classNames[i].substring(lastIndex + 1);
			String packageName = classNames[i].substring(0, lastIndex);
			String group = packageName.substring(packageName.lastIndexOf(".") + 1);
			String groupStart = group.substring(0, 1);
			String groupEnd = group.substring(1);
			group = groupStart.toUpperCase() + groupEnd.toLowerCase();

			try {
				String deprecationInfo = null;
				if (deprecationInfos != null)
					deprecationInfo = deprecationInfos.get(classNames[i]);
				String shortDescription = null;
				String longDescription = null;
				if (infoString != null) {
					int pointIndex = infoString.indexOf('.');
					if (pointIndex >= 0) {
						String shortCandidate = infoString.substring(0, pointIndex + 1);
						if (shortCandidate.length() > 10) {
							shortDescription = shortCandidate;
							longDescription = infoString;
						} else {
							shortDescription = firstDescription.trim() + " " + name;
							longDescription = infoString;
						}
					} else {
						shortDescription = firstDescription.trim() + " " + name;
						longDescription = infoString;						
					}
				} else {
					shortDescription = firstDescription.trim() + " " + name;
					longDescription = firstDescription.trim() + " " + name;
				}

				String operatorGroup = firstGroup.endsWith(".") ? firstGroup + group : firstGroup;
				OperatorDescription description = new OperatorDescription(classLoader, name, name, operatorClass, shortDescription, longDescription, operatorGroup, icon, deprecationInfo, plugin);
				description.setIsReplacementFor(name);
				
				// ====================================================================
				//    TODO: add the following command for testing new Weka versions !!!
				// ====================================================================
				//description.createOperatorInstance();

				// no error? --> register...
				OperatorService.registerOperator(description);
			} catch (OperatorCreationException e) {
				// RapidMiner problems --> report
				LogService.getRoot().log(Level.WARNING, "Cannot construct operator '" + name + "', error: " + e.getMessage(), e);
			} catch (Exception t) {
				// weka problems --> do nothing
				LogService.getRoot().log(Level.WARNING, "Cannot register operator '" + name + "', cause: " + t.getMessage(), t);
			}
		}
	}

	private static String htmlEscape(String toEscape, String what, String by) {
		String result = toEscape;
		int index = 0;
		int generalIndex = 0;
		while ((index = result.indexOf(what, generalIndex)) >= 0) {
			String first = result.substring(0, index);
			String last = result.substring(index + what.length());
			result = first + by + last;
			generalIndex = index + by.length();
		}
		return result;
	}
}
