package de.upb.cognicryptfix.generator.jimple;

import java.util.ArrayList;
import java.util.List;

import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Trap;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.Jimple;
import soot.util.Chain;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleTrapGenerator {

	private Body body;
	private JimpleLocalGenerator localGenerator;
	private JimpleInvokeGenerator invokeGenerator;
	private JimpleAssignGenerator assignGenerator;

	public JimpleTrapGenerator(Body body) {
		this.localGenerator = new JimpleLocalGenerator(body);
		this.invokeGenerator = new JimpleInvokeGenerator();
		this.assignGenerator = new JimpleAssignGenerator();
		this.body = body;
	}

	public void generateTraps(SootMethod method, List<Unit> tryUnits) {
		List<SootClass> exceptions = method.getExceptions();

		// remove caught exceptions
		removeCaughtExceptionsByThrown(body, exceptions);
		removeCaughtExceptionsByCatch(body, exceptions, tryUnits);
		if (Utils.isNullOrEmpty(exceptions)) {
			return;
		}

		// Try block
		Unit startTryBlockUnit = tryUnits.get(0);
		Unit endTryBlockUnit = tryUnits.get(tryUnits.size() - 1);

		// null initialization
		Unit nullAssign = null;
		if (startTryBlockUnit instanceof AssignStmt) {
			AssignStmt assign = (AssignStmt) startTryBlockUnit;
			Value variable = assign.getLeftOpBox().getValue();
			nullAssign = assignGenerator.generateAssignStmt(variable, JimpleUtils.generateConstantValue(null));
			body.getUnits().insertBefore(nullAssign, body.getUnits().getPredOf(startTryBlockUnit)); 
		}
		
		Unit endTryBlockUnitSucc = Jimple.v().newGotoStmt(body.getUnits().getSuccOf(endTryBlockUnit));

		// Catch block;
		for (SootClass exception : exceptions) {

			SootClass thrwoableClass = Scene.v().getSootClass("java.lang.Throwable");
			RefType thrwoableRefType = RefType.v(thrwoableClass.getName());
			Local thrwoableTempLocal = localGenerator.generateFreshLocal(thrwoableRefType);

			Unit caughtStmt = Jimple.v().newIdentityStmt(thrwoableTempLocal, Jimple.v().newCaughtExceptionRef());

			Local thrwoableELocal = localGenerator.generateFreshLocal(thrwoableRefType, "e");
			Unit localAssign = assignGenerator.generateAssignStmt(thrwoableELocal, thrwoableTempLocal);

			RefType exceptionRefType = RefType.v(exception.getName());
			Local exceptionTempLocal = localGenerator.generateFreshLocal(exceptionRefType);
			Value castExpr = Jimple.v().newCastExpr(thrwoableELocal, exceptionRefType);

			Unit castAssign = assignGenerator.generateAssignStmt(exceptionTempLocal, castExpr);
			SootMethod mPrintStackTrace = thrwoableClass.getMethod("void printStackTrace()");
			Unit printStackTraceInvokeStmt = invokeGenerator.generateInvokeStmt(exceptionTempLocal, mPrintStackTrace);

			List<Unit> catchBlock = new ArrayList<Unit>();
			catchBlock.add(endTryBlockUnitSucc);
			catchBlock.add(caughtStmt);
			catchBlock.add(localAssign);
			catchBlock.add(castAssign);
			catchBlock.add(printStackTraceInvokeStmt);

			insertRightBeforeNoRedirect(body, catchBlock, body.getUnits().getSuccOf(endTryBlockUnit));
			body.getTraps().add(Jimple.v().newTrap(exception, startTryBlockUnit, endTryBlockUnitSucc, caughtStmt));
		}
	}

	private static void insertRightBeforeNoRedirect(Body body, List<Unit> catchBlock, Unit s) {
		assert !(s instanceof IdentityStmt);
		for (Unit stmt : catchBlock)
			body.getUnits().insertBeforeNoRedirect(stmt, s);
	}

	private static void removeCaughtExceptionsByThrown(Body body, List<SootClass> exceptions) {
		List<SootClass> throwsExceptions = body.getMethod().getExceptions();
		// remove exception that caught by "thrown"
		exceptions.removeAll(throwsExceptions);

		// remove subclasses of caught exceptions by "thrown"
		for (SootClass throwsException : throwsExceptions) {
			for (SootClass exception : new ArrayList<>(exceptions)) { // iterating over a copy
				if (JimpleUtils.getFastHierarchy().isSubclass(exception, throwsException)) {
					exceptions.remove(exception);
				}
			}
		}
	}

	private static void removeCaughtExceptionsByCatch(Body body, List<SootClass> exceptions, List<Unit> tryUnits) {

		Chain<Trap> traps = body.getTraps();
		ArrayList bodyUnits = new ArrayList(body.getUnits());

		for (Trap trap : traps) {
			// TODO: check if it is necessary!
			List<Unit> trapTryUnits = bodyUnits.subList(bodyUnits.indexOf(trap.getBeginUnit()),
					bodyUnits.indexOf(trap.getEndUnit()));
			SootClass trapCatchException = trap.getException();
			if (trapTryUnits.containsAll(tryUnits)) {
				for (SootClass exception : new ArrayList<>(exceptions)) {
					if (exception.equals(trapCatchException) || JimpleUtils.getFastHierarchy().isSubclass(exception, trapCatchException)) {
						exceptions.remove(exception);
					}
				}
			}
		}
	}
}
