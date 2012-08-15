package cmu.arktweetnlp.impl;

import edu.stanford.nlp.math.ArrayMath;
import edu.stanford.nlp.optimization.DiffFunction;
import gnu.trove.set.hash.THashSet;

import java.util.*;


/**
 * Class implementing the Orthant-Wise Limited-memory Quasi-Newton
 * algorithm (OWL-QN). OWN-QN is a numerical optimization procedure for
 * finding the optimum of an objective of the form smooth function plus
 * L1-norm of the parameters. It has been used for training log-linear
 * models (such as logistic regression) with L1-regularization. The
 * algorithm is described in "Scalable training of L1-regularized
 * log-linear models" by Galen Andrew and Jianfeng Gao. This
 * implementation includes built-in capacity to train logistic regression
 * or least-squares models with L1 regularization. It is also possible to
 * use OWL-QN to optimize any arbitrary smooth convex loss plus L1
 * regularization by defining the function and its gradient using the
 * supplied "DifferentiableFunction" class, and passing an instance of
 * the function to the OWLQN object. For more information, please read
 * the included file README.txt. Also included in the distribution are
 * the ICML paper and slide presentation.
 *
 * Significant portions of this code are taken from
 * <a href="http://research.microsoft.com/en-us/downloads/b1eb1016-1738-4bd5-83a9-370c9d498a03/default.aspx">Galen Andew's implementation</a>
 *
 * @author Michel Galley
 * 
 * modified by Michael Heilman (mheilman@cmu.edu) 
 * -allow for bias/intercept parameters that shouldn't be penalized
 * -make outside API calls easier (11/9/2010)
 * -removed lots of extraneous stuff from the stanford API
 * 
 * modified by Nathan Schneider (nschneid@cs.cmu.edu)
 * - support for constrained optimization with the projected gradient method
 * (see {@link #setConstrained(boolean)}
 * 
 */
public class OWLQN {

	private int maxIters = Integer.MAX_VALUE;
	
	/** Whether to use the "projected gradient" method to require weights to be 
	 * nonnegative and sum to 1. The projection operation is implemented in 
	 * {@link #project(double[])}.
	 */
	private static boolean constrained = false;

	interface TerminationCriterion {
		double getValue(OptimizerState state, StringBuilder out);
	}

	static class RelativeMeanImprovementCriterion implements TerminationCriterion {
		int numItersToAvg;
		Queue<Double> prevVals;

		RelativeMeanImprovementCriterion() {
			this(10);
		}

		RelativeMeanImprovementCriterion(int numItersToAvg) {
			this.numItersToAvg = numItersToAvg;
			this.prevVals = new LinkedList<Double>();
		}

		public double getValue(OptimizerState state, StringBuilder out) {

			double retVal = Double.POSITIVE_INFINITY;

			if (prevVals.size() >= numItersToAvg) {
				double prevVal = prevVals.peek();
				if (prevVals.size() == numItersToAvg) prevVals.poll();
				double averageImprovement = (prevVal - state.getValue()) / prevVals.size();
				double relAvgImpr = averageImprovement / Math.abs(state.getValue());
				String relAvgImprStr = String.format("%.4e",relAvgImpr);
				out.append("  (").append(relAvgImprStr).append(") ");
				retVal = relAvgImpr;
			} else {
				out.append("  (wait for "+numItersToAvg+" iters) ");
			}

			prevVals.offer(state.getValue());
			return retVal;
		}
	} // end static class RelativeMeanImprovementCriterion


	boolean quiet;
	boolean responsibleForTermCrit;

	public static Set<Integer> biasParameters = new THashSet<Integer>();

	TerminationCriterion termCrit;

	public interface WeightsPrinter {
		public void printWeights();
	}
	
	WeightsPrinter printer;
	
	
	public OWLQN(boolean quiet) {
		this.quiet = quiet;
		this.termCrit = new RelativeMeanImprovementCriterion();
		this.responsibleForTermCrit = true;
	}

	public OWLQN() {
		this(false);
	}

