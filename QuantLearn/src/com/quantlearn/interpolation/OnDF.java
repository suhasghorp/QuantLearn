package com.quantlearn.interpolation;

public class OnDF implements InterpolateOnWhat {
	public OnDF() {
		
	}
	@Override
	public double fromDfToInterp(double Df) { 
		return Df;
	}
    @Override
	public double fromInterpToDf(double x) { 
    	return x;
    }
}

