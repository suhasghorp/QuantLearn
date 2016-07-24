package com.quantlearn.enums;

public enum DC {
     _30_360(1),
    _Act_360(2), 
    _Act_365(3),
    _30_Act(4),
    _30_365(5);
    
    private final int value;
    DC(final int value) { this.value = value; }
    public int getValue() { return value; }
}    

