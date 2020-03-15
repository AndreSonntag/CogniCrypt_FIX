package de.upb.cognicryptfix.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.rules.CrySLArithmeticConstraint;
import crypto.rules.CrySLComparisonConstraint;
import de.upb.cognicryptfix.extractor.constraints.ArithmeticConstraint;
import de.upb.cognicryptfix.extractor.constraints.ComparisonConstraint;
import de.upb.cognicryptfix.utils.Pair;
import de.upb.cognicryptfix.utils.Utils;

/*-
 * Internally, the CrytSLComparisonConstraint consists 
 * of a left and right CrytSLArithmeticConstraint and
 * of an operator from type CompOp (<,>,>=,!=,=)
 * 
 * Internally, the CryptSLArithmeticConstraint consists of 
 * a left and right CryptSLObject and of an operator 
 * from type ArithOp (+,-,>) . Mostly, these
 * CryptSLObjects are variables and integer values
 * 
 * CryptSLArithmeticConstraint
 * 	- left (CrypSLObject)	varName = iterationsCount
 * 	- operations (ArithOp) name = p
 * 	- right (CrypSLObject) varName = 0 
 * 
 *  Example:
 *  iterationCount + 0 >= 10000 + 0
 * 
 */

// TODO: process methods like length(pre_plaintext) 

/**
 * @author Andre Sonntag
 * @date 22.09.2019
 */
public class CrySLComparsionConstraintExtractor {

	private static final Logger logger = LogManager.getLogger(CrySLComparsionConstraintExtractor.class.getSimpleName());
	private final CrySLComparisonConstraint constraint;
	private final AnalysisSeedWithSpecification seed;

	public CrySLComparsionConstraintExtractor(AnalysisSeedWithSpecification seed,
			CrySLComparisonConstraint constraint) {
		this.constraint = constraint;
		this.seed = seed;
	}

	public ComparisonConstraint extract() {
		ComparisonConstraint compCon = extractComparsionConstraint(constraint);
		return compCon;
	}

	private ComparisonConstraint extractComparsionConstraint(CrySLComparisonConstraint compCon) {

		Pair<Pair<String, String>, Pair<String, String>> leftArithmeticPair = extractArithmeticPairs(compCon.getLeft());
		Pair<Pair<String, String>, Pair<String, String>> rightArithmeticPair = extractArithmeticPairs(
				compCon.getRight());
		ArithmeticConstraint rightArithmeticConstraint = new ArithmeticConstraint(compCon.getRight().getOperator(),
				rightArithmeticPair.getLeft(), rightArithmeticPair.getRight());
		ArithmeticConstraint leftArithmeticConstraint = new ArithmeticConstraint(compCon.getLeft().getOperator(),
				leftArithmeticPair.getLeft(), leftArithmeticPair.getRight());
		ComparisonConstraint ret = new ComparisonConstraint(compCon.getOperator(), leftArithmeticConstraint,
				rightArithmeticConstraint);

		return ret;
	}

	/**
	 * This method converts a {@link CrySLArithmeticConstraint} to a {@link Pair}
	 * that consists of a left and right {@link Pair}. Each inner {@link Pair}
	 * presents a side of the {@link CrySLArithmeticConstraint}
	 * 
	 * @param artihCon
	 * @return a new {@link Pair} with a variable name and value
	 */
	private Pair<Pair<String, String>, Pair<String, String>> extractArithmeticPairs(
			CrySLArithmeticConstraint artihCon) {

		String left = artihCon.getLeft().getName();
		Pair<String, String> leftPair = assignVariableToValue(left);

		String right = artihCon.getRight().getName();
		Pair<String, String> rightPair = assignVariableToValue(right);

		return new Pair<Pair<String, String>, Pair<String, String>>(leftPair, rightPair);
	}

	/**
	 * This method assigns a new variable name to a constant value or extracts a
	 * value for a used variable name
	 * 
	 * @param name
	 * @return a new {@link Pair} with a variable name and value
	 */
	private Pair<String, String> assignVariableToValue(String name) {
		String varName = "";
		String varValue = "";

		if (StringUtils.isNumeric(name)) {
			varName = "_";
			varValue = name;
		} else {
			varName = name;
			varValue = Utils.extractValueAsString(seed, varName).keySet().iterator().next().toString();
		}
		return new Pair<String, String>(varName, varValue);
	}
}