	public OWLQN(TerminationCriterion termCrit, boolean quiet) {
		this.quiet = quiet;
		this.termCrit = termCrit;
		this.responsibleForTermCrit = false;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.ark.tdf.IOptimizer#setQuiet(boolean)
	 */
	public void setQuiet(boolean q) {
		quiet = q;
	}

	/*
	public void minimize(DiffFunction function, double[] initial) {
		minimize(function, initial, 1.0);
	}

	public void minimize(DiffFunction function, double[] initial, double l1weight) {
		minimize(function, initial, l1weight, 1e-5);
	}

	public void minimize(DiffFunction function, double[] initial, double l1weight, double tol) {
		minimize(function, initial, l1weight, tol, 10);
	}
	*/
	
	
	/* (non-Javadoc)
	 * @see edu.cmu.cs.ark.tdf.IOptimizer#minimize(edu.stanford.nlp.optimization.DiffFunction, double[], double, double, int)
	 */
	public double[] minimize(DiffFunction function, double[] initial, double l1weight, double tol, int m) {

		OptimizerState state = new OptimizerState(function, initial, m, l1weight, quiet);

		if (!quiet) {
			System.err.printf("Optimizing function of %d variables with OWL-QN parameters:\n", state.dim);
			System.err.printf("   l1 regularization weight: %f.\n", l1weight);
			System.err.printf("   L-BFGS memory parameter (m): %d\n", m);
			System.err.printf("   Convergence tolerance: %f\n\n", tol);
			System.err.printf("Iter    n:\tnew_value\tdf\t(conv_crit)\tline_search\n");
			System.err.printf("Iter    0:\t%.4e\t\t(***********)\t", state.value);
		}

		StringBuilder buf = new StringBuilder();
		termCrit.getValue(state, buf);

		for(int i=0; i<maxIters; i++){
			buf.setLength(0);
			state.updateDir();				
			state.backTrackingLineSearch();
			
			double termCritVal = termCrit.getValue(state, buf);
			if (!quiet) {
				int numnonzero = ArrayMath.countNonZero(state.newX);
				System.err.printf("Iter %4d:\t%.4e\t%d", state.iter, state.value, numnonzero);
				System.err.print("\t"+ buf.toString());
				
				if (printer!=null)
					printer.printWeights();
			}

			//mheilman: I added this check because OWLQN was failing without it sometimes 
			//for large L1 penalties and few features...
			//This checks that the parameters changed in the last iteration.
			//If they didn't, then OWL-QN will try to divide by zero when approximating the Hessian.
			//The ro values end up 0 when the line search ends up trying a newX that equals X (or newGrad and grad).
			//That causes the differences stored in sList and yList to be zero, which eventually causes 
			//values in roList to be zero.  Below, ro values appear in the denominator, and they would 
			//cause the program to crash in the mapDirByInverseHessian() method if any were zero.
			//This only appears to happen once the parameters have already converged.  
			//I suspect that numerical loss of precision is the cause.
			if(arrayEquals(state.x, state.newX)){
				System.err.println("Warning: Stopping OWL-QN since there was no change in the parameters in the last iteration.  This probably means convergence has been reached.");
				break;
			}
					
			if (termCritVal < tol)
				break;

			state.shift();
		}

		if (!quiet) {
			System.err.println();
			System.err.printf("Finished with optimization.  %d/%d non-zero weights.\n",
					ArrayMath.countNonZero(state.newX), state.newX.length);
		}

		return state.newX;
	}

	
	private boolean arrayEquals(double [] x, double [] y){
		if(x.length != y.length) return false;
		for(int i=0; i<x.length; i++){
			if(x[i] != y[i]) return false;
		}
		return true;
	}
	

	/* (non-Javadoc)
	 * @see edu.cmu.cs.ark.tdf.IOptimizer#setMaxIters(int)
	 */
	public void setMaxIters(int maxIters) {
		this.maxIters = maxIters;
	}

	/* (non-Javadoc)
	 * @see edu.cmu.cs.ark.tdf.IOptimizer#getMaxIters()
	 */
	public int getMaxIters() {
		return maxIters;
	}

	/** Specify a callback for printing the weights after each iteration of optimization. */
	public void setWeightsPrinting(WeightsPrinter printer) {
		this.printer = printer;
	}
	
	private static int numUnconstrainedWeights = -1;
	
	/** Sets {@link #constrained} */
	public static void setConstrained(boolean constrained) {
		OWLQN.constrained = constrained;
		OWLQN.numUnconstrainedWeights = (constrained) ? 0 : -1;
	}
	
	/** Specify that each parameter vector will start with numUnconstrainedWeights; only the 
	 * remaining weights are subject to the constraints. A negative value indicates that 
	 * no constraints should be applied.
	 */
	public static void setConstrained(int numUnconstrainedWeights) {
		OWLQN.numUnconstrainedWeights = numUnconstrainedWeights;
		OWLQN.constrained = (numUnconstrainedWeights<0) ? false : true;
	}
	
	public static boolean isConstrained() {
		return constrained;
	}
	
	/** Projects the full weight vector into the feasible region where constrains are operative only on a subset of weights.
	 * The first numUnconstrainedWeights weights are preserved.
	 */
	protected static double[] projectWeights(double[] c) {
		if (numUnconstrainedWeights==0)
			return project(c);
		
		double[] constrained = new double[c.length-numUnconstrainedWeights];
		for (int j=numUnconstrainedWeights; j<c.length; j++) {
			constrained[j-numUnconstrainedWeights] = c[j];
		}
		double[] projected = project(constrained);
		double[] x = new double[c.length];
		for (int j=0; j<c.length; j++) {
			x[j] = (j<numUnconstrainedWeights) ? c[j] : projected[j-numUnconstrainedWeights];
		}
		return x;
	}
	
	/**
	 * Implementation of the (Michelot 1986) technique for projecting 
	 * a real vector into the feasible region K defined by the following constraints:
	 *   - components must sum to 1, and
	 *   - they must be nonnegative.
	 *  
	 * PROJECTION ALGORITHM
	 * <pre>
	   Given: a real vector c to be projected into a linearly-constrained subspace
			I ← ∅	// index set
			x ← c
			do
				Compute x' = P_V_I(x): x'_i =
				        0 if i ∈ I,
				        x_i - (1/(dim(c)-dim(I))) * (Σ_j { x_j } - 1) otherwise
				if x' ≥ 0: return x'
				I ← I ∪ {i | x'_i < 0}
				x ← P_X_I(x'): i.e. x' but with any negative components replaced by 0
			repeat
		Output: projection of c into constrained subspace K, i.e. P_K(c)
		</pre>
	 */
	public static double[] project(double[] c) {
		Set<Integer> indexSet = new THashSet<Integer>();
		double[] x = c;
		
		while (true) {
			double sum = 0.0;
			for (double xj : x) {
				sum += xj;
			}
			final double adjustmentTerm = (sum-1.0)/(c.length - indexSet.size());
			
			boolean nonnegative = true;
			for (int i=0; i<x.length; i++) {
				x[i] = (indexSet.contains(i)) ? 0.0 : x[i] - adjustmentTerm;
				if (x[i]<0.0) {
					nonnegative = false;
					indexSet.add(i);
					x[i] = 0.0;
				}
			}
			if (nonnegative)
				return x;
		}
	}

} // end static class OWLQN

class OptimizerState {

