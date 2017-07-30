package org.openml.experiment.operators;

import org.openml.experiment.Logger;

import com.rapidminer.operator.Operator;

// A wrapper for the Polynominal by Binominal Classification
public class SVMWrapper 
{
	private Operator operator;
	
	public SVMWrapper(Operator operator, String strategy)
	{
		if(operator == null)
		{
			Logger.getInstance().logToFile("The operator reference given to SVMWrapper is null");
			System.exit(-1);
		}
		this.operator = operator;
		operator.setParameter("classification_strategies", strategy);
	}
	
	public void setClassificationStrategy(String strategy)
	{
		operator.setParameter("classification_strategies", strategy);
	}
	
	public String getClassificationStrategy()
	{
		try
		{
			return operator.getParameter("classification_strategies");
		}
		catch(Exception e)
		{
			return null;
		}
	}
}