package org.openml.experiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main 
{

	public static void main(String[] args) 
	{
		List<String> parameters = new ArrayList<String>();
		parameters.addAll(Arrays.asList("22", "7061", "1 against 1", "multiquadric", "0.0", "0.001", "1.0", "1.0", "0.0", "0.0", "0.0", "1.0", "1.0"));
		new Experiment(parameters).run();
	}
}