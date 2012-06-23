package edu.cmu.cs.lti.ark.ssl.util;


import org.apache.commons.math.linear.Array2DRowRealMatrix;
import org.apache.commons.math.linear.ArrayRealVector;
import org.apache.commons.math.linear.DecompositionSolver;
import org.apache.commons.math.linear.LUDecompositionImpl;
import org.apache.commons.math.linear.RealMatrix;
import org.apache.commons.math.linear.RealVector;

public class SolveEquation {

	public static void main(String[] args) {
		double[][] coefficients = { { 3.0, 20.0, 89.0 },
				{ 4.0, 40.0, 298.0 },
				{ 7.0, 21.0, 0.42 } };
		double[] values = { 1324, 2999, 2039 };
		solve(coefficients, values);
	}

	public static double[] solve(double[][] coefficients, double[] values) {
		RealMatrix matrix = new Array2DRowRealMatrix(coefficients, false);
		DecompositionSolver solver = new LUDecompositionImpl(matrix).getSolver();
		RealVector constants = new ArrayRealVector(values, false);
		RealVector solution = solver.solve(constants);
		double[] solArray =  solution.getData();
		return solArray;
	}
}