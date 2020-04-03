package de.upb.cognicryptfix.generator.jimple;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.utils.RequiredExceptionHandlingTag;
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
import soot.jimple.GotoStmt;
import soot.jimple.InvokeExpr;
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
	
	
	public void removeTrapsWithOnlyGotoStatement() {
		Chain<Trap> traps = body.getTraps();

		Map<Unit, List<Trap>> multipleTrapsMap = Maps.newHashMap();
	
		for(Trap t: traps) {
			if(t.getBeginUnit() instanceof GotoStmt && t.getBeginUnit() == t.getEndUnit()) {
				if(multipleTrapsMap.get(t.getBeginUnit()) != null){
					multipleTrapsMap.get(t.getBeginUnit()).add(t);
				} else {
					multipleTrapsMap.put(t.getBeginUnit(), Lists.newArrayList(t));
				}
			}
		}
		
		for(Unit u : multipleTrapsMap.keySet()) {
			List<Trap> multipleTrap = multipleTrapsMap.get(u);
			Trap t = multipleTrap.get(0);
			GotoStmt stmt = (GotoStmt) t.getBeginUnit();
			Unit gotoTarget = stmt.getTarget();
			Iterator<Unit> it = body.getUnits().iterator(stmt, body.getUnits().getPredOf(gotoTarget));
			List<Unit> unitsToDelete = Lists.newArrayList();
			
			while(it.hasNext()) {
				unitsToDelete.add(it.next());
			}
			body.getUnits().removeAll(unitsToDelete);
			body.getTraps().removeAll(multipleTrap);
		}	
	}
		
	public void generateTrap(Unit unit) {
		if (JimpleUtils.containsInvokeExpr(unit)) {
			InvokeExpr inokeExpr = JimpleUtils.getInvokeExpr(unit);
			generateTraps(inokeExpr.getMethod(), Lists.newArrayList(unit));
		}
	}

	public void generateTrap(List<Unit> units) {
		for (Unit u : units) {
			if (JimpleUtils.containsInvokeExpr(u) && u.hasTag(Constants.requiredExceptionHandlingTag)) {
				InvokeExpr inokeExpr = JimpleUtils.getInvokeExpr(u);
				RequiredExceptionHandlingTag reqExceptionTag = (RequiredExceptionHandlingTag) u.getTag(Constants.requiredExceptionHandlingTag);
				List<Unit> tryUnits = reqExceptionTag.getUnits();
				generateTraps(inokeExpr.getMethod(), tryUnits);
			} 
		}
	}

	public void generateTraps(SootMethod method, List<Unit> tryUnits) {
		List<SootClass> exceptions = method.getExceptions();

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
		for(Unit u : tryUnits) {
			if (u instanceof AssignStmt && JimpleUtils.containsInvokeExpr(u)) {
				AssignStmt assign = (AssignStmt) u;
				Value variable = assign.getLeftOpBox().getValue();
				nullAssign = assignGenerator.generateAssignStmt(variable, JimpleUtils.generateConstantValue(null));
				body.getUnits().insertBefore(nullAssign, body.getUnits().getPredOf(startTryBlockUnit));
			}
		}
	

		Unit endTryBlockUnitSucc = Jimple.v().newGotoStmt(body.getUnits().getSuccOf(endTryBlockUnit)); // return
		Unit gotoDestinationUnit = null;
		for (int i = 0; i < exceptions.size(); i++) {

			SootClass thrwoableClass = Scene.v().getSootClass("java.lang.Throwable");
			RefType thrwoableRefType = RefType.v(thrwoableClass.getName());
			Local thrwoableTempLocal = localGenerator.generateFreshLocal(thrwoableRefType);
			Unit caughtStmt = Jimple.v().newIdentityStmt(thrwoableTempLocal, Jimple.v().newCaughtExceptionRef());

			Local thrwoableELocal = localGenerator.generateFreshLocal(thrwoableRefType);
			Unit localAssign = assignGenerator.generateAssignStmt(thrwoableELocal, thrwoableTempLocal);

			RefType exceptionRefType = RefType.v(exceptions.get(i).getName());
			Local exceptionTempLocal = localGenerator.generateFreshLocal(exceptionRefType);
			Value castExpr = Jimple.v().newCastExpr(thrwoableELocal, exceptionRefType);

			Unit castAssign = assignGenerator.generateAssignStmt(exceptionTempLocal, castExpr);
			SootMethod mPrintStackTrace = thrwoableClass.getMethod("void printStackTrace()");
			Unit printStackTraceInvokeStmt = invokeGenerator.generateInvokeStmt(exceptionTempLocal, mPrintStackTrace);

			List<Unit> catchBlock = new ArrayList<Unit>();
			if (!body.getUnits().contains(endTryBlockUnitSucc)) {
				catchBlock.add(endTryBlockUnitSucc);

			}
			catchBlock.add(caughtStmt);
			catchBlock.add(localAssign);
			catchBlock.add(castAssign);
			catchBlock.add(printStackTraceInvokeStmt);
			if (exceptions.size() > 1 && i == 0) {
				catchBlock.add(Jimple.v().newGotoStmt(body.getUnits().getSuccOf(endTryBlockUnit)));
			}

			// multiple catch blockes
			if (!body.getUnits().contains(endTryBlockUnitSucc)) {
				gotoDestinationUnit = body.getUnits().getSuccOf(endTryBlockUnit);
				insertRightBeforeNoRedirect(body, catchBlock, body.getUnits().getSuccOf(endTryBlockUnit));
			} else {
				insertRightBeforeNoRedirect(body, catchBlock, gotoDestinationUnit);
			}

			Trap t = Jimple.v().newTrap(exceptions.get(i), startTryBlockUnit, endTryBlockUnitSucc, caughtStmt);
			body.getTraps().add(t);
		}
	}

	private static void insertRightBeforeNoRedirect(Body body, List<Unit> catchBlock, Unit s) {
		for (Unit stmt : catchBlock) {
			body.getUnits().insertBeforeNoRedirect(stmt, s);
		}
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
		List<Unit> bodyUnits = Lists.newArrayList(body.getUnits());

		for (Trap trap : traps) {
			List<Unit> trapTryUnits = bodyUnits.subList(bodyUnits.indexOf(trap.getBeginUnit()),
					bodyUnits.indexOf(trap.getEndUnit()));
			SootClass trapCatchException = trap.getException();
			if (trapTryUnits.containsAll(tryUnits)) {
				for (SootClass exception : new ArrayList<>(exceptions)) {
					if (exception.equals(trapCatchException)
							|| JimpleUtils.getFastHierarchy().isSubclass(exception, trapCatchException)) {
						exceptions.remove(exception);
					}
				}
			}
		}
	}
}
