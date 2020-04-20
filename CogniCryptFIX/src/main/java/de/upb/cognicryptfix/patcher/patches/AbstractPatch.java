package de.upb.cognicryptfix.patcher.patches;

import de.upb.cognicryptfix.exception.patch.RepairException;
import soot.Body;

public abstract class AbstractPatch {
	
	public abstract Body applyPatch() throws RepairException;
	public abstract String toPatchString();
	
	/*-
	 * Preferred field order and name convention
	 * 
	 * 1. AbstractError		error
	 * 2. ISLConstraint		constraint?
	 * 3. AnalysisSeed		seed
	 * 
	 * 4. CrySLEntity		entity
	 * 5. Body				body
	 * 6. CodeGenerator		generator
	 * 7. ObservableICFG	icfg
	 * 8. List<Unit>		patch
	 */

	/*-
	 * @override
	 * applyPatch(){
	 * 
	 * contains exception handling!	
	 * 
	 * }
	 * 
	 * 
	 * 
	 */

}
