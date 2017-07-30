package org.openml.experiment.operators;

import java.util.List;

import org.openml.experiment.Logger;

import com.rapidminer.operator.Operator;

/*
 * Wrapper for the SVM with the neural kernel type
 */
public class NeuralSVM extends DotSVM
{
	public NeuralSVM(List<String> args, Operator operator)
	{
		super(args, operator);
		if(args.size() != 10)
		{
			Logger.getInstance().logToFile("Not enough arguments for the NeuralSVM");
			System.exit(-1);
		}
		setKernelA(args.get(8));
		setKernelB(args.get(9));
	}
	
	public void setKernelA(String kA)
	{
		this.getOperator().setParameter("kernel_a", kA);
	}
	
	public String getKernelA()
	{
		try
		{
			return this.getOperator().getParameter("kernel_a");
		}
		catch(Exception e)
		{
			return null;
		}
	}
	public void setKernelB(String kB)
	{
		this.getOperator().setParameter("kernel_b", kB);
	}
	
	public String getKernelB()
	{
		try
		{
			return this.getOperator().getParameter("kernel_b");
		}
		catch(Exception e)
		{
			return null;
		}
	}
}