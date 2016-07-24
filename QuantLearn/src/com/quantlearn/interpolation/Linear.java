package com.quantlearn.interpolation;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

public class Linear extends BaseInterpolator {
	/*public Linear() { super(); }
	
	public Linear(double[] x, double[] y) {
		super(x,y);
	}
	@Override
	public double solve(double xvar)
    {
        // Find the interpolated valued at a value x)
        int j = findAbscissa(xvar);	
        // Now use the formula; x in interval [ x[j], x[j+1] ]
        return y[j] + (xvar - x[j]) * (y[j + 1] - y[j]) / (x[j + 1] - x[j]);
    }*/
	public Linear() { super(); }
	
	public Linear(double[] x, double[] y) {
		super(x,y);
	}
	@Override
	public double solve(double xvar) {
		UnivariateInterpolator interpolator = new LinearInterpolator();
		UnivariateFunction function = interpolator.interpolate(x, y);
		return function.value(xvar);
	}

}
