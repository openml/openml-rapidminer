package org.openml.experiment;

import java.util.HashMap;
import java.util.Random;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class Main{

	public static void main(String[] args){
		
		try{
			HashMap<String, String> parameters = new HashMap<String, String>();
			String[] tokens = args[0].split(System.lineSeparator());
			for(String token:tokens){
				String[] parameter = token.split("=");
				parameters.put(parameter[0], parameter[1]);
			}
            parameters.put("C", generateComplexity(parameters.get("complexity_max"), parameters.get("complexity_min")));
            parameters.put("kernel_gamma", generateKernelGamma(parameters.get("kernel_gamma_max"), parameters.get("kernel_gamma_min")));
            parameters.remove("complexity_max");
            parameters.remove("complexity_min");
            parameters.remove("kernel_gamma_max");
            parameters.remove("kernel_gamma_min");
            new Experiment(parameters).run();
		}
		catch(Exception e){
			Logger.getInstance().logToFile(e.getMessage() + ExceptionUtils.getStackTrace(e));
		}
    }
	
	/***
	 * 
	 * @param max- The upper bound for the complexity parameter.
	 * @param min- The lower bound for the complexity parameter.
	 * @return
	 * @throws NumberFormatException
	 */
	private static String generateComplexity(String max, String min) throws NumberFormatException{
		
		Random rand = new Random();
		Double upperBound = Math.log(Double.parseDouble(max));
		Double lowerBound = Math.log(Double.parseDouble(min));
		return Double.toString(Math.exp((rand.nextDouble() * (upperBound - lowerBound)) + lowerBound));
	}
	
	/***
	 * 
	 * @param max- The upper bound for the kernel gamma parameter.
	 * @param min- The lower bound for the kernel gamma parameter.
	 * @return
	 * @throws NumberFormatException
	 */
	private static String generateKernelGamma(String max, String min) throws NumberFormatException{
		
		Random rand = new Random();
		Double upperBound = Math.log(Double.parseDouble(max));
		Double lowerBound = Math.log(Double.parseDouble(min));
		return Double.toString(Math.exp((rand.nextDouble() * (upperBound - lowerBound)) + lowerBound));
	}
}