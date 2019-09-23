package de.upb.cognicryptfix.extractor.constraints;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.rules.CryptSLArithmeticConstraint.ArithOp;
import crypto.rules.CryptSLComparisonConstraint.CompOp;
import de.upb.cognicryptfix.utils.Pair;

public class ComparisonConstraint implements IConstraint{

	private static final Logger logger = LogManager.getLogger(ComparisonConstraint.class.getSimpleName());
	private CompOp operator;
	private SubConstraint leftSubCon;	//used values
	private SubConstraint rightSubCon;	//expected values

	public ComparisonConstraint() {
	}
	
	public String getOperatorAsString() {
		return resolveCompOperator(operator);
	}
	
	public void setOperator(CompOp operator) {
		this.operator = operator;
	}
	
	public SubConstraint getLeftSubCon() {
		return leftSubCon;
	}

	public void createLeft(ArithOp operator, Pair<String> left, Pair<String> right) {
		this.leftSubCon = new SubConstraint(operator, left, right);
	}

	public SubConstraint getRightSubCon() {
		return rightSubCon;
	}

	public void createRight(ArithOp operator, Pair<String> left, Pair<String> right) {
		this.rightSubCon = new SubConstraint(operator, left, right);
	}

	@Override
	public String getUsedValue() throws ScriptException {
		//TODO: check boolean expression
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
	    String expressions  = getLeftSubCon().getLeftVarPair().getVal()+
	    					  getLeftSubCon().getOperatorAsString()+
	    					  getLeftSubCon().getRightVarPair().getVal();
	    Object res = engine.eval(expressions);
		return res.toString();
	}
	
	@Override
	public String getExpectedConstraintValue() throws ScriptException {
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
	    String expressions  = getRightSubCon().getLeftVarPair().getVal()+
	    					  getRightSubCon().getOperatorAsString()+
	    					  getRightSubCon().getRightVarPair().getVal();
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
		return "SimpleConstraint [operator:" + resolveCompOperator(operator) + ", leftSubCon:" + leftSubCon.toString() + ", rightSubCon:" + rightSubCon.toString() + "]";
	}

	private class SubConstraint {
		
		private ArithOp operator;
		private Pair<String> leftVarPair;
		private Pair<String> rightVarPair;
		
		public SubConstraint(ArithOp operator, Pair<String> leftVarPair, Pair<String> rightVarPair) {
			this.operator = operator;
			this.leftVarPair = leftVarPair;
			this.rightVarPair = rightVarPair;
		}
		
		public String getOperatorAsString() {
			return resolveArithOperator(operator);
		}
		
		public Pair<String> getLeftVarPair() {
			return leftVarPair;
		}

		public Pair<String> getRightVarPair() {
			return rightVarPair;
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
			return "SubConstraint [operator:" + resolveArithOperator(operator) + ", leftVarPair:" + leftVarPair.toString() + ", rightVarPair:" + rightVarPair.toString() + "]";
		}
	
	}

}
