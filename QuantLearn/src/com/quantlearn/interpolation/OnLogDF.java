package com.quantlearn.interpolation;

public class OnLogDF implements InterpolateOnWhat {
	public OnLogDF() {
		
	}
	@Override
	public double fromDfToInterp(double Df) { 
		return Math.log(Df); 
	}
    @Override
	public double fromInterpToDf(double x) { 
    	return Math.exp(x); 
    }
}
