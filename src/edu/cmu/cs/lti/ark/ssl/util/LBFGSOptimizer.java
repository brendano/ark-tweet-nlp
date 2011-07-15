package edu.cmu.cs.lti.ark.ssl.util;

import java.util.logging.Logger;


import edu.berkeley.nlp.math.DifferentiableFunction;
import edu.berkeley.nlp.math.DoubleArrays;
import edu.berkeley.nlp.util.CallbackFunction;
import edu.cmu.cs.lti.ark.ssl.util.LBFGS.ExceptionWithIflag;

public class LBFGSOptimizer {

	private static Logger log = Logger.getLogger(LBFGSOptimizer.class.getCanonicalName());

	/*
	 * for LBFGS
	 */
	//not sure how this parameter comes into play
	protected static double m_eps = 1.0e-4;
	protected static double xtol = 1.0e-10; //estimate of machine precision.  get this right
	//number of corrections, between 3 and 7
	//a higher number means more computation and time, but more accuracy, i guess
	protected static int m_num_corrections = 3; 
	protected static boolean m_debug = true; 


	public static double[] optimize(DifferentiableFunction function, 
			double[] initial,
			CallbackFunction iterCallbackFunction,
			int iters)
	{    
		int numParams = initial.length;
		double[] diagco = new double[numParams];
		int[] iprint = new int[2];
		iprint[0] = m_debug?1:-1;  //output at every iteration (0 for 1st and last, -1 for never)
		iprint[1] = 0; //output the minimum level of info
		int[] iflag = new int[1];
		iflag[0] = 0;
		double[] m_estimate = DoubleArrays.clone(initial);
		int iteration = 0;
		try {
			do {
				double m_value = function.valueAt(m_estimate);
				double[] derivative = function.derivativeAt(m_estimate);
				log.info("Function Value:" + m_value);
				LBFGS.lbfgs(numParams,
						m_num_corrections, 
						m_estimate, 
						m_value,
						derivative, 
						false, //true if we're providing the diag                                                                                of cov matrix Hk0 (?)
						diagco, //the cov matrix
						iprint, //type of output generated
						m_eps,
						xtol, //estimate of machine precision
						iflag //i don't get what this is about
				);

				if (iterCallbackFunction != null) {
					iterCallbackFunction.callback(m_estimate, iteration, m_value, derivative);
				}
				iteration++;
			} while (iteration <= iters && iflag[0] != 0);	
		} catch (ExceptionWithIflag e) {
			e.printStackTrace();
		}
		return m_estimate;
	}	
}