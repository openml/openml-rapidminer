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

import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.logging.Level;

import weka.attributeSelection.AttributeEvaluator;
import weka.clusterers.Clusterer;

import com.rapidminer.tools.plugin.Plugin;

/**
 * Registers all Weka core operators.
 * 
 * @author Ingo Mierswa
 */
public class WekaOperatorFactory implements GenericOperatorFactory {

	public static String[] WEKA_ASSOCIATORS = new String[0];
	public static String[] WEKA_ATTRIBUTE_EVALUATORS = new String[0];
    public static String[] WEKA_META_CLASSIFIERS = new String[0]; 
	public static String[] WEKA_CLASSIFIERS = new String[0];
	public static String[] WEKA_CLUSTERERS = new String[0];
	
	private static final String[] SKIPPED_META_CLASSIFIERS = new String[] { "weka.classifiers.meta.AttributeSelectedClassifier", "weka.classifiers.meta.CVParameterSelection", "weka.classifiers.meta.ClassificationViaRegression", "weka.classifiers.meta.FilteredClassifier", "weka.classifiers.meta.MultiScheme", "weka.classifiers.meta.Vote",
			"weka.classifiers.meta.Grading", "weka.classifiers.meta.Stacking", "weka.classifiers.meta.StackingC", "weka.classifiers.meta.RotationForest$ClassifierWrapper" };

	private static final String[] SKIPPED_CLASSIFIERS = new String[] { ".meta.", ".pmml.", "weka.classifiers.functions.LibSVM", "MISVM", "UserClassifier", "LMTNode", "PreConstructedLinearModel", "RuleNode", "FTInnerNode", "FTLeavesNode", "FTNode", "weka.classifiers.functions.LibLINEAR" };

	private static final String[] SKIPPED_CLUSTERERS = new String[] { "weka.clusterers.FilteredClusterer", "weka.clusterers.OPTICS", "weka.clusterers.DBScan", "weka.clusterers.MakeDensityBasedClusterer" };

	private static final String[] SKIPPED_ASSOCIATORS = new String[] { "FilteredAssociator" };

	private static final String[] ENSEMBLE_CLASSIFIERS = new String[] { "weka.classifiers.meta.MultiScheme", "weka.classifiers.meta.Vote", "weka.classifiers.meta.Grading", "weka.classifiers.meta.Stacking", "weka.classifiers.meta.StackingC" };

	private static final Map<String, String> DEPRECATED_CLASSIFIER_INFOS = new HashMap<String, String>();

	static {
		DEPRECATED_CLASSIFIER_INFOS.put("weka.classifiers.bayes.NaiveBayesSimple", "Deprecated: please use NaiveBayes instead.");
		DEPRECATED_CLASSIFIER_INFOS.put("weka.classifiers.bayes.NaiveBayesUpdateable", "Deprecated: please use NaiveBayes instead.");
		DEPRECATED_CLASSIFIER_INFOS.put("weka.classifiers.bayes.NaiveBayes", "Deprecated: please use NaiveBayes instead.");
	}

	@Override
	public void registerOperators(ClassLoader classLoader, Plugin plugin) {
		JarFile jarFile = plugin.getArchive();
		
		WEKA_ATTRIBUTE_EVALUATORS = WekaTools.getWekaClasses(jarFile, AttributeEvaluator.class);
	    WEKA_META_CLASSIFIERS = WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, ".meta.", true);
		WEKA_CLASSIFIERS = WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, ".meta.", false);
		WEKA_CLUSTERERS = WekaTools.getWekaClasses(jarFile, Clusterer.class);

		
		// learning schemes
		try {
			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, null, SKIPPED_CLASSIFIERS), DEPRECATED_CLASSIFIER_INFOS, "com.rapidminer.operator.learner.weka.GenericWekaLearner", "The weka learner", "modeling.classification_and_regression.weka.", null, plugin);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Cannot register Weka learners: " + e, e);
		}

		// meta learning schemes
		try {
			
			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, new String[] { ".meta." }, SKIPPED_META_CLASSIFIERS), "com.rapidminer.operator.learner.weka.GenericWekaMetaLearner", "The weka meta learner", "modeling.classification_and_regression.weka.", null, plugin);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Cannot register Weka meta learners: " + e, e);
		}

		// ensemble learning schemes
		try {
			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.classifiers.Classifier.class, ENSEMBLE_CLASSIFIERS, null), "com.rapidminer.operator.learner.weka.GenericWekaEnsembleLearner", "The weka ensemble learner", "modeling.classification_and_regression.weka.", null, plugin);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Cannot register Weka ensemble learners: " + e, e);
		}

		// association rule learners
		try {
			WEKA_ASSOCIATORS = WekaTools.getWekaClasses(jarFile, weka.associations.Associator.class, null, SKIPPED_ASSOCIATORS);
			WekaTools.registerWekaOperators(classLoader, WEKA_ASSOCIATORS, "com.rapidminer.operator.learner.weka.GenericWekaAssociationLearner", "The weka associator", "modeling.associations.weka", null, plugin);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Cannot register Weka association rule learners: " + e, e);
		}

		// feature weighting
		try {
			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.attributeSelection.AttributeEvaluator.class), "com.rapidminer.operator.features.weighting.GenericWekaAttributeWeighting", "The weka attribute evaluator", "modeling.weighting.weka", null, plugin);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Cannot register Weka feature weighting schemes: " + e, e);
		}

		// clusterers
		try {
			WekaTools.registerWekaOperators(classLoader, WekaTools.getWekaClasses(jarFile, weka.clusterers.Clusterer.class, SKIPPED_CLUSTERERS, false), "com.rapidminer.operator.clustering.clusterer.GenericWekaClustererAdaptor", "The weka clusterer", "modeling.clustering.weka", null, plugin);
		} catch (Exception e) {
			LogService.getRoot().log(Level.WARNING, "Cannot register Weka clusterers: " + e, e);
		}
	}
}
