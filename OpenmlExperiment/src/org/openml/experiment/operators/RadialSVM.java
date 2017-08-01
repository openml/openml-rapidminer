package org.openml.experiment.operators;

import java.util.List;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with the radial kernel type
 */
public class RadialSVM extends DotSVM
{
	public RadialSVM(List<String> args, Operator operator) throws Exception
	{
		super(args, operator);
		if(args.size() != 9)
		{
			throw new Exception("Not enough arguments for the RadialSVM");
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