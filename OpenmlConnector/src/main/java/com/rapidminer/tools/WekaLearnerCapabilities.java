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

import weka.classifiers.Classifier;
import weka.classifiers.UpdateableClassifier;
import weka.classifiers.misc.SerializedClassifier;
import weka.core.Capabilities;
import weka.core.WeightedInstancesHandler;

import com.rapidminer.operator.OperatorCapability;

/**
 * Checks if a given classifier supports the desired capability.
 * 
 * @author Ingo Mierswa
 */
public class WekaLearnerCapabilities {

	public static boolean supportsCapability(Classifier classifier, OperatorCapability lc) {
		if (!(classifier instanceof SerializedClassifier)) {
			Capabilities capabilities = classifier.getCapabilities();

			if (lc == OperatorCapability.POLYNOMINAL_ATTRIBUTES) {
				return capabilities.handles(Capabilities.Capability.NOMINAL_ATTRIBUTES);
			} else if (lc == OperatorCapability.BINOMINAL_ATTRIBUTES) {
				return capabilities.handles(Capabilities.Capability.BINARY_ATTRIBUTES);
			} else if (lc == OperatorCapability.NUMERICAL_ATTRIBUTES) {
				return capabilities.handles(Capabilities.Capability.NUMERIC_ATTRIBUTES);
			} else if (lc == OperatorCapability.POLYNOMINAL_LABEL) {
				return capabilities.handles(Capabilities.Capability.NOMINAL_CLASS);
			} else if (lc == OperatorCapability.BINOMINAL_LABEL) {
				return capabilities.handles(Capabilities.Capability.BINARY_CLASS);
			} else if (lc == OperatorCapability.NUMERICAL_LABEL) {
				return capabilities.handles(Capabilities.Capability.NUMERIC_CLASS);
			} else if (lc == OperatorCapability.UPDATABLE) {
				return (classifier instanceof UpdateableClassifier);
			} else if (lc == OperatorCapability.WEIGHTED_EXAMPLES) {
				return (classifier instanceof WeightedInstancesHandler);
			}
		}
		return false;
	}
}
