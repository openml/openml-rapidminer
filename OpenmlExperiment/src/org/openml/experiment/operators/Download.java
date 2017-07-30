package org.openml.experiment.operators;

import org.openml.apiconnector.settings.Config;
import org.openml.experiment.Logger;

/*
 * Wrapper class for the RapidMiner Download Operator
 */
import com.rapidminer.operator.Operator;

/*
 * Wrapper for the download operator in RapidMiner
 */
public class Download
{
	private Operator operator;
	
	public Download(Operator operator, Config config, String taskId)
	{
		if(operator == null)
		{
			Logger.getInstance().logToFile("The operator reference given to Download is null");
			System.exit(-1);
		}
		this.operator = operator;
		setTaskId(taskId);
		setUrl(config.getServer());
		setApiKey(config.getApiKey());
	}
	
	public void setTaskId(String taskId)
	{
		operator.setParameter("Task id", taskId);
	}
	
	public String getTaskId()
	{
		try
		{
			return operator.getParameter("Task id");
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public void setUrl(String url)
	{
		operator.setParameter("Url", url);
	}
	
	public String getUrl()
	{
		try
		{
			return operator.getParameter("Url");
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public void setApiKey(String apiKey)
	{
		operator.setParameter("Api key", apiKey);
	}
	
	public String getApiKey()
	{
		try
		{
			return operator.getParameter("Api key");
		}
		catch(Exception e)
		{
			return null;
		}
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