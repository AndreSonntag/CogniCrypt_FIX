package de.upb.cognicryptfix.analysis;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;
import com.google.common.collect.Table;

import boomerang.BackwardQuery;
import boomerang.Query;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.ForwardBoomerangResults;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.CrySLAnalysisListener;
import crypto.analysis.EnsuredCryptSLPredicate;
import crypto.analysis.IAnalysisSeed;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.ImpreciseValueExtractionError;
import crypto.analysis.errors.IncompleteOperationError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.analysis.errors.PredicateContradictionError;
import crypto.analysis.errors.RequiredPredicateError;
import crypto.analysis.errors.TypestateError;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CryptSLComparisonConstraint;
import crypto.rules.CryptSLConstraint;
import crypto.rules.CryptSLPredicate;
import crypto.rules.CryptSLValueConstraint;
import de.upb.cognicryptfix.utils.MavenProject;
import sync.pds.solver.nodes.Node;
import typestate.TransitionFunction;


/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class CryptoAnalysisListener extends CrySLAnalysisListener{

	private static final Logger logger = LogManager.getLogger(CryptoAnalysisListener.class.getSimpleName());
	private final HashMap<String, String> projectStructure;
	
	public CryptoAnalysisListener(MavenProject mp) {
		this.projectStructure = mp.getProjectStructureHashMap();
	}
	
	public void reportError(AbstractError error) {

		if (error instanceof ForbiddenMethodError) {
			ForbiddenMethodError forbiddenMethError = (ForbiddenMethodError) error;
			logger.info(forbiddenMethError.getClass().getSimpleName() + " : " + forbiddenMethError.toErrorMarkerString());

		} else if (error instanceof PredicateContradictionError) {
			PredicateContradictionError predContError = (PredicateContradictionError) error;
			logger.info(predContError.getClass().getSimpleName() + " : " + predContError.toErrorMarkerString());

		} else if (error instanceof RequiredPredicateError) {
			RequiredPredicateError requiredPredError = (RequiredPredicateError) error;
			logger.info(requiredPredError.getClass().getSimpleName() + " : " + requiredPredError.toErrorMarkerString());

		} else if (error instanceof NeverTypeOfError) {
			NeverTypeOfError nevTypeError = (NeverTypeOfError) error;
			logger.info(nevTypeError.getClass().getSimpleName() + " : " + nevTypeError.toErrorMarkerString());

		} else if (error instanceof ConstraintError) {
			ConstraintError conError = (ConstraintError) error;
			logger.info(conError.getClass().getSimpleName() + " :" + conError.toErrorMarkerString());
		
		} else if (error instanceof IncompleteOperationError) {
			IncompleteOperationError incompleteOpError = (IncompleteOperationError) error;
			logger.info(incompleteOpError.getClass().getSimpleName() + " : " + incompleteOpError.toErrorMarkerString());

		} else if (error instanceof TypestateError) {
			ForbiddenMethodError typeStatError = (ForbiddenMethodError) error;
			logger.info(typeStatError.getClass().getSimpleName() + " : " + typeStatError.toErrorMarkerString());

		} else if (error instanceof ImpreciseValueExtractionError) {
			ImpreciseValueExtractionError impreciseValExtError = (ImpreciseValueExtractionError) error;
			logger.info(impreciseValExtError.getClass().getSimpleName() + " : "
					+ impreciseValExtError.toErrorMarkerString());
		}
		
	}
	
	
	public void afterAnalysis() {
		// TODO Auto-generated method stub
		
	}

	public void afterConstraintCheck(AnalysisSeedWithSpecification arg0) {
		// TODO Auto-generated method stub
		
	}

	public void afterPredicateCheck(AnalysisSeedWithSpecification arg0) {
		// TODO Auto-generated method stub
		
	}

	public void beforeAnalysis() {
		// TODO Auto-generated method stub
		
	}

	public void beforeConstraintCheck(AnalysisSeedWithSpecification arg0) {
		// TODO Auto-generated method stub
		
	}

	public void beforePredicateCheck(AnalysisSeedWithSpecification arg0) {
		// TODO Auto-generated method stub
		
	}

	public void boomerangQueryFinished(Query arg0, BackwardQuery arg1) {
		// TODO Auto-generated method stub
		
	}

	public void boomerangQueryStarted(Query arg0, BackwardQuery arg1) {
		// TODO Auto-generated method stub
		
	}

	public void ensuredPredicates(Table<Statement, Val, Set<EnsuredCryptSLPredicate>> arg0,
			Table<Statement, IAnalysisSeed, Set<CryptSLPredicate>> arg1,
			Table<Statement, IAnalysisSeed, Set<CryptSLPredicate>> arg2) {
		// TODO Auto-generated method stub
		
	}

	public void seedStarted(IAnalysisSeed arg0) {
		// TODO Auto-generated method stub
		
	}

	public void checkedConstraints(AnalysisSeedWithSpecification arg0, Collection<ISLConstraint> arg1) {
		// TODO Auto-generated method stub
		
	}

	public void collectedValues(AnalysisSeedWithSpecification arg0,
			Multimap<CallSiteWithParamIndex, ExtractedValue> arg1) {
		// TODO Auto-generated method stub
		
	}

	public void discoveredSeed(IAnalysisSeed arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onSecureObjectFound(IAnalysisSeed arg0) {
		// TODO Auto-generated method stub
		
	}

	public void onSeedFinished(IAnalysisSeed arg0, ForwardBoomerangResults<TransitionFunction> arg1) {
		// TODO Auto-generated method stub
		
	}

	public void onSeedTimeout(Node<Statement, Val> arg0) {
		// TODO Auto-generated method stub
		
	}

	
}
