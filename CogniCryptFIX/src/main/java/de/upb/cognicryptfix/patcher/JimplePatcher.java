package de.upb.cognicryptfix.patcher;


import java.util.Collection;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import boomerang.jimple.Statement;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.CrySLResultsReporter;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.constraints.ConstraintSolver;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLComparisonConstraint;
import crypto.rules.CrySLPredicate;
import crypto.rules.CrySLValueConstraint;
import de.upb.cognicryptfix.analysis.CryptoAnalysisListener;
import de.upb.cognicryptfix.extractor.CrySLComparsionConstraintExtractor;
import de.upb.cognicryptfix.extractor.CrySLPredicateExtractor;
import de.upb.cognicryptfix.extractor.CrySLValueConstraintExtractor;
import de.upb.cognicryptfix.extractor.constraints.ComparisonConstraint;
import de.upb.cognicryptfix.extractor.constraints.PredicateConstraint;
import de.upb.cognicryptfix.extractor.constraints.ValueConstraint;
import de.upb.cognicryptfix.patcher.patches.ForbiddenMethodPatch;
import de.upb.cognicryptfix.patcher.patches.NeverTypeOfPatch;
import de.upb.cognicryptfix.patcher.patches.PrimitiveConstraintPatch;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.SootClass;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class JimplePatcher implements IPatcher{

	private static final Logger logger = LogManager.getLogger(JimplePatcher.class.getSimpleName());
	private static int counter = 0;
	
	public SootClass getPatchedClass(AbstractError error) {
		SootClass errorClass = error.getErrorLocation().getMethod().getDeclaringClass();
		Body patchedBody = createPatch(error);
		int errorCounter = verifyPatch2(error);
		
		if(errorCounter > 0) {
			patchedBody = createPatch(error);
			errorCounter = verifyPatch2(error);
		}
		
		
	//	error.getErrorLocation().getMethod().setActiveBody(patchedBody);

		return errorClass;
	}
	
	private Body createPatch(AbstractError error) {
		Body patchedJimpleBody = error.getErrorLocation().getMethod().getActiveBody();
		
		if (error instanceof ForbiddenMethodError) {
			ForbiddenMethodError fMethodError = (ForbiddenMethodError) error;			
			ForbiddenMethodPatch patch = new ForbiddenMethodPatch(fMethodError);
			patch.getPatch();
			logger.info("Create patch for "+fMethodError.getClass().getSimpleName());
			logger.info(patch.toString());
		}
		else if (error instanceof NeverTypeOfError) {
			NeverTypeOfError nTypeError = (NeverTypeOfError) error;
			AnalysisSeedWithSpecification seed = (AnalysisSeedWithSpecification) nTypeError.getObjectLocation();

			CrySLPredicateExtractor extractor = new CrySLPredicateExtractor(seed, (CrySLPredicate) nTypeError.getBrokenConstraint());
			PredicateConstraint predCon = extractor.extract();
			
			NeverTypeOfPatch patch = new NeverTypeOfPatch(nTypeError, predCon);
			patchedJimpleBody = patch.getPatch();
			logger.info("Create patch for "+nTypeError.getClass().getSimpleName());
			logger.info(patch.toString());
		}
		else if (error instanceof ConstraintError) {
			ConstraintError conError = (ConstraintError) error;
			ISLConstraint brokenCon = conError.getBrokenConstraint();
			//logger.info(conError.getClass().getSimpleName() +"_"+brokenCon.getClass().getSimpleName()+" :" + conError.toErrorMarkerString());
			AnalysisSeedWithSpecification seed = (AnalysisSeedWithSpecification) conError.getObjectLocation();
			
				if(brokenCon instanceof CrySLComparisonConstraint) {
					logger.info("Create patch for "+conError.getClass().getSimpleName() +"_"+brokenCon.getClass().getSimpleName()+" :" + conError.toErrorMarkerString());

					CrySLComparsionConstraintExtractor extractor = new CrySLComparsionConstraintExtractor(seed , (CrySLComparisonConstraint) brokenCon);
					ComparisonConstraint primCon = extractor.extract();
					PrimitiveConstraintPatch patch = new PrimitiveConstraintPatch(conError, primCon);
					patchedJimpleBody = patch.getPatch();
					error.getErrorLocation().getMethod().setActiveBody(patchedJimpleBody);
					logger.info(patch.toString());
				}
				if(brokenCon instanceof CrySLValueConstraint) {
					logger.info("Create patch for "+conError.getClass().getSimpleName() +"_"+brokenCon.getClass().getSimpleName()+" :" + conError.toErrorMarkerString());

					CrySLValueConstraintExtractor extractor = new CrySLValueConstraintExtractor(seed, (CrySLValueConstraint) brokenCon);
					ValueConstraint valCon = extractor.extract();
					PrimitiveConstraintPatch patch = new PrimitiveConstraintPatch(conError, valCon);
					patchedJimpleBody = patch.getPatch();
					error.getErrorLocation().getMethod().setActiveBody(patchedJimpleBody);
					logger.info(patch.toString());
				}				
		}

		return patchedJimpleBody;
	}
	
	/*
	 * Notes: Maybe just useful vor value constraints ??
	 */
	private int verifyPatch2(AbstractError error){
		logger.info("Verify patch for "+error.getClass().getSimpleName()+" : " +error.toErrorMarkerString());

		AnalysisSeedWithSpecification seed = null;
		if(error instanceof ConstraintError) {
			ConstraintError conError = (ConstraintError) error;
			seed = (AnalysisSeedWithSpecification)conError.getObjectLocation();
		}else {
			seed = Utils.createSeed(error.getRule(), error.getErrorLocation().getMethod());

		}
		seed.execute();
		Collection<Statement> collectedCalls = new HashSet<Statement>();
		collectedCalls.add(error.getErrorLocation());		

		CrySLResultsReporter resultsAggregator = new CrySLResultsReporter();
		resultsAggregator.addReportListener(new CryptoAnalysisListener());
		
		ConstraintSolver solver = new ConstraintSolver(seed, collectedCalls, resultsAggregator);

		resultsAggregator.checkedConstraints(seed, solver.getRelConstraints());
		
		
		int errors = solver.evaluateRelConstraints();
		return errors;
	}
	
//	/*
//	 * Notes: Maybe just useful vor value constraints ??
//	 */
//	private boolean verifyPatch(ConstraintError error){
//		logger.info("Verify patch for "+error.getClass().getSimpleName()+" : " +error.toErrorMarkerString());
//
//		AnalysisSeedWithSpecification seed = (AnalysisSeedWithSpecification) error.getObjectLocation();
//		seed.execute();
//		//seed.getParameterAnalysis().getCollectedValues()
//		Collection<Statement> collectedCalls = new HashSet<Statement>();
//		collectedCalls.add(error.getErrorLocation());		
//
//		CrySLResultsReporter resultsAggregator = new CrySLResultsReporter();
//		resultsAggregator.addReportListener(new CryptoAnalysisListener());
//		
//		ConstraintSolver solver = new ConstraintSolver(seed, collectedCalls, resultsAggregator);
//
//		resultsAggregator.checkedConstraints(seed, solver.getRelConstraints());
//	
//		/*
//		 * TODO: try to understand!
//		 */
//		boolean internalConstraintSatisfied = false;
//		internalConstraintSatisfied = (0 == solver.evaluateRelConstraints());
//
//	
//		return internalConstraintSatisfied;
//	}
	
}
