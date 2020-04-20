package de.upb.cognicryptfix.patcher;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.Scene;
import soot.SootClass;
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
		LOGGER.debug(Utils.printErrorInformation(error));
		LOGGER.debug("BEFORE PATCH: \n"+body.toString());
		
		if (error instanceof ConstraintError && !(((ConstraintError) error).getBrokenConstraint() instanceof CrySLPredicate)) {
			ConstraintError conError = (ConstraintError) error;			
			ConstraintPatch patch = new ConstraintPatch(conError);
			body = patch.applyPatch();
			LOGGER.info(patch.toPatchString());			
		}
		else if (error instanceof NeverTypeOfError) {
			
			NeverTypeOfError nTypeError = (NeverTypeOfError) error;
			NeverTypeOfPatch patch = new NeverTypeOfPatch(nTypeError);
			body = patch.applyPatch();
			LOGGER.info(patch.toPatchString());			
		}
		else if (error instanceof HardCodedError) {
			
			HardCodedError hardCodedError = (HardCodedError) error;
			HardCodedPatch patch = new HardCodedPatch(hardCodedError);
			body = patch.applyPatch();
			LOGGER.info(patch.toPatchString());			
		}
		else if (error instanceof ForbiddenMethodError) {
			
			ForbiddenMethodError fMethodError = (ForbiddenMethodError) error;			
			ForbiddenMethodPatch patch = new ForbiddenMethodPatch(fMethodError);
			body = patch.applyPatch();
			LOGGER.info(patch.toPatchString());			
		}
		else if(error instanceof TypestateError) {
			
			TypestateError typeStateError = (TypestateError) error;
			TypeStatePatch patch = new TypeStatePatch(typeStateError);
			body = patch.applyPatch();
			LOGGER.info(patch.toPatchString());			
		}
		else if(error instanceof IncompleteOperationError) {

			IncompleteOperationError incompleteError = (IncompleteOperationError) error;
			IncompleteOperationPatch patch = new IncompleteOperationPatch(incompleteError);
			body = patch.applyPatch();
			LOGGER.info(patch.toPatchString());			
		}
		else if (error instanceof RequiredPredicateError) {
			
			RequiredPredicateError reqPredicateErrpr = (RequiredPredicateError) error;
			RequiredPredicatePatch patch = new RequiredPredicatePatch(reqPredicateErrpr);
			body = patch.applyPatch();
			LOGGER.info(patch.toPatchString());			
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
