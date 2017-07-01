package org.openml.rapidminer.utils;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.FlowExists;
import org.openml.apiconnector.xml.Task;
import org.openml.apiconnector.xml.UploadFlow;
import org.openml.apiconnector.xml.UploadRun;
/*
 * Class which is used to transform the JSON response into the object which is needed 
 */
public class JsonMapper 
{
	// Create and return the task object from the JSON response
	public static Task getTask(String jsonString)
	{
		Task task = null;
		try
		{
			JSONObject object = new JSONObject(jsonString);
			JSONObject taskJson = object.getJSONObject("task");
			int taskId = taskJson.getInt("task_id");
			JSONArray input = taskJson.getJSONArray("input");
			JSONObject firstInput = (JSONObject) input.get(0);
			String firstInputName = firstInput.getString("name");
			JSONObject dataSetObject = firstInput.getJSONObject("data_set");
			int datasetId = dataSetObject.getInt("data_set_id");
			String targetFeature = dataSetObject.getString("target_feature");
			JSONObject secondInput = (JSONObject) input.get(1);
			String secondInputName = secondInput.getString("name");
			JSONObject estimationProcedure = secondInput.getJSONObject("estimation_procedure");
			//String type = estimationProcedure.getString("type");
			String data_splits_url = estimationProcedure.getString("data_splits_url");
			HashMap<String, String> parameterList = new HashMap<String, String>();
			JSONArray parameters = estimationProcedure.getJSONArray("parameter");
			JSONObject firstParameter = (JSONObject) parameters.get(0);
			String firstParameterName = firstParameter.getString("name");
			String firstParameterValue = firstParameter.getString("value");
			parameterList.put(firstParameterName, firstParameterValue);
			JSONObject secondParameter = (JSONObject) parameters.get(1);
			String secondParameterName = secondParameter.getString("name");
			/*
			 * Check if the second/third parameter have values. Only if they have values add them in the task.
			 */
			if(secondParameter.has("value"))
			{
				String secondParameterValue = secondParameter.getString("value");
				parameterList.put(secondParameterName, secondParameterValue);
			}
			JSONObject thirdParameter = (JSONObject) parameters.get(2);
			String thirdParameterName = thirdParameter.getString("name");
			if(thirdParameter.has("value"))
			{
				String thirdParameterValue = thirdParameter.getString("value");
				parameterList.put(thirdParameterName, thirdParameterValue);
			}
			task = new Task(taskId, firstInputName, datasetId, targetFeature, secondInputName, data_splits_url, parameterList);
			
		}
		catch(JSONException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(ClassCastException e)
		{
			e.printStackTrace();
			return null;
		}
		return task;
	}
	// Create and return the DataSetDescription from the JSON response
	public static DataSetDescription getDataSetDescription(String jsonString)
	{
		DataSetDescription dataset = null;
		try
		{
			JSONObject object = new JSONObject(jsonString);
			JSONObject datasetObject = object.getJSONObject("data_set_description");
			Integer datasetId = datasetObject.getInt("id");
			String datasetName = datasetObject.getString("name");
			String url = datasetObject.getString("url");
			String fileId = datasetObject.getString("file_id");
			dataset = new DataSetDescription(datasetId, datasetName, null, null, null, null, null, null, null, null, url, fileId, null, null, null, null);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(ClassCastException e)
		{
			e.printStackTrace();
			return null;
		}
		return dataset;
	}
	// Create and return the FlowExists object from the JSON response
	public static FlowExists getFlowExists(String jsonString)
	{
		FlowExists flowExist = null;
		try
		{
			JSONObject object = new JSONObject(jsonString);
			JSONObject flowExistsObject = object.getJSONObject("flow_exists");
			boolean exists = flowExistsObject.getBoolean("exists");
			int id = flowExistsObject.getInt("id");
			flowExist = new FlowExists(exists, id);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(ClassCastException e)
		{
			e.printStackTrace();
			return null;
		}
		return flowExist;
	}
	// Create and return the UploadFlow from the JSON response
	public static UploadFlow getUploadFlow(String jsonString)
	{
		UploadFlow uploadFlow = null;
		try
		{
			JSONObject object = new JSONObject(jsonString);
			JSONObject uploadFlowObject = object.getJSONObject("upload_flow");
			int id = Integer.parseInt(uploadFlowObject.getString("id"));
			uploadFlow = new UploadFlow(id);
		}
		catch(JSONException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(ClassCastException e)
		{
			e.printStackTrace();
			return null;
		}
		return uploadFlow;
	}
	// Create and return the UploadRun from the JSON response
	public static UploadRun getUploadRun(String jsonString)
	{
		UploadRun uploadRun = null;
		try
		{
			JSONObject object = new JSONObject(jsonString);
			JSONObject uploadRunObject = object.getJSONObject("upload_run");
			String id = uploadRunObject.getString("run_id");
			uploadRun = new UploadRun(Integer.parseInt(id));
		}
		catch(JSONException e)
		{
			e.printStackTrace();
			return null;
		}
		catch(ClassCastException e)
		{
			e.printStackTrace();
			return null;
		}
		return uploadRun;
	}
}
