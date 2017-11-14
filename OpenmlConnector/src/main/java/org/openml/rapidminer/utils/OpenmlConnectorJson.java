package org.openml.rapidminer.utils;

import java.io.File;
import java.util.Map;
import java.util.zip.DataFormatException;
import org.openml.rapidminer.utils.HttpConnectorJson;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.openml.apiconnector.algorithms.Conversion;
import org.openml.apiconnector.io.OpenmlConnector;
import org.openml.apiconnector.settings.Constants;
import org.openml.apiconnector.xml.DataSetDescription;
import org.openml.apiconnector.xml.FlowExists;
import org.openml.apiconnector.xml.Task;
import org.openml.apiconnector.xml.UploadFlow;
import org.openml.apiconnector.xml.UploadRun;

public class OpenmlConnectorJson extends OpenmlConnector {
	
	private static final long serialVersionUID = -671941927570371462L;

	public OpenmlConnectorJson(String url, String api_key, boolean useJson) {
		super(url, api_key, useJson);
	}

	/**
	 * @param task_id
	 *            - The numeric id of the task to be obtained.
	 * @return Task - An object describing the task
	 * @throws Exception
	 *             - Can be: API Error (see documentation at openml.org), server
	 *             down, etc.
	 */
	@Override
	public Task taskGet(int task_id) throws Exception {
		String jsonString = HttpConnectorJson
				.doApiRequest(getApiUrl() + "task/" + task_id, getApiKey(), getVerboselevel()).toString();
		Object apiResult = JsonMapper.getTask(jsonString);
		if (apiResult instanceof Task) {
			return (Task) apiResult;
		} else {
			throw new DataFormatException("Casting Api Object to Task");
		}
	}

	/**
	 * Retrieves the description of a specified data set.
	 * 
	 * @param did
	 *            - The data_id of the data description to download.
	 * @return DataSetDescription - An object containing the description of the
	 *         data
	 * @throws Exception
	 *             - Can be: API Error (see documentation at openml.org), server
	 *             down, etc.
	 */
	@Override
	public DataSetDescription dataGet(int did) throws Exception {
		String jsonString = HttpConnectorJson.doApiRequest(getApiUrl() + "data/" + did, getApiKey(), getVerboselevel())
				.toString();
		Object apiResult = JsonMapper.getDataSetDescription(jsonString);
		if (apiResult instanceof DataSetDescription) {
			return (DataSetDescription) apiResult;
		} else {
			throw new DataFormatException("Casting Api Object to DataSetDescription");
		}
	}

	/**
	 * Checks whether a flow exists, by name/external_version combination
	 * 
	 * @param name
	 *            - The name of the implementation to be checked
	 * @param external_version
	 *            - The external version (workbench version). If not a proper
	 *            revision number is available, it is recommended to use a MD5
	 *            hash of the source code.
	 * @return ImplementationExists - An object describing whether this
	 *         implementation is already known on the server.
	 * @throws Exception
	 *             - Can be: API Error (see documentation at openml.org), server
	 *             down, etc.
	 */
	@Override
	public FlowExists flowExists(String name, String external_version) throws Exception {
		String jsonString = HttpConnectorJson.doApiRequest(getApiUrl() + "flow/exists/" + name + "/" + external_version,
				getApiKey(), getVerboselevel()).toString();
		Object apiResult = JsonMapper.getFlowExists(jsonString);
		if (apiResult instanceof FlowExists) {
			return (FlowExists) apiResult;
		} else {
			throw new DataFormatException("Casting Api Object to ImplementationExists");
		}
	}

	/**
	 * Uploads a flow
	 * 
	 * @param description
	 *            - An XML file describing the implementation. See documentation
	 *            at openml.org.
	 * @param binary
	 *            - A file containing the implementation binary.
	 * @param source
	 *            - A file containing the implementation source.
	 * @return UploadImplementation - An object containing information on the
	 *         implementation upload.
	 * @throws Exception
	 *             - Can be: API Error (see documentation at openml.org), server
	 *             down, etc.
	 */
	@Override
	public UploadFlow flowUpload(File description, File binary, File source) throws Exception {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		entityBuilder.addPart("description", new FileBody(description));
		entityBuilder.addPart("api_key", new StringBody(getApiKey()));

		if (source != null) {
			entityBuilder.addPart("source", new FileBody(source));
		}
		if (binary != null) {
			entityBuilder.addPart("binary", new FileBody(binary));
		}
		String jsonString = HttpConnectorJson
				.doApiRequest(getApiUrl() + "flow", entityBuilder.build(), getApiKey(), getVerboselevel()).toString();
		Object apiResult = JsonMapper.getUploadFlow(jsonString);
		if (apiResult instanceof UploadFlow) {
			return (UploadFlow) apiResult;
		} else {
			throw new DataFormatException("Casting Api Object to UploadImplementation");
		}
	}

	/**
	 * Uploads a run
	 * 
	 * @param description
	 *            - An XML file describing the run. See documentation at
	 *            openml.org.
	 * @param output_files
	 *            - A Map&gt;String,File&lt; containing all relevant output
	 *            files. Key "predictions" usually contains the predictions that
	 *            were generated by this run.
	 * @return UploadRun - An object containing information on the
	 *         implementation upload.
	 * @throws Exception
	 *             - Can be: API Error (see documentation at openml.org), server
	 *             down, etc.
	 */
	@Override
	public UploadRun runUpload(File description, Map<String, File> output_files) throws Exception {
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		if (getVerboselevel() >= Constants.VERBOSE_LEVEL_ARFF) {
			System.out.println(Conversion.fileToString(output_files.get("predictions")) + "\n==========\n");
		}
		if (getVerboselevel() >= Constants.VERBOSE_LEVEL_XML) {
			System.out.println(Conversion.fileToString(description) + "\n==========");
		}
		entityBuilder.addPart("description", new FileBody(description));
		entityBuilder.addPart("api_key", new StringBody(getApiKey()));
		if (output_files != null) {
			for (String s : output_files.keySet()) {
				entityBuilder.addPart(s, new FileBody(output_files.get(s)));
			}
		}
		String jsonString = HttpConnectorJson
				.doApiRequest(getApiUrl() + "run/", entityBuilder.build(), getApiKey(), getVerboselevel()).toString();
		Object apiResult = JsonMapper.getUploadRun(jsonString);
		if (apiResult instanceof UploadRun) {
			return (UploadRun) apiResult;
		} else {
			throw new DataFormatException("Casting Api Object to UploadRun");
		}
	}
}
