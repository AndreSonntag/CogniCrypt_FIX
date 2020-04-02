package de.upb.cognicryptfix.analysis;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
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
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.IncompleteOperationError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.analysis.errors.RequiredPredicateError;
import crypto.analysis.errors.TypestateError;
import crypto.extractparameter.CallSiteWithParamIndex;
import crypto.extractparameter.ExtractedValue;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.HeadlessRepairer;
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
	private List<String> ruleClassNames;
	private List<AbstractError> errors;
	private List<AbstractError> fMethodError;
	private List<AbstractError> nTypeOfError;
	private List<AbstractError> compValueError;
	private List<AbstractError> incompleteError;
	private List<AbstractError> reqPredicateError;
	private List<AbstractError> typeStateError;
	private List<AbstractError> ruleClassError;

	public CryptoAnalysisListener() {
		errors = Lists.newArrayList();
		fMethodError = Lists.newArrayList();
		nTypeOfError = Lists.newArrayList();
		compValueError = Lists.newArrayList();
		incompleteError = Lists.newArrayList();
		reqPredicateError = Lists.newArrayList();
		typeStateError = Lists.newArrayList();
		ruleClassError = Lists.newArrayList();
		ruleClassNames = Lists.newArrayList();
		
		for(CrySLRule rule : HeadlessRepairer.getCrySLRules()) {
			ruleClassNames.add(rule.getClassName());
		}
	}
	
	
	public void reportError(AbstractError error) {
		if(ruleClassNames.contains(error.getErrorLocation().getMethod().getDeclaringClass().toString())){
			ruleClassError.add(error);
		}	
		else if(error instanceof ForbiddenMethodError) {
			fMethodError.add(error);
		}
		else if(error instanceof NeverTypeOfError) {
			nTypeOfError.add(error);
		}
		else if(error instanceof TypestateError) {
			typeStateError.add(error);
		}
		else if(error instanceof ConstraintError) {
			compValueError.add(error);
		}
		else if(error instanceof IncompleteOperationError) {
			incompleteError.add(error);
		}
		else if(error instanceof RequiredPredicateError) {
			reqPredicateError.add(error);
		}
	}
	
	public void afterAnalysis() {
		
		errors.addAll(compValueError);
		errors.addAll(fMethodError);
		errors.addAll(nTypeOfError);
		errors.addAll(typeStateError);
		errors.addAll(incompleteError);
		errors.addAll(reqPredicateError);

		IPatcher patcher = new JimplePatcher();
		for(AbstractError e : errors) {
			patcher.getPatchedClass(e);
		}		
	}

	public void afterConstraintCheck(AnalysisSeedWithSpecification arg0) {}

	public void afterPredicateCheck(AnalysisSeedWithSpecification arg0) {}

	public void beforeAnalysis() {}

	public void beforeConstraintCheck(AnalysisSeedWithSpecification arg0) {}

	public void beforePredicateCheck(AnalysisSeedWithSpecification arg0) {}

	public void boomerangQueryFinished(Query arg0, BackwardQuery arg1) {}

	public void boomerangQueryStarted(Query arg0, BackwardQuery arg1) {}

	public void seedStarted(IAnalysisSeed arg0) {}

	public void checkedConstraints(AnalysisSeedWithSpecification arg0, Collection<ISLConstraint> arg1) {}

	public void collectedValues(AnalysisSeedWithSpecification arg0,
			Multimap<CallSiteWithParamIndex, ExtractedValue> arg1) {}

	public void discoveredSeed(IAnalysisSeed arg0) {}

	public void onSecureObjectFound(IAnalysisSeed arg0) {}

	public void onSeedFinished(IAnalysisSeed arg0, ForwardBoomerangResults<TransitionFunction> arg1) {}

	public void onSeedTimeout(Node<Statement, Val> arg0) {}

	@Override
	public void ensuredPredicates(Table<Statement, Val, Set<EnsuredCrySLPredicate>> arg0,
			Table<Statement, IAnalysisSeed, Set<CrySLPredicate>> arg1,
			Table<Statement, IAnalysisSeed, Set<CrySLPredicate>> arg2) {}

	@Override
	public void addProgress(int arg0, int arg1) {}

	
}