	double[] x, grad, newX, newGrad, dir;
	double[] steepestDescDir;
	LinkedList<double[]> sList = new LinkedList<double[]>();
	LinkedList<double[]> yList = new LinkedList<double[]>();
	LinkedList<Double> roList = new LinkedList<Double>();
	double[] alphas;
	double value;
	int iter, m;
	int dim;
	DiffFunction func;
	double l1weight;

	boolean quiet;
	
	//mheilman: I added this for debugging
	private String arrayToString(double [] arr){
		String res = "";
		for(int i=0; i<arr.length; i++){
			if(i>0) res += "\t";
			res += arr[i];
		}
		return res;
	}
	
	//mheilman: I added this for debugging the updateDir() method.
	private void printStateValues() {
		System.err.println("\nSLIST:");
		for(int i=0; i<sList.size(); i++){
			System.err.println(arrayToString(sList.get(i)));
		}
		
		System.err.println("YLIST:");
		for(int i=0; i<yList.size(); i++){
			System.err.println(arrayToString(yList.get(i)));
		}
		
		System.err.println("ROLIST:");
		for(int i=0; i<roList.size(); i++){
			System.err.println(roList.get(i));
		}
		System.err.println();
	}
	

	void mapDirByInverseHessian() {
		int count = sList.size();

		if (count != 0) {				
			for (int i = count - 1; i >= 0; i--) {
				//mheilman: The program will try to divide by zero here unless there is a check 
				//that the parameters change at each iteration.  See comments in the minimize() method.
				//A roList value is the inner product of the change in the gradient 
				//and the change in parameters between the current and last iterations.  
				//See the discussion of L-BFGS in Nocedal and Wright's Numerical Optimization book 
				//(though I think that defines rho as the multiplicative inverse of what is here).
				alphas[i] = -ArrayMath.innerProduct(sList.get(i), dir) / roList.get(i); 
				ArrayMath.addMultInPlace(dir, yList.get(i), alphas[i]);
			}

			double[] lastY = yList.get(count - 1);
			double yDotY = ArrayMath.innerProduct(lastY, lastY);
			double scalar = roList.get(count - 1) / yDotY;
			ArrayMath.multiplyInPlace(dir, scalar);

			for (int i = 0; i < count; i++) {
				double beta = ArrayMath.innerProduct(yList.get(i), dir) / roList.get(i);
				ArrayMath.addMultInPlace(dir, sList.get(i), -alphas[i] - beta);
			}
		}
	}

