package cmu.arktweetnlp.util;

import java.util.ArrayList;
import java.util.Arrays;

import edu.stanford.nlp.util.StringUtils;


public class Util { 
	public static void p(Object x) { System.out.println(x); }
	public static void p(String[] x) { p(Arrays.toString(x)); }
	public static void p(double[] x) { p(Arrays.toString(x)); }
	public static void p(int[] x) { p(Arrays.toString(x)); }
	public static void p(double[][] x) {

		System.out.printf("(%s x %s) [\n", x.length, x[0].length);
		for (double[] row : x) {
			System.out.printf(" ");
			p(Arrays.toString(row));
		}
		p("]");
	}
	public static String sp(double[] x) {
		ArrayList<String> parts = new ArrayList<String>();
		for (int i=0; i < x.length; i++)
			parts.add(String.format("%.2g", x[i]));
		return "[" + StringUtils.join(parts) + "]";
	}
	//	public static void p(int[][] x) { p(Arrays.toString(x)); }
	public static void p(String x) { System.out.println(x); }
}
