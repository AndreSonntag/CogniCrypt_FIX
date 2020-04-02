package de.upb.cognicryptfix.patcher;


import javax.activation.UnsupportedDataTypeException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.IncompleteOperationError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.analysis.errors.RequiredPredicateError;
import crypto.analysis.errors.TypestateError;
import crypto.rules.CrySLPredicate;
import de.upb.cognicryptfix.exception.NotExpectedUnitException;
import de.upb.cognicryptfix.exception.NotSupportedConstraintException;
import de.upb.cognicryptfix.patcher.patches.ConstraintPatch;
import de.upb.cognicryptfix.patcher.patches.ForbiddenMethodPatch;
import de.upb.cognicryptfix.patcher.patches.IncompleteOperationPatch;
import de.upb.cognicryptfix.patcher.patches.NeverTypeOfPatch;
import de.upb.cognicryptfix.patcher.patches.RequiredPredicatePatch;
import de.upb.cognicryptfix.patcher.patches.TypeStatePatch;
import soot.Body;
import soot.SootClass;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class JimplePatcher implements IPatcher{

	private static final Logger logger = LogManager.getLogger(JimplePatcher.class.getSimpleName());
	
	public SootClass getPatchedClass(AbstractError error) {
		SootClass errorClass = error.getErrorLocation().getMethod().getDeclaringClass();
		try {
			error.getErrorLocation().getMethod().setActiveBody(createPatch(error));
		} catch (NotSupportedConstraintException | NotExpectedUnitException | UnsupportedDataTypeException e) {
			e.printStackTrace();
		}
		return errorClass;
	}	
	
	private Body createPatch(AbstractError error) throws NotSupportedConstraintException, NotExpectedUnitException, UnsupportedDataTypeException {
		
		printErrorInformation(error);
		Body patchedJimpleBody = error.getErrorLocation().getMethod().getActiveBody();
		
		if (error instanceof ConstraintError && !(((ConstraintError) error).getBrokenConstraint() instanceof CrySLPredicate)) {
			ConstraintError conError = (ConstraintError) error;			
			ConstraintPatch patch = new ConstraintPatch(conError);
			patchedJimpleBody = patch.applyPatch();
			logger.info(patch.toPatchString());			
		}
		else if (error instanceof ForbiddenMethodError) {
			
			ForbiddenMethodError fMethodError = (ForbiddenMethodError) error;			
			ForbiddenMethodPatch patch = new ForbiddenMethodPatch(fMethodError);
			patchedJimpleBody = patch.applyPatch();
			logger.info(patch.toPatchString());			
		}
		else if (error instanceof NeverTypeOfError) {
			
			NeverTypeOfError nTypeError = (NeverTypeOfError) error;
			NeverTypeOfPatch patch = new NeverTypeOfPatch(nTypeError);
			patchedJimpleBody = patch.applyPatch();
			logger.info(patch.toPatchString());			
		}
		else if(error instanceof TypestateError) {
			
			TypestateError typeStateError = (TypestateError) error;
			TypeStatePatch patch = new TypeStatePatch(typeStateError);
			patch.applyPatch();
			logger.info(patch.toPatchString());			
		}
		else if(error instanceof IncompleteOperationError) {

			IncompleteOperationError incompleteError = (IncompleteOperationError) error;
			IncompleteOperationPatch patch = new IncompleteOperationPatch(incompleteError);
			patch.applyPatch();
			logger.info(patch.toPatchString());			
		}
		else if (error instanceof RequiredPredicateError) {
			
			RequiredPredicateError reqPredicateErrpr = (RequiredPredicateError) error;
			RequiredPredicatePatch patch = new RequiredPredicatePatch(reqPredicateErrpr);
			patchedJimpleBody = patch.applyPatch();
			logger.info(patch.toPatchString());			
		}

		return patchedJimpleBody;
	}
	
	
	private void printErrorInformation(AbstractError error) {
		String errorType = "";
		String errorRule = error.getRule().getClassName();
		String errorClass = error.getErrorLocation().getMethod().getDeclaringClass().toString();
		String errorOuterMethod = error.getErrorLocation().getMethod().getSignature();
		String errorMessage = error.toErrorMarkerString();
		
		if (error instanceof ConstraintError) {
			errorType = "ConstraintError";
		} else if (error instanceof ForbiddenMethodError) {
			errorType = "ForbiddenMethodError";
		} else if (error instanceof NeverTypeOfError) {
			errorType = "NeverTypeOfError";
		} else if(error instanceof TypestateError) {
			errorType = "TypestateError";
		} else if(error instanceof IncompleteOperationError) {
			errorType = "IncompleteOperationError";
		} else if (error instanceof RequiredPredicateError) {
			errorType = "RequiredPredicateError";
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("\n_________________________________________CogniCrypt_SAST_________________________________________\n");
		builder.append("Detected: \t"+errorType+"\n");
		builder.append("CrySLRule: \t"+errorRule+"\n");
		builder.append("Class: \t\t"+errorClass+"\n");
		builder.append("Method: \t"+errorOuterMethod+"\n");
		builder.append("Message: \t"+errorMessage+"\n");
		builder.append("_________________________________________________________________________________________________\n");
		logger.info(builder.toString());
		
	}
	
	
	
	/*
	 * Notes: Maybe just useful vor value constraints ??
	 */
//	private int verifyPatch2(AbstractError error){
//		logger.info("Verify patch for "+error.getClass().getSimpleName()+" : " +error.toErrorMarkerString());
//
//		AnalysisSeedWithSpecification seed = null;
//		if(error instanceof ConstraintError) {
//			ConstraintError conError = (ConstraintError) error;
//			seed = (AnalysisSeedWithSpecification)conError.getObjectLocation();
//		}else {
//			seed = Utils.createSeed(error.getRule(), error.getErrorLocation().getMethod());
//
//		}
//		seed.execute();
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
//		
//		int errors = solver.evaluateRelConstraints();
//		return errors;
//	}
	
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