	void makeSteepestDescDir() {
		if (l1weight == 0) {
			ArrayMath.multiplyInto(dir, grad, -1);
		} else {

			for (int i=0; i<dim; i++) {
				//mheilman: I added this if-statement to avoid penalizing bias parameters.
				if(OWLQN.biasParameters.contains(i)){
					dir[i] = -grad[i];
					continue;
				}
				if (x[i] < 0) {
					dir[i] = -grad[i] + l1weight;
				} else if (x[i] > 0) {
					dir[i] = -grad[i] - l1weight;
				} else {
					if (grad[i] < -l1weight) {
						dir[i] = -grad[i] - l1weight;
					} else if (grad[i] > l1weight) {
						dir[i] = -grad[i] + l1weight;
					} else {
						dir[i] = 0;
					}
				}
			}
		}
		steepestDescDir = dir.clone(); // deep copy needed
	}

	void fixDirSigns() {
		if (l1weight > 0) {
			for (int i = 0; i<dim; i++) {
				if(OWLQN.biasParameters.contains(i)){
					continue;
				}
				if (dir[i] * steepestDescDir[i] <= 0) {
					dir[i] = 0;
				}
			}
		}
	}

	void updateDir() {
		//printStateValues(); //mheilman: I added this for debugging.
		makeSteepestDescDir();
		mapDirByInverseHessian();
		fixDirSigns();
		//mheilman: I commented out this debugging check.
		//if(!quiet) testDirDeriv();
	}

	/*
	void testDirDeriv() {
		double dirNorm = Math.sqrt(ArrayMath.innerProduct(dir, dir));
		double eps = 1.05e-8 / dirNorm;
		getNextPoint(eps);
		//double val2 = evalL1();
		//double numDeriv = (val2 - value) / eps;
		//double deriv = dirDeriv();
		//if (!quiet) System.err.print("  Grad check: " + numDeriv + " vs. " + deriv + "  ");
	}
	 */
	
	double dirDeriv() {
		if (l1weight == 0) {
			return ArrayMath.innerProduct(dir, grad);
		} else {
			double val = 0.0;
			for (int i = 0; i < dim; i++) {
				//mheilman: I added this if-statement to avoid penalizing bias parameters.
				if(OWLQN.biasParameters.contains(i)){
					val += dir[i] * grad[i]; 
					continue;
				}
				if (dir[i] != 0) {
					if (x[i] < 0) {
						val += dir[i] * (grad[i] - l1weight);
					} else if (x[i] > 0) {
						val += dir[i] * (grad[i] + l1weight);
					} else if (dir[i] < 0) {
						val += dir[i] * (grad[i] - l1weight);
					} else if (dir[i] > 0) {
						val += dir[i] * (grad[i] + l1weight);
					}
				}
			}
			return val;
		}
	}

	private boolean getNextPoint(double alpha) {			
		ArrayMath.addMultInto(newX, x, dir, alpha);
		/*if (OWLQN.isConstrained())
			newX = OWLQN.projectWeights(newX);*/ //TODO
		if (l1weight > 0) {
			for (int i=0; i<dim; i++) {
				//mheilman: I added this if-statement to avoid penalizing bias parameters.
				if(OWLQN.biasParameters.contains(i)){
		        	  continue;
		        }
				if (x[i] * newX[i] < 0.0) {
					newX[i] = 0.0;
				}
			}
		}
		return true;
	}
	
	

