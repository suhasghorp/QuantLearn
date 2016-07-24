package com.quantlearn.enums;

public enum Freq {
	 Once(0),
    Annual(1),           
    SemiAnnual(2),
    Quarterly(4),
    Monthly(12),         
    Weekly(52),          
    Daily(365); 
    
	private final int value;
    Freq(final int value) { this.value = value; }
    public int getValue() { return value; }
}
