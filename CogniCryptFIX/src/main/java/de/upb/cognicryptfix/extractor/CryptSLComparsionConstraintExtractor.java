package de.upb.cognicryptfix.extractor;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CryptSLArithmeticConstraint;
import crypto.rules.CryptSLComparisonConstraint;
import de.upb.cognicryptfix.extractor.constraints.ComparisonConstraint;
import de.upb.cognicryptfix.utils.Pair;
import de.upb.cognicryptfix.utils.Utils;

/*
 * Internally, the CryptSLComparisonConstraint consists 
 * of a left and right CryptSLArithmeticConstraint and
 * of an operator from type CryptSLComparsionConstraint
 * 
 * Each CryptSLArithmeticConstraint also consists of 
 * a left and right CryptSLObject and of an operator 
 * from type CryptSLComparsionConstraint. Mostly, this
 * CryptSLObjects are variables and integer values
 * 
 * CryptSLArithmeticConstraint
 * 	- left (CrypSLObject)	varName = iterationsCount
 * 	- operations (CryptSLComparsionConstraint) name = p
 * 	- right (CrypSLObject) varName = 0 
 * 
 * The CrySL format looks like (the right CrypSLObject
 * doesn't matter, because it is 0):
 * 	CONSTRAINTS
 * 		iterationCount >= 10000;
 * 
 * Internally it looks like:
 * iterationCount + 0 >= 10000 + 0
 */
//TODO: process methods like length(pre_plaintext)

/**
 * @author Andre Sonntag
 * @date 22.09.2019
 */
public class CryptSLComparsionConstraintExtractor{

	private static final Logger logger = LogManager.getLogger(CryptSLComparsionConstraintExtractor.class.getSimpleName());
	private final CryptSLComparisonConstraint constraint;
	private AnalysisSeedWithSpecification seed;

	public CryptSLComparsionConstraintExtractor(AnalysisSeedWithSpecification seed, CryptSLComparisonConstraint constraint) {
		this.constraint = constraint;
		this.seed = seed;
	}

	public ComparisonConstraint extract() {
		ComparisonConstraint compCon = extractSimpleConstraint(constraint);
		return compCon;
	}
	
	private ComparisonConstraint extractSimpleConstraint(CryptSLComparisonConstraint compCon) {
		ComparisonConstraint sc = new ComparisonConstraint();
		sc.setOperator(compCon.getOperator());
		Pair<Pair>left = extractPairs(compCon.getLeft());
		Pair<Pair>right = extractPairs(compCon.getRight());
		sc.createLeft(compCon.getLeft().getOperator(), left.getVar(), left.getVal());
		sc.createRight(compCon.getRight().getOperator(), right.getVar(), right.getVal());
		return sc;
	}
		
	//TODO: new method
	private Pair<Pair> extractPairs(CryptSLArithmeticConstraint artihCon) {
			
		String left = artihCon.getLeft().getName();
		String leftVarName = "";
		String leftVarValue = "";
		
		if (StringUtils.isNumeric(left)) {
			leftVarName = "_";
			leftVarValue = left;
		}
		else {
			leftVarName = left;
			leftVarValue = Utils.extractValueAsString(seed, leftVarName, constraint).keySet().iterator().next().toString();
		}
		Pair<String> leftPair = new Pair<String>(leftVarName, leftVarValue);
		
		
		String right = artihCon.getRight().getName();
		String rightVarName = "";
		String rightVarValue = "";
		
		if (StringUtils.isNumeric(right)) {
			rightVarName = "_";
			rightVarValue = right;
		}
		else {
			rightVarName = right;
			rightVarValue = Utils.extractValueAsString(seed, rightVarName, constraint).keySet().iterator().next().toString();
		}
		
		Pair<String> rightPair = new Pair<String>(rightVarName, rightVarValue);

	return new Pair<Pair>(leftPair, rightPair);
	}
}
