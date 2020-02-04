package de.upb.cognicryptfix.analysis;

import java.util.Collection;
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
import crypto.analysis.EnsuredCrySLPredicate;
import crypto.analysis.IAnalysisSeed;
import crypto.analysis.errors.AbstractError;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLPredicate;
import de.upb.cognicryptfix.patcher.IPatcher;
import de.upb.cognicryptfix.patcher.JimplePatcher;
import sync.pds.solver.nodes.Node;
import typestate.TransitionFunction;


/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class CryptoAnalysisListener extends CrySLAnalysisListener{

	private static final Logger logger = LogManager.getLogger(CryptoAnalysisListener.class.getSimpleName());
	
	public CryptoAnalysisListener() {
	}
	
	public void reportError(AbstractError error) {
		IPatcher patcher = new JimplePatcher();
		patcher.getPatchedClass(error);
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

	@Override
	public void ensuredPredicates(Table<Statement, Val, Set<EnsuredCrySLPredicate>> arg0,
			Table<Statement, IAnalysisSeed, Set<CrySLPredicate>> arg1,
			Table<Statement, IAnalysisSeed, Set<CrySLPredicate>> arg2) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addProgress(int arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	
}
