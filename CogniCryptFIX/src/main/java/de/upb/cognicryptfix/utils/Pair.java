package de.upb.cognicryptfix.utils;

public class Pair <T>{
	
	private T var;
	private T val;

	public Pair() {
		
	}

	public Pair(T var, T val) {
		super();
		this.var = var;
		this.val = val;
	}

	public T getVar() {
		return var;
	}

	public void setVar(T var) {
		this.var = var;
	}

	public T getVal() {
		return val;
	}

	public void setVal(T val) {
		this.val = val;
	}

	@Override
	public String toString() {
		return "Pair [var:" + var + ", val:" + val + "]";
	}
	
	


}
