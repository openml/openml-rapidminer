package org.openml.experiment.operators;

import java.util.List;

import org.openml.experiment.Logger;

import com.rapidminer.operator.Operator;
/*
 * Wrapper for the SVM with the gaussian combination kernel type
 */
public class GaussianCombinationSVM extends DotSVM
{
	public GaussianCombinationSVM(List<String> args, Operator operator)
	{
		super(args, operator);
		if(args.size() != 11)
		{
			Logger.getInstance().logToFile("Not enough arguments for the GaussianCombinationSVM");
			System.exit(-1);
		}
		setKernelSigma1(args.get(8));
		setKernelSigma2(args.get(9));
		setKernelSigma3(args.get(10));
	}
	
	public void setKernelSigma1(String kSigma1)
	{
		this.getOperator().setParameter("kernel_sigma1", kSigma1);
	}
	
	public String getKernelSigma1()
	{
		try
		{
			return this.getOperator().getParameter("kernel_sigma1");
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public void setKernelSigma2(String kSigma2)
	{
		this.getOperator().setParameter("kernel_sigma2", kSigma2);
	}
	
	public String getKernelSigma2()
	{
		try
		{
			return this.getOperator().getParameter("kernel_sigma2");
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public void setKernelSigma3(String kSigma3)
	{
		this.getOperator().setParameter("kernel_sigma3", kSigma3);
	}
	
	public String getKernelSigma3()
	{
		try
		{
			return this.getOperator().getParameter("kernel_sigma3");
		}
		catch(Exception e)
		{
			return null;
		}
	}

}