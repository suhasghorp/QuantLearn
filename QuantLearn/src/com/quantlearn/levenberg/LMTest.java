package com.quantlearn.levenberg;

import java.util.Arrays;

import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;


public class LMTest {
	
	double[] F_star = new double[] {0.05, 0.05,0.05};
    double dt = 0.25;
    double[] S = new double[] { 0.25, 0.50, 0.75, 1.00, 1.25, 1.50, 1.75, 2.00 };
    double[] T_star = new double[] { 0.0, 0.75, 1.75 };
    double[] DF_mktValue = new double[] { 0.99, 0.95, 0.91 };
    static double[] T = new double[] { 0.0, 0.25, 0.50, 0.75, 1.00, 1.25, 1.50, 1.75 };
	
	public static void main(String...strings) throws SolverException {
		LMTest test = new LMTest();
		test.test1();
		test.test2();
	}
	public void test1() throws SolverException {	
		
		LevenbergMarquardt optimizer = new LevenbergMarquardt() {

			// Override your objective function here
			@Override
			public void setValues(double[] parameters, double[] values) {
				values[0] = parameters[0] * 0.0 + parameters[1];
				values[1] = parameters[0] * 2.0 + parameters[1];
			}
		};

		// Set solver parameters
		optimizer.setInitialParameters(new double[] { 0, 0 });
		optimizer.setWeights(new double[] { 1, 1 });
		optimizer.setMaxIteration(100);
		optimizer.setTargetValues(new double[] { 5, 10 });

		optimizer.run();
		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);

		System.out.println();
	}
	
	public double[] AllFwd_Linear(double[] knownRatesStart, double[] knownFwd) {
        LinearInterpolator LI = new LinearInterpolator(knownRatesStart, knownFwd);
        return LI.Curve(T);  // getting linear interpolated data
    }
	
	public double CalcPxDF(double maturity, double[] fwds)
    {
        int n = Arrays.binarySearch(S, maturity);
        double df = 1.0;
        for (int i = 0; i <= n; i++)
        {
            df *= 1 / (1 + fwds[i] * dt);  // fwd rates are equidistant for construction 
        }
        return df;
    }
	
public void test2() throws SolverException {	
		
		LevenbergMarquardt optimizer = new LevenbergMarquardt() {
			@Override
			public void setValues(double[] x, double[] fi) {
				double[] f = AllFwd_Linear(T_star, x);
				fi[0] = 1000000 * (DF_mktValue[0] - CalcPxDF(0.25, f));
	            fi[1] = 1000000 * (DF_mktValue[1] - CalcPxDF(1.00, f));
	            fi[2] = 1000000 * (DF_mktValue[2] - CalcPxDF(2.00, f));
			}
		};

		// Set solver parameters
		optimizer.setInitialParameters(F_star);
		optimizer.setWeights(new double[] { 1, 1, 1 });
		optimizer.setMaxIteration(100000);
		optimizer.setTargetValues(new double[] { 0, 0, 0 });
		

		optimizer.run();
		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);

		System.out.println();
	}

static class LinearInterpolator {
		protected int n;
		protected double[] xarr;		 // Abscissa x-values
        protected double[] yarr;

        public LinearInterpolator(double[] xarr, double[] yarr) { 
        	n = xarr.length;
        	this.xarr = xarr;
            this.yarr = yarr;
        } 
        
        public int findAbscissa(double xvar)
        {
            for (int j = 0; j < n - 1; j++)
            {
                if (xarr[j] <= xvar && xvar <= xarr[j + 1])
                {
                    return j;
                }
            }
            return -1;
        }        
        
        public double Solve(double xvar)
        {  // Find the interpolated valued at a value x)

            int j = findAbscissa(xvar);	 // will give index of LHS value <= x


            // Now use the formula; x in interval [ x[j], x[j+1] ]
            return yarr[j] + (xvar - xarr[j]) * (yarr[j + 1] - yarr[j]) / (xarr[j + 1] - xarr[j]);
        }
        
        public double[] Curve(double[] xarr)
        {  // Create the interpolated curve

            int max = xarr.length;
            double[] result = new double[max];

            for (int j = 0; j <max; j++)
            {
                result[j] = Solve(xarr[j]);
            }
            return result;
        }

    }

	
	
}

