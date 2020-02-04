package de.upb.cognicryptfix.extractor.constraints;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.rules.CrySLArithmeticConstraint.ArithOp;
import de.upb.cognicryptfix.utils.Pair;

/**
 * A ArithmeticConstraint is an inner constraint of a
 * {@link ComparisonConstraint}, for example, (iterationCount + 0) or (10000 +
 * 0);
 */
public class ArithmeticConstraint {

	private static final Logger logger = LogManager.getLogger(ArithmeticConstraint.class.getSimpleName());
	private ArithOp operator;
	private Pair<String, String> leftPair; // we use <String,String>, because we don't know if the value is int or double
	private Pair<String, String> rightPair;

	public ArithmeticConstraint(ArithOp operator, Pair<String, String> leftPair, Pair<String, String> rightPair) {
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
		// TODO: check boolean expression
		ScriptEngineManager mgr = new ScriptEngineManager();
		ScriptEngine engine = mgr.getEngineByName("JavaScript");
		String expressions = leftPair.getRight() + getOperatorAsString() + rightPair.getRight();
		try {
			return engine.eval(expressions).toString();
		} catch (ScriptException e) {
			logger.error("SubComparsionConstraint could not solve following equation: " + expressions);
		}
		return null;
	}

}
