package com.quantlearn.utils;

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
}
