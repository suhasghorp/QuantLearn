package com.quantlearn.schedule;

import com.quantlearn.enums.TenorType;

public class Period {
	public int tenor;  
    public TenorType tenorType;  

     
    public Period(int tenor, TenorType tenorType) {
        this.tenor = tenor;
        this.tenorType = tenorType;
    }
    
    public Period(final String period) {
    	String maturity = period.substring(period.length() - 1);
        int nPeriods;
		try {
			nPeriods = Integer.parseInt(period.substring(0, period.length()-1));
			tenor = nPeriods;
	        tenorType = TenorType.valueOf(maturity);  
		} catch(NumberFormatException nfe) {
			//must be a fractional year
			double np = Double.parseDouble(period.substring(0, period.length()-1));
			tenor = (int)(12 * np);
	        tenorType = TenorType.M;
			
		}
             
    }

    public String getPeriodStringFormat() {
        return tenor + tenorType.name();
    }
     // Interval in time 1Y = 1, 6m = 0.5, ...18m =1.5
    public double timeInterval() { 
        return ((double)this.tenor / (double)this.tenorType.getValue()) ;    
    }
    public int getTenor() {
    	return this.tenor;
    }
}
