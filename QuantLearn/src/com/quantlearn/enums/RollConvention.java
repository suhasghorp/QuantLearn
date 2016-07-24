package com.quantlearn.enums;

public enum RollConvention {
	Forward(1),
	Backward(2); 
	
	private final int value;
    RollConvention(final int value) { this.value = value; }
    public int getValue() { return value; }
}
