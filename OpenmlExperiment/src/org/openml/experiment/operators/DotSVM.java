package org.openml.experiment.operators;

import java.util.List;

import org.openml.experiment.Logger;

import com.rapidminer.operator.Operator;

/*
 * A wrapper for the SVM with the dot kernel type
 */
public class DotSVM 
{
	private Operator operator;
	
	public DotSVM(List<String> args, Operator operator)
	{
		if(operator == null)
		{
			Logger.getInstance().logToFile("The operator reference given to DotSVM is null");
			System.exit(-1);
		}
		this.operator = operator;
		setKernelType(args.get(0));
		setC(args.get(1));
		setConvEps(args.get(2));
		setlPos(args.get(3));
		setlNeg(args.get(4));
		setEps(args.get(5));
		setEpsPlus(args.get(6));
		setEpsMin(args.get(7));
	}

	public String getKernelType() 
	{
		try
		{
			return operator.getParameter("kernel_type");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void setKernelType(String kernelType) 
	{
		operator.setParameter("kernel_type", kernelType);
	}

	public String getC() 
	{
		try
		{
			return operator.getParameter("C");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void setC(String c) 
	{
		operator.setParameter("C", c);
	}

	public String getConvEps() 
	{
		try
		{
			return operator.getParameter("convergence_epsilon");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void setConvEps(String convEps) 
	{
		operator.setParameter("convergence_epsilon", convEps);
	}

	public String getlPos() 
	{
		try
		{
			return operator.getParameter("L_pos");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void setlPos(String lPos) 
	{
		operator.setParameter("L_pos", lPos);
	}

	public String getlNeg() 
	{
		try
		{
			return operator.getParameter("L_neg");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void setlNeg(String lNeg) 
	{
		operator.setParameter("L_neg", lNeg);
	}

	public String getEps() 
	{
		try
		{
			return operator.getParameter("epsilon");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void setEps(String eps) 
	{
		operator.setParameter("epsilon", eps);
	}

	public String getEpsPlus() 
	{
		try
		{
			return operator.getParameter("epsilon_plus");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void setEpsPlus(String epsPlus) 
	{
		operator.setParameter("epsilon_plus", epsPlus);
	}

	public String getEpsMin() 
	{
		try
		{
			return operator.getParameter("epsilon_minus");
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public void setEpsMin(String epsMin) 
	{
		operator.setParameter("epsilon_minus", epsMin);
	}

	public Operator getOperator() 
	{
		return operator;
	}

	public void setOperator(Operator operator) 
	{
		this.operator = operator;
	}

}