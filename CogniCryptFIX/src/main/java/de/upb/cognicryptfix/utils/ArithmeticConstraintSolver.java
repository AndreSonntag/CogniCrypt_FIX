package de.upb.cognicryptfix.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.rules.CrySLArithmeticConstraint.ArithOp;

/**
 * A ArithmeticConstraint is an inner constraint of a
 * {@link ComparisonConstraint}, for example, (iterationCount + 0) or (10000 +
 * 0);
 */
public class ArithmeticConstraintSolver {

	private static final Logger logger = LogManager.getLogger(ArithmeticConstraintSolver.class);
	private ArithOp operator;
	private Pair<String, String> leftPair; // we use <String,String>, because we don't know if the value is int or double
	private Pair<String, String> rightPair;

	public ArithmeticConstraintSolver(ArithOp operator, Pair<String, String> leftPair, Pair<String, String> rightPair) {
		this.operator = operator;
		this.leftPair = leftPair;
		this.rightPair = rightPair;
	}

	public String getOperatorAsString() {
		return resolveArithmeticOperator(operator);
	}

	private String resolveArithmeticOperator(ArithOp op) {
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

	public String resolveConstraint() {
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		String expressions = leftPair.getRight() + getOperatorAsString() + rightPair.getRight();
		try {
			return engine.eval(expressions).toString();
		} catch (ScriptException e) {
			logger.error("ArithmeticConstraintSolver could not solve following equation: " + expressions);
		}
		return null;
	}

}
