package de.upb.cognicryptfix.patcher;


import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.analysis.CrySLResultsReporter;
import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.constraints.ConstraintSolver;
import crypto.interfaces.ISLConstraint;
import crypto.rules.CryptSLComparisonConstraint;
import de.upb.cognicryptfix.analysis.CryptoAnalysisListener;
import de.upb.cognicryptfix.extractor.CryptSLComparsionConstraintExtractor;
import de.upb.cognicryptfix.patcher.patches.PrimitiveConstraintPatch;
import de.upb.cognicryptfix.utils.PrimitiveConstraint;
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

	public SootClass getPatchedClass(AbstractError error) {
		SootClass errorClass = error.getErrorLocation().getMethod().getDeclaringClass();
		Body patchedBody = createPatch(error);
//		error.getErrorLocation().getMethod().setActiveBody(patchedBody);		
		return errorClass;
	}
	
	private boolean verifyPatch(ConstraintError error){
		AnalysisSeedWithSpecification seed = (AnalysisSeedWithSpecification)error.getObjectLocation();
		seed.execute();
		
		Collection<Statement> collectedCalls = new HashSet<Statement>();
		collectedCalls.add(error.getErrorLocation());		

		CrySLResultsReporter resultsAggregator = new CrySLResultsReporter();
		resultsAggregator.addReportListener(new CryptoAnalysisListener());
		
		ConstraintSolver solver = new ConstraintSolver(seed, collectedCalls, resultsAggregator);

		resultsAggregator.checkedConstraints(seed, solver.getRelConstraints());
		boolean internalConstraintSatisfied = false;
		internalConstraintSatisfied = (0 == solver.evaluateRelConstraints());
		
		return true;
	}
	
	private Body createPatch(AbstractError error) {
		
		Body patchedJimpleBody = null;
		
		if (error instanceof ConstraintError) {
			ConstraintError conError = (ConstraintError) error;
			logger.info(conError.getClass().getSimpleName() + " :" + conError.toErrorMarkerString());
			ISLConstraint brokenCon = conError.getBrokenConstraint();
			AnalysisSeedWithSpecification seed = (AnalysisSeedWithSpecification) conError.getObjectLocation();
			
				if(brokenCon instanceof CryptSLComparisonConstraint) {
					CryptSLComparsionConstraintExtractor extractor = new CryptSLComparsionConstraintExtractor(seed,brokenCon);
					PrimitiveConstraint primCon = extractor.extract();
					logger.info(primCon.toString());
					PrimitiveConstraintPatch patch = new PrimitiveConstraintPatch(conError, primCon);
					patchedJimpleBody = patch.getPatch();
					
					error.getErrorLocation().getMethod().setActiveBody(patchedJimpleBody);
					verifyPatch(conError);
				}
				
		}
		return patchedJimpleBody;
	}
	
}
