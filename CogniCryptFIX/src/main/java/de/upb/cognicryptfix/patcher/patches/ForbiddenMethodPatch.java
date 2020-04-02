package de.upb.cognicryptfix.patcher.patches;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.analysis.errors.ForbiddenMethodError;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.NoEnsuredPredicateException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.InitializationMethodSorter;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.internal.JimpleLocalBox;

/**
 * 
 * @author Andre Sonntag
 * @date 18.03.2020
 *
 */
public class ForbiddenMethodPatch extends AbstractPatch {

	private ForbiddenMethodError error;
	
	private CrySLEntity entity;
	private Body body;
	private JimpleCodeGeneratorByRule generator;
	private Unit patch;

	/**
	 * Creates a new {@link ForbiddenMethodPatch} object that represents
	 * the patch for the {@link ForbiddenMethodError} argument. 
	 * 
	 * @param error the error
	 */
	public ForbiddenMethodPatch(ForbiddenMethodError error) {
		this.error = error;
		this.entity = CrySLEntityPool.getInstance().getEntityByClassName(error.getRule().getClassName());
		this.body = error.getErrorLocation().getMethod().getActiveBody();
		this.generator = new JimpleCodeGeneratorByRule(error.getErrorLocation().getMethod().getActiveBody());
		this.patch = null;
	}

	@Override
	public Body applyPatch() {

		Unit forbiddenUnit = error.getErrorLocation().getUnit().get();
		SootMethod forbiddenMethod = error.getCalledMethod();
		ArrayList<SootMethod> alternativeMethods = new ArrayList<>(error.getAlternatives());

		if (Utils.isNullOrEmpty(alternativeMethods)) {
			body.getUnits().remove(forbiddenUnit);
		} else {
			Collections.sort(alternativeMethods, new InitializationMethodSorter());
			replaceForbiddenMethod(forbiddenUnit, forbiddenMethod, alternativeMethods.get(0));
		}
		generator.removeUnnecessaryTryCatchBlock();
		return body;
	}

	private void replaceForbiddenMethod(Unit forbiddenUnit, SootMethod forbiddenMethod, SootMethod alternativeMethod) {

		Map<Local, List<Unit>> generatedCallUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedParameterUnits = Maps.newLinkedHashMap();
		CrySLMethodCall call = entity.getFSM().getCrySLMethodCallBySootMethod(alternativeMethod);

		generatedParameterUnits = generator.generateParameters(call);
		Local invokeLocal = JimpleUtils.getInvokeLocal(forbiddenUnit);
		Local[] parameterLocals = generatedParameterUnits.keySet().toArray(new Local[0]);
		generatedCallUnits = generator.generateCall(invokeLocal, call, true, parameterLocals);

		List<Unit> parameterUnitList = Lists.newArrayList();
		for (List<Unit> l : generatedParameterUnits.values()) {
			parameterUnitList.addAll(l);
		}

		List<Unit> callUnitList = Lists.newArrayList();
		for (List<Unit> l : generatedCallUnits.values()) {
			callUnitList.addAll(l);
		}
		
		insertParameterUnits(forbiddenUnit, parameterUnitList);
		insertAlternativeCallUnits(forbiddenMethod, forbiddenUnit, alternativeMethod, callUnitList);
	}

	private void insertParameterUnits(Unit forbiddenUnit, List<Unit> parameterUnits) {
		if (!parameterUnits.isEmpty()) {
			body.getUnits().insertBefore(parameterUnits, forbiddenUnit);
		}
		generator.generateTryCatchBlock(parameterUnits);		
	}

	private void insertAlternativeCallUnits(SootMethod forbiddenMethod, Unit forbiddenUnit,
			SootMethod alternativeMethod, List<Unit> alternativeUnits) {
		InvokeExpr alternativeInvokeExpr = null;
		for (Unit u : alternativeUnits) {
			if (JimpleUtils.containsInvokeExpr(u)) {
				alternativeInvokeExpr = JimpleUtils.getInvokeExpr(u);
				break;
			}
		}

		// little workaroud because our code generator doesn't generate individual
		// InvokeExpr
		for (ValueBox box : alternativeInvokeExpr.getUseBoxes()) {
			if (box instanceof JimpleLocalBox) {
				box.setValue(JimpleUtils.getInvokeLocal(forbiddenUnit));
				break;
			}
		}

		if (alternativeMethod.isConstructor()) {
			if (forbiddenMethod.isConstructor()) {

				// replace forbidden InvokeExpr by alternative InvokeExpr
				if (forbiddenUnit instanceof AssignStmt) {
					AssignStmt assign = (AssignStmt) forbiddenUnit;
					assign.getRightOpBox().setValue((Value) alternativeInvokeExpr);
				} else {
					InvokeStmt invoke = (InvokeStmt) forbiddenUnit;
					invoke.setInvokeExpr(alternativeInvokeExpr);
				}
			} else {
				body.getUnits().insertBefore(alternativeUnits, forbiddenUnit);
				body.getUnits().remove(forbiddenUnit);
			}
		} else {
			if (forbiddenUnit instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) forbiddenUnit;
				assign.getRightOpBox().setValue((Value) alternativeInvokeExpr);

			} else {
				InvokeStmt invoke = (InvokeStmt) forbiddenUnit;
				invoke.setInvokeExpr(alternativeInvokeExpr);
			}
		}
		generator.generateTryCatchBlock(forbiddenUnit);		
		patch = forbiddenUnit;
	}

	@Override
	public String toPatchString() {
		StringBuilder builder = new StringBuilder();
		builder.append("\n------------------->--------------------ForbiddenMethodPatch---------------->------------------\n");
		builder.append("Class: \t\t"+error.getErrorLocation().getMethod().getDeclaringClass().toString()+"\n");
		builder.append("Method: \t"+error.getErrorLocation().getMethod().getSignature()+"\n");
		builder.append("Error: \t\t"+error.getClass().getSimpleName()+"\n");
		builder.append("CrySLRule: \t"+entity.getRule().getClassName()+"\n");
		builder.append("Message: \t"+error.toErrorMarkerString()+"\n");
		builder.append("Patch: \t\t");
		if(patch == null) {
			builder.append("Forbidden call removed");
		} else {
			builder.append(patch);
		}
		builder.append("\n");
		builder.append("-----------------------------------------------------------------------------------------------\n");
		return builder.toString();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ForbiddenMethodPatch [error=");
		builder.append(error.toErrorMarkerString());
		builder.append(", entity=");
		builder.append(entity.getRule().getClassName());
		builder.append("]");
		return builder.toString();
	}
}
