package de.upb.cognicryptfix.patcher;


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import boomerang.jimple.Statement;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.CrySLResultsReporter;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.constraints.ConstraintSolver;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CryptSLComparisonConstraint;
import crypto.rules.CryptSLPredicate;
import crypto.rules.CryptSLValueConstraint;
import de.upb.cognicryptfix.analysis.CryptoAnalysisListener;
import de.upb.cognicryptfix.extractor.CryptSLComparsionConstraintExtractor;
import de.upb.cognicryptfix.extractor.CryptSLValueConstraintExtractor;
import de.upb.cognicryptfix.extractor.constraints.ComparisonConstraint;
import de.upb.cognicryptfix.extractor.constraints.ValueConstraint;
import de.upb.cognicryptfix.patcher.patches.PrimitiveConstraintPatch;
import soot.Body;
import soot.SootClass;
import soot.SootMethod;
import sync.pds.solver.nodes.Node;

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
//		error.getErrorLocation().getMethod().setActiveBody(patchedBody);		
		return errorClass;
	}
	
	private boolean verifyPatch(ConstraintError error){
		logger.info("Verify patch for "+error.getClass().getSimpleName()+" : " +error.toErrorMarkerString());

		AnalysisSeedWithSpecification seed = (AnalysisSeedWithSpecification)error.getObjectLocation();
		seed.execute();
		
		Collection<Statement> collectedCalls = new HashSet<Statement>();
		collectedCalls.add(error.getErrorLocation());		

		CrySLResultsReporter resultsAggregator = new CrySLResultsReporter();
		resultsAggregator.addReportListener(new CryptoAnalysisListener());
		
		ConstraintSolver solver = new ConstraintSolver(seed, collectedCalls, resultsAggregator);

		resultsAggregator.checkedConstraints(seed, solver.getRelConstraints());
	
		/*
		 * TODO: try to understand!
		 */
		boolean internalConstraintSatisfied = false;
		internalConstraintSatisfied = (0 == solver.evaluateRelConstraints());

	
		return internalConstraintSatisfied;
	}
	
	private Body createPatch(AbstractError error) {
		Body patchedJimpleBody = null;
		if (error instanceof ConstraintError) {
			ConstraintError conError = (ConstraintError) error;
			ISLConstraint brokenCon = conError.getBrokenConstraint();
			//logger.info(conError.getClass().getSimpleName() +"_"+brokenCon.getClass().getSimpleName()+" :" + conError.toErrorMarkerString());
			AnalysisSeedWithSpecification seed = (AnalysisSeedWithSpecification) conError.getObjectLocation();
			
				if(brokenCon instanceof CryptSLComparisonConstraint) {
					logger.info("Create patch for "+conError.getClass().getSimpleName() +"_"+brokenCon.getClass().getSimpleName()+" :" + conError.toErrorMarkerString());

					CryptSLComparsionConstraintExtractor extractor = new CryptSLComparsionConstraintExtractor(seed,(CryptSLComparisonConstraint)brokenCon);
					ComparisonConstraint primCon = extractor.extract();
					PrimitiveConstraintPatch patch = new PrimitiveConstraintPatch(conError, primCon);
					patchedJimpleBody = patch.getPatch();
					error.getErrorLocation().getMethod().setActiveBody(patchedJimpleBody);
					verifyPatch(conError);
				}
				if(brokenCon instanceof CryptSLValueConstraint) {
					
					CryptSLValueConstraintExtractor extractor = new CryptSLValueConstraintExtractor(seed, (CryptSLValueConstraint)brokenCon);
					ValueConstraint valCon = extractor.extract();
					PrimitiveConstraintPatch patch = new PrimitiveConstraintPatch(conError, valCon);
					patchedJimpleBody = patch.getPatch();
					error.getErrorLocation().getMethod().setActiveBody(patchedJimpleBody);
					verifyPatch(conError);
				}
				if(brokenCon instanceof CryptSLPredicate) {
					//TODO: NeverTypeOfError
				}
				
		}
		return patchedJimpleBody;
	}
	
}