	double evalL1() {

		double val = func.valueAt(newX);
		// Don't remove clone(), otherwise newGrad and grad may end up referencing the same vector
		// (that's the case with LogisticObjectiveFunction)
		newGrad = func.derivativeAt(newX).clone();
		if (l1weight > 0) {
			for (int i=0; i<dim; i++) {
				//mheilman: I added this if-statement to avoid penalizing bias parameters.
				if(OWLQN.biasParameters.contains(i)){
		        	  continue;
		        }
				val += Math.abs(newX[i]) * l1weight;
			}
		}

		return val;
	}

	void backTrackingLineSearch() {

		double origDirDeriv = dirDeriv();
		// if a non-descent direction is chosen, the line search will break anyway, so throw here
		// The most likely reason for this is a bug in your function's gradient computation
		if (origDirDeriv >= 0) {
			throw new RuntimeException("L-BFGS chose a non-descent direction: check your gradient!");
		}
		
		

		double alpha = 1.0;
		double backoff = 0.5;
		if (iter == 1) {
			double normDir = Math.sqrt(ArrayMath.innerProduct(dir, dir));
			alpha = (1 / normDir);
			backoff = 0.1;
		}
		
		double c1 = 1e-4;
		double oldValue = value;
		while (true) {
			getNextPoint(alpha);
			value = evalL1();
			
			//mheilman: I think loss of precision can happen here for pathological cases because 
			//origDirDeriv can be many orders of magnitude less than value and oldValue.
			//Then, the right side will just end up being oldValue.
			if (value <= oldValue + c1 * origDirDeriv * alpha){
				break;
			}

			//mheilman: I added this extra check to keep the program
			//from backing off for a long time until numerical underflow happens.
			//If the line search hasn't found something by 1e-30, it's probably not going to,
			//and I think not having this check may cause the program to crash 
			//for some rare, pathological cases.
			if(alpha < 1e-30){
				System.err.println("Warning: The line search backed off to alpha < 1e-30, and stayed with the current parameter values.  This probably means converged has been reached.");
				value = oldValue;
				break;
			}

			if (!quiet) System.err.print(".");

			alpha *= backoff;
		}

		if (!quiet) System.err.println();
	}

	
	void shift() {
		double[] nextS = null, nextY = null;

		int listSize = sList.size();

		if (listSize < m) {
			try {
				nextS = new double[dim];
				nextY = new double[dim];
			} catch (OutOfMemoryError e) {
				m = listSize;
				nextS = null;
			}
		}

		if (nextS == null) {
			nextS = sList.poll();
			nextY = yList.poll();
			roList.poll();
		}

		ArrayMath.addMultInto(nextS, newX, x, -1);
		ArrayMath.addMultInto(nextY, newGrad, grad, -1);

		double ro = ArrayMath.innerProduct(nextS, nextY);
		assert(ro != 0.0);

		sList.offer(nextS);
		yList.offer(nextY);
		roList.offer(ro);

		double[] tmpX = newX;
		newX = x;
		x = tmpX;
		
		// TODO: added: nschneid
		/*if (OWLQN.isConstrained()) {
			newX = OWLQN.projectWeights(newX);
			x = OWLQN.projectWeights(x);
		}*/

		double[] tmpGrad = newGrad;
		newGrad = grad;
		grad = tmpGrad;

		++iter;
	}

	double getValue() { return value; }

	OptimizerState(DiffFunction f, double[] init, int m, double l1weight, boolean quiet) {
		this.x = init;
		this.grad = new double[init.length];
		this.newX = init.clone();
		this.newGrad = new double[init.length];
		this.dir = new double[init.length];
		this.steepestDescDir = newGrad.clone();
		this.alphas = new double[m];
		this.iter = 1;
		this.m = m;
		this.dim = init.length;
		this.func = f;
		this.l1weight = l1weight;
		this.quiet = quiet;

		if (m <= 0)
			throw new RuntimeException("m must be an integer greater than zero.");

		value = evalL1();
		grad = newGrad.clone();
	}
	
}


