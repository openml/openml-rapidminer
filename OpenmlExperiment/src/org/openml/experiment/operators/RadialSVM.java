package org.openml.experiment.operators;

import java.util.List;

import org.openml.experiment.Logger;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with the radial kernel type
 */
public class RadialSVM extends DotSVM
{
	public RadialSVM(List<String> args, Operator operator)
	{
		super(args, operator);
		if(args.size() != 9)
		{
			Logger.getInstance().logToFile("Not enough arguments for the RadialSVM");
			System.exit(-1);
		}
		setKernelGamma(args.get(8));
	}
	
	public void setKernelGamma(String kGamma)
	{
		this.getOperator().setParameter("kernel_gamma", kGamma);
	}
	
	public String getKernelGamma()
	{
		try
		{
			return this.getOperator().getParameter("kernel_gamma");
		}
		catch(Exception e)
		{
			return null;
		}
	}
}