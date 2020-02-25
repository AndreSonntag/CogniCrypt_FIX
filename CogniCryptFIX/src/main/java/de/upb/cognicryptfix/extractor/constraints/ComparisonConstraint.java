package de.upb.cognicryptfix.extractor.constraints;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.rules.CrySLComparisonConstraint.CompOp;


/**
 * A ComparsionConstraint is, for instance, iterationCount >= 10000;
 * 
 * @author Andre Sonntag
 * @date 22.09.2019
 */
public class ComparisonConstraint implements IConstraint{

	private static final Logger logger = LogManager.getLogger(ComparisonConstraint.class.getSimpleName());
	private CompOp operator;
	private ArithmeticConstraint leftAritCon;		//used values
	private ArithmeticConstraint rightAritCon;	//expected values

	public ComparisonConstraint(CompOp operator, ArithmeticConstraint leftAritCon, ArithmeticConstraint rightAritCon) {
		super();
		this.operator = operator;
		this.leftAritCon = leftAritCon;
		this.rightAritCon = rightAritCon;
	}

	@Override
	public String getUsedValue() throws ScriptException {
		return leftAritCon.resolveConstraint();
	}
	
	@Override
	public String getExpectedConstraintValue() throws ScriptException {
		return rightAritCon.resolveConstraint();
	}
	
	private String resolveComparsionOperator(CompOp op) {
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
		return "SimpleConstraint [operator:" + resolveComparsionOperator(operator) + ", leftSubCon:" + leftAritCon.toString() + ", rightSubCon:" + rightAritCon.toString() + "]";
	}

	


}
