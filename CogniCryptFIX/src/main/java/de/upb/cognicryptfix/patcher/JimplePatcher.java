package de.upb.cognicryptfix.patcher;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Stopwatch;

import crypto.analysis.errors.AbstractError;
import crypto.analysis.errors.ConstraintError;
import crypto.analysis.errors.ForbiddenMethodError;
import crypto.analysis.errors.HardCodedError;
import crypto.analysis.errors.IncompleteOperationError;
import crypto.analysis.errors.NeverTypeOfError;
import crypto.analysis.errors.RequiredPredicateError;
import crypto.analysis.errors.TypestateError;
import crypto.rules.CrySLPredicate;
import de.upb.cognicryptfix.exception.patch.RepairException;
import de.upb.cognicryptfix.patcher.patches.ConstraintPatch;
import de.upb.cognicryptfix.patcher.patches.ForbiddenMethodPatch;
import de.upb.cognicryptfix.patcher.patches.HardCodedPatch;
import de.upb.cognicryptfix.patcher.patches.IncompleteOperationPatch;
import de.upb.cognicryptfix.patcher.patches.NeverTypeOfPatch;
import de.upb.cognicryptfix.patcher.patches.RequiredPredicatePatch;
import de.upb.cognicryptfix.patcher.patches.TypeStatePatch;
import de.upb.cognicryptfix.utils.CallGraphPrinter;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.MethodOrMethodContext;
import soot.Scene;
import soot.SootClass;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.util.Chain;

/**
 * @author Andre Sonntag
 * @date 21.09.2019
 */
public class JimplePatcher implements IPatcher{

	private static final Logger LOGGER = LogManager.getLogger(JimplePatcher.class);
	private static JimplePatcher instance;

	private JimplePatcher() {}
	
	public static JimplePatcher getInstance() {
		if (JimplePatcher.instance == null) {
			JimplePatcher.instance = new JimplePatcher();
		}
		return JimplePatcher.instance;
	}
	
	public SootClass getPatchedClass(AbstractError error) throws RepairException {	
		SootClass errorClass = error.getErrorLocation().getMethod().getDeclaringClass();
		Body errorBody = error.getErrorLocation().getMethod().getActiveBody();
		replaceInvalidLocalNames(errorBody.getLocals());		
		createPatch(error);
		return errorClass;
	}	
	
	private void createPatch(AbstractError error) throws RepairException {
		Body body = error.getErrorLocation().getMethod().getActiveBody();
		LOGGER.debug(Utils.printErrorInformation(error,true));
		
		//LOGGER.debug("BEFORE PATCH: \n"+body.toString());
		//CallGraph ch = Scene.v().getCallGraph();
		//CallGraphPrinter.serializeCallGraph(ch, "callGraph.dot");	
		
		if (error instanceof ConstraintError && !(((ConstraintError) error).getBrokenConstraint() instanceof CrySLPredicate)) {
			ConstraintError conError = (ConstraintError) error;	
			LOGGER.debug("Time measurement: ConstraintError");
			Stopwatch stopwatch = Stopwatch.createStarted();
			ConstraintPatch patch = new ConstraintPatch(conError);
			body = patch.applyPatch();
			stopwatch.stop();
			long timeElapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
			LOGGER.info(patch.toPatchString());
			LOGGER.info("\nConstraintError repairing took " + timeElapsed + " microseconds!");
			
		}
		else if (error instanceof NeverTypeOfError) {
			NeverTypeOfError nTypeError = (NeverTypeOfError) error;
			LOGGER.debug("Time measurement: ConstraintError");
			Stopwatch stopwatch = Stopwatch.createStarted();
			NeverTypeOfPatch patch = new NeverTypeOfPatch(nTypeError);
			body = patch.applyPatch();
			stopwatch.stop();
			long timeElapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
			LOGGER.info(patch.toPatchString());	
			LOGGER.info("\nNeverTypeOfError repairing took " + timeElapsed  + " microseconds!");
		}
		else if (error instanceof HardCodedError) {
			HardCodedError hardCodedError = (HardCodedError) error;
			LOGGER.debug("Time measurement: HardCodedError");
			Stopwatch stopwatch = Stopwatch.createStarted();
			HardCodedPatch patch = new HardCodedPatch(hardCodedError);
			body = patch.applyPatch();
			LOGGER.info(patch.toPatchString());			
			stopwatch.stop();
			long timeElapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
			LOGGER.info("\nHardCodedError repairing took " + timeElapsed  + " microseconds!");
			stopwatch.reset();
		}
		else if (error instanceof ForbiddenMethodError) {
			ForbiddenMethodError fMethodError = (ForbiddenMethodError) error;	
			LOGGER.debug("Time measurement: ForbiddenMethodError");
			Stopwatch stopwatch = Stopwatch.createStarted();
			ForbiddenMethodPatch patch = new ForbiddenMethodPatch(fMethodError);
			body = patch.applyPatch();
			long timeElapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
			LOGGER.info(patch.toPatchString());		
			LOGGER.info("\nForbiddenMethodError repairing took " + timeElapsed  + " microseconds!");
			stopwatch.reset();
		}
		else if(error instanceof TypestateError) {
			TypestateError typeStateError = (TypestateError) error;
			LOGGER.debug("Time measurement: TypestateError");
			Stopwatch stopwatch = Stopwatch.createStarted();
			TypeStatePatch patch = new TypeStatePatch(typeStateError);
			body = patch.applyPatch();
			long timeElapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
			LOGGER.info(patch.toPatchString());
			LOGGER.info("\nTypestateError repairing took " + timeElapsed  + " microseconds!");
			stopwatch.reset();
		}
		else if(error instanceof IncompleteOperationError) {
			IncompleteOperationError incompleteError = (IncompleteOperationError) error;
			LOGGER.debug("Time measurement: IncompleteOperationError");
			Stopwatch stopwatch = Stopwatch.createStarted();
			IncompleteOperationPatch patch = new IncompleteOperationPatch(incompleteError);
			body = patch.applyPatch();
			long timeElapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
			LOGGER.info(patch.toPatchString());
			LOGGER.info("\nIncompleteOperationError repairing took " + timeElapsed  + " microseconds!");
			stopwatch.reset();
		}
		else if (error instanceof RequiredPredicateError) {
			RequiredPredicateError reqPredicateErrpr = (RequiredPredicateError) error;
			LOGGER.debug("Time measurement: RequiredPredicateError");
			Stopwatch stopwatch = Stopwatch.createStarted();
			RequiredPredicatePatch patch = new RequiredPredicatePatch(reqPredicateErrpr);
			body = patch.applyPatch();
			long timeElapsed = stopwatch.elapsed(TimeUnit.MICROSECONDS);
			LOGGER.info(patch.toPatchString());	
			LOGGER.info("\nRequiredPredicateError repairing took " + timeElapsed + " microseconds!");
			stopwatch.reset();
		}
		LOGGER.debug("AFTER PATCH: \n"+body.toString());
	}
	
	private void replaceInvalidLocalNames(Chain<Local> locals) {
		for(Local l : locals) {
			if(l.getName().contains("#")) {
				l.setName(l.getName().replaceFirst("#", ""));
			}
		}
		
	}
	
	
}
