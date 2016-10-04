package com.quantlearn.utils;

import java.util.List;

public class FormulaUtil {
	
	public static double DFsimple(double yf, double simpleRate) {            
        return 1.0 / (1.0 + (yf * simpleRate));
    }
	public static double parRate(double[] YearFraction, double[] DF) {
        double up = 0.0;              // numerator
        double down = 0.0;            // denominator
        int n = DF.length - 1;  // max size 

         // Numerator
        up = 1 - DF[n];

         // Denominator
        for (int i = 0; i <= n; i++) 
        {
            down += YearFraction[i] * DF[i];            
        }

         // Par Rate
        return up / down;
    }
	
	
	public static double[] DFSimpleList(List<Double> yf, double[] simpleRate) 
    {
         // yf and simpleRate array should have same size
        int n = yf.size();
        double[] Df = new double[n];  // array of df
        Df[0] = DFsimple(yf.get(0), simpleRate[0]);  // first DF
        for (int i = 1; i < n; i++) 
        {
            Df[i] = DFsimple(yf.get(i), simpleRate[i]) * Df[i - 1];  // df_0_i = df_0_i-1 * df_i-1_i;
        }
        return Df;
    }
	
	public static double multiCurveParRate(double[] yfFloatLeg, double[] dfFloatLeg, double[] fwdFloatLeg,double[] yfFixLeg,double[] dfFixLeg) throws Exception {
         // number of floating leg elements. 
        int nFlt = yfFloatLeg.length; // yfFloatLeg,dfFloatLeg,fwdFloatLeg should have same number of elements
         // number of fix leg elements. 
        int nFix = yfFixLeg.length; //  yfFixLeg,dfFixLeg should have same number of elements

         // Mercurio, F. 2009: numerator of formula (8)
        double numerator = 0.0;
         // Mercurio, F. 2009: denominator of formula (8)
        double denominator = 0.0;
        
         // Check correct dim
        if ((yfFloatLeg.length != dfFloatLeg.length) || (fwdFloatLeg.length != dfFloatLeg.length)
            || (yfFixLeg.length != dfFixLeg.length))
        {
            throw new Exception("error in leg dimension");
        }           
        
         // numerator
        for (int i = 0; i < nFlt; i++ ) {
            numerator += yfFloatLeg[i] * dfFloatLeg[i] * fwdFloatLeg[i];
        }

         // denominator
        for (int i = 0; i < nFix; i++) {
            denominator += yfFixLeg[i] * dfFixLeg[i];            
        }

         // par rate
        return numerator / denominator;
    }
	
	// Hagan. P.S. and West, G. 2006. Formula 3)
    public static double finalDF(double parRate, double[] yearFraction, double[] DF) 
    {
         // DF element should be >= YearFractionElement-1
        double up = 0.0;              // numerator
        double down = 0.0;            // denominator
        int n = yearFraction.length - 1;  // max size  of YearFraction
                                              // it uses only n-1 DF, since DF[n] is the one to find

         // numerator
        for (int i = 0; i < n; i++)
        {
            up += yearFraction[i] * DF[i];
        }
        up = 1 - parRate * up;

         // Denominator
        down = 1 + parRate * yearFraction[n];
        
        return up/down;            
    }
	
}
