package de.upb.cognicryptfix.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CryptSLArithmeticConstraint.ArithOp;
import crypto.rules.CryptSLComparisonConstraint.CompOp;

public class PrimitiveConstraint {

	private static final Logger logger = LogManager.getLogger(PrimitiveConstraint.class.getSimpleName());
	private CompOp operator;
	private SubConstraint left;
	private SubConstraint right;

	public PrimitiveConstraint() {
	}
	
	public String getOperatorAsString() {
		return resolveCompOperator(operator);
	}
	
	public void setOperator(CompOp operator) {
		this.operator = operator;
	}
	
	public SubConstraint getLeft() {
		return left;
	}

	public void createLeft(ArithOp operator, Pair<String> left, Pair<String> right) {
		this.left = new SubConstraint(operator, left, right);
	}

	public SubConstraint getRight() {
		return right;
	}

	public void createRight(ArithOp operator, Pair<String> left, Pair<String> right) {
		this.right = new SubConstraint(operator, left, right);
	}

	public String getRealValueOfVar(String varName) throws ScriptException {
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
	    String expressions  = left.left.getVal()+left.getOperatorAsString()+left.right.getVal();
	    Object res = engine.eval(expressions);
		return res.toString();
	}
	
	public String getExpectedValueOfVar(String varName) throws ScriptException {
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
	    String expressions  = right.left.getVal()+right.getOperatorAsString()+right.right.getVal();
		Object res = engine.eval(expressions);
		return res.toString();
	}
	
	private String resolveCompOperator(CompOp op) {
		switch (op) {
		case l:
			return "<";
		case le:
			return "<";
		case g:
			return ">";
		case ge:
			return ">=";
		case neq:
			return "!=";
		default:
			return "=";
		}
	}
	
	@Override
	public String toString() {
		return "SimpleConstraint [operator:" + resolveCompOperator(operator) + ", left:" + left.toString() + ", right:" + right.toString() + "]";
	}

	private class SubConstraint {
		
		private ArithOp operator;
		private Pair<String> left;
		private Pair<String> right;
		
		public SubConstraint(ArithOp operator, Pair<String> left, Pair<String> right) {
			this.operator = operator;
			this.left = left;
			this.right = right;
		}
		
		public boolean hasLeft() {
			return left != null ? true : false;
		}
		
		public boolean hasRight() {
			return right != null ? true : false;
		}

		public String getOperatorAsString() {
			return resolveArithOperator(operator);
		}
		
		public Pair<String> getLeft() {
			return left;
		}

		public Pair<String> getRight() {
			return right;
		}
		
		private String resolveArithOperator(ArithOp op) {
			switch (op) {
			case p:
				return "+";
			case n:
				return "-";
			case m:				
				return ">";
			default:
				return "something went wrong";
			}
		}

		@Override
		public String toString() {
			return "SubConstraint [operator:" + resolveArithOperator(operator) + ", left:" + left.toString() + ", right:" + right.toString() + "]";
		}
	
	}

}
