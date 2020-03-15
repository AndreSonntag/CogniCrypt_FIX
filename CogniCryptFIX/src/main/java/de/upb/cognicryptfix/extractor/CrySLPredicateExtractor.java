package de.upb.cognicryptfix.extractor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.interfaces.ICrySLPredicateParameter;
import crypto.rules.CrySLObject;
import crypto.rules.CrySLPredicate;
import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.extractor.constraints.PredicateConstraint;
import de.upb.cognicryptfix.patcher.patches.NeverTypeOfPatch;
import soot.Type;

/**
 * For NeverTypeOf
 * @author Andre Sonntag
 * @date 19.07.2019
 */
public class CrySLPredicateExtractor {

	private static final Logger logger = LogManager.getLogger(NeverTypeOfPatch.class.getSimpleName());
	private AnalysisSeedWithSpecification seed;
	private CrySLPredicate predicate;

	public CrySLPredicateExtractor(AnalysisSeedWithSpecification seed, CrySLPredicate predicate) {
		this.seed = seed;
		this.predicate = predicate;
	}

	public PredicateConstraint extract() {
		String predName = predicate.getPredName();

		if (Constants.predefinedPreds.contains(predName)) {
			PredicateConstraint ret = handlePredefinedPreds();
			return ret;
		} else {
			logger.info("normal predicate -->" + predName);
			return null;
		}

	}
	
	private PredicateConstraint handlePredefinedPreds() {

		List<ICrySLPredicateParameter> parameters = predicate.getParameters();

		switch (predicate.getPredName()) {
		case "neverTypeOf":  //  neverTypeOf[variable, data type]
			String varName = ((CrySLObject) parameters.get(0)).getVarName();
			return handleNeverTypeOf(varName);
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
	
	/*
	 * We need to use the first and the last propagated type, since the CrySL rule object does not store if a datatype is an array or not
	 * 
	 * TODO: can be replace by CrySLEntity type.
	 */
	private PredicateConstraint handleNeverTypeOf(String variable)
	{
		for (CallSiteWithParamIndex cs : seed.getParameterAnalysis().getAllQuerySites()) {
			if (cs.getVarName().equals(variable)) {
				Collection<Type> vals = seed.getParameterAnalysis().getPropagatedTypes().get(cs);
				List valsList = new ArrayList(vals);
				return new PredicateConstraint(valsList.get(1).toString(), valsList.get(0).toString());
			}
		}
		return null;	
	}
}
