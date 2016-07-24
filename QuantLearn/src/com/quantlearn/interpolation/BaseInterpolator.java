package com.quantlearn.interpolation;

public abstract class BaseInterpolator {
	double[] x;
	double[] y;
	int n;
	
	public BaseInterpolator() { }
	
	public BaseInterpolator(double[] x, double[] y) {
		this.x = x;
		this.y = y;
		this.n = x.length;
	}
	public void reinitialize(double[] x, double[] y) {
		this.x = x;
		this.y = y;
		this.n = x.length;
	}
	public double[] curve(double[] xarr) {
		int max = xarr.length;
        double[] result = new double[max];
        for (int j = 0; j <max; j++) {
            result[j] = solve(xarr[j]);
        }
        return result;
	}
	public abstract double solve(double xvar);
}
