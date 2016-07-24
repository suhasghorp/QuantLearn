package com.quantlearn.interpolation;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

public class CubicSpline extends BaseInterpolator {
public CubicSpline() { super(); }
	
	public CubicSpline(double[] x, double[] y) {
		super(x,y);
	}
	@Override
	public double solve(double xvar) {
		UnivariateInterpolator interpolator = new SplineInterpolator();
		UnivariateFunction function = interpolator.interpolate(x, y);
		return function.value(xvar);
	}
}
