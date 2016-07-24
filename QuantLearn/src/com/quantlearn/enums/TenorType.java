package com.quantlearn.enums;

public enum TenorType {
	D(365),        
    W(52),       
    M(12),       
    Y(1);
	private final int value;
    TenorType(final int value) { this.value = value; }
    public int getValue() { return value; }
}
