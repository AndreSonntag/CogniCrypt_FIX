package de.upb.cognicryptfix.extractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import boomerang.jimple.Statement;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.HardCodedError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.extractparameter.CallSiteWithExtractedValue;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ICryptSLPredicateParameter;
import crypto.rules.CryptSLMethod;
import crypto.rules.CryptSLObject;
import crypto.rules.CryptSLPredicate;
import crypto.typestate.CryptSLMethodToSootMethod;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.extractor.constraints.PredicateConstraint;
import de.upb.cognicryptfix.patcher.patches.NeverTypeOfPatch;
import de.upb.cognicryptfix.utils.Pair;
import de.upb.cognicryptfix.utils.Utils;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.jimple.Stmt;
import soot.jimple.internal.JAssignStmt;

/**
 * @author Andre Sonntag
 * @date 19.07.2019
 */
public class CryptSLPredicateExtractor {

	private static final Logger logger = LogManager.getLogger(NeverTypeOfPatch.class.getSimpleName());
	private AnalysisSeedWithSpecification seed;
	private CryptSLPredicate predicate;

	public CryptSLPredicateExtractor(AnalysisSeedWithSpecification seed, CryptSLPredicate predicate) {
		this.seed = seed;
		this.predicate = predicate;
	}

	public PredicateConstraint extract() {
		String predName = predicate.getPredName();

		if (Constants.predefinedPreds.contains(predName)) {
			return handlePredefinedPreds();
		} else {
			logger.info("normal predicate -->" + predName);
			return null;
		}

	}

	public PredicateConstraint handlePredefinedPreds() {

		PredicateConstraint predCon;
		List<ICryptSLPredicateParameter> parameters = predicate.getParameters();

		switch (predicate.getPredName()) {
		case "neverTypeOf":
			// pred looks as follows: neverTypeOf($varName, $type)
			// -> first parameter is always the variable
			// -> second parameter is always the type
			String varName = ((CryptSLObject) parameters.get(0)).getVarName();
			for (CallSiteWithParamIndex cs : seed.getParameterAnalysis().getAllQuerySites()) {
				if (cs.getVarName().equals(varName)) {
					Collection<Type> vals = seed.getParameterAnalysis().getPropagatedTypes().get(cs);
					List valsList = new ArrayList(vals);
					return new PredicateConstraint(valsList.get(1).toString(), valsList.get(0).toString());
					
//					for (Type t : vals) {
//						for (ExtractedValue v : seed.getParameterAnalysis().getCollectedValues().get(cs)) {
//							seed.getParameterAnalysis().getCollectedValues()
//							Pair<String> parameter = null;
//							CallSiteWithExtractedValue temp1 = new CallSiteWithExtractedValue(cs, v);
//							AnalysisSeedWithSpecification temp2 = seed;
//							CryptSLPredicate temp3 = predicate;
//						}
//					}
				}

			}
			break;
		case "length":
		case "notHardCoded":
		case "callTo":
		case "noCallTo":
			break;
		default:
			break;
		}
		return null;
	}

}
