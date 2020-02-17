package de.upb.cognicryptfix.patcher.patches;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.analysis.errors.ForbiddenMethodError;
import crypto.rules.CrySLForbiddenMethod;
import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.utils.JimpleCodeGenerator;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.internal.JimpleLocalBox;

public class ForbiddenMethodPatch extends AbstractPatch {
	private static final Logger logger = LogManager.getLogger(ForbiddenMethodPatch.class.getSimpleName());
	private ForbiddenMethodError error;
	private CrySLRule violatedCrySLRule;
	private String brokenJimpleCode;
	private String patchValue;

//	Aggregator.v().transform(body);

	public ForbiddenMethodPatch(ForbiddenMethodError error) {
		this.error = error;
		setErrorInformation();
	}

	@Override
	public Body getPatch() {
		Body methodBody = error.getErrorLocation().getMethod().getActiveBody();
		Unit forbiddenStmt = error.getErrorLocation().getUnit().get();
		SootMethod forbiddenMethod = error.getCalledMethod();
		ArrayList<SootMethod> alternatives = new ArrayList<>(error.getAlternatives());

		if (Utils.isNullOrEmpty(alternatives)) {
			methodBody.getUnits().remove(forbiddenStmt);
		} else {
			replaceForbiddenMethod(methodBody, forbiddenMethod, forbiddenStmt, alternatives);
		}
		return methodBody;
	}

	private void replaceForbiddenMethod(Body body, SootMethod forbiddenMethod, Unit forbiddenUnit,
			ArrayList<SootMethod> alternatives) {

		SootMethod alternativeMethod = alternatives.get(0);

		HashMap<Local, List<Unit>> generatedParameterUnits = generateParameterUnits(body, forbiddenMethod,
				forbiddenUnit, alternativeMethod);
		Value[] parameterLocals = generatedParameterUnits.keySet().toArray(new Value[0]);
		
		
		generateAlternativeInvokeExpr(body, forbiddenMethod, forbiddenUnit, alternativeMethod, parameterLocals);
//		alternatives.get(0).addException(Scene.v().getSootClass("java.io.IOException"));	//Test purpose
		generateTraps(body, Arrays.asList(forbiddenUnit));
		
		for(List<Unit> unitList : generatedParameterUnits.values()) {
			generateTraps(body, unitList);
		}
		System.out.println();
	}

	private void generateAlternativeInvokeExpr(Body body, SootMethod forbiddenMethod, Unit forbiddenUnit,
			SootMethod alternativeMethod, Value[] alternativeParameterLocals) {

		boolean isAssignStmt = false;
		InvokeExpr forbiddenInvokeExpr = null;

		// extract InvokeExpr
		if (forbiddenUnit instanceof AssignStmt) { // i.e. "cipherText = c.doFinal();"
			isAssignStmt = true;
			AssignStmt assign = (AssignStmt) forbiddenUnit;
			forbiddenInvokeExpr = (InvokeExpr) assign.getRightOpBox().getValue();

		} else if (forbiddenUnit instanceof InvokeStmt) { // i.e. "c.init()"
			InvokeStmt invoke = (InvokeStmt) forbiddenUnit;
			forbiddenInvokeExpr = invoke.getInvokeExpr();
		}

		// extract Local from InvokeExpr
		Local invokeVarLocal = (Local) forbiddenInvokeExpr.getUseBoxes().stream()
				.filter(useBox -> useBox instanceof JimpleLocalBox).map(ValueBox::getValue).findAny().orElse(null);

		// generate the alternative InvokeStmt
		Unit alternativeInvokeUnit = JimpleCodeGenerator.generateInvokeStmt(invokeVarLocal, alternativeMethod,
				alternativeParameterLocals);
		InvokeExpr alternativeInvokeExpr = ((InvokeStmt) alternativeInvokeUnit).getInvokeExpr();

		if (isAssignStmt) {
			AssignStmt assign = (AssignStmt) forbiddenUnit;
			assign.getRightOpBox().setValue((Value) alternativeInvokeExpr);

		} else {
			InvokeStmt invoke = (InvokeStmt) forbiddenUnit;
			invoke.setInvokeExpr(alternativeInvokeExpr);
		}
		
		patchValue = alternativeInvokeExpr.toString();
	}

	private HashMap<Local, List<Unit>> generateParameterUnits(Body body, SootMethod forbiddenMethod, Unit forbiddenUnit,
			SootMethod alternativeMethod) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();
		List<Entry<String, String>> parameterNames = extractParameterVarNameFromRule(forbiddenMethod,
				alternativeMethod);
		List<String> parameterNameList = Lists.newArrayList();
		for (Entry<String, String> names : parameterNames) {
			parameterNameList.add(names.getKey());
		}
		generatedUnits = JimpleCodeGenerator.generateParameterUnits(body, alternativeMethod, parameterNameList, null);

		List<Unit> parameterUnits = Lists.newArrayList();
		for (List<Unit> l : generatedUnits.values()) {
			parameterUnits.addAll(l);
		}
		
		body.getUnits().insertBefore(parameterUnits, forbiddenUnit);
		return generatedUnits;
	}

	private void generateTraps(Body body, List<Unit> units) {

		for (Unit u : units) {

			if (u instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) u;
				Value rightBox = assign.getRightOpBox().getValue();
				if (rightBox instanceof InvokeExpr) {
					InvokeExpr expr = (InvokeExpr) rightBox;
					JimpleCodeGenerator.generateTraps(body, expr.getMethod(), units);
				}
			} else if (u instanceof InvokeStmt) {
				InvokeStmt invoke = (InvokeStmt) u;
				InvokeExpr expr = invoke.getInvokeExpr();
				JimpleCodeGenerator.generateTraps(body, expr.getMethod(), units);
			}
		}

	}

	private List<Entry<String, String>> extractParameterVarNameFromRule(SootMethod forbiddenSootMethod,
			SootMethod alternativeSootMethod) {

		List<CrySLForbiddenMethod> forbiddenCrySLMethods = violatedCrySLRule.getForbiddenMethods();
		for (CrySLForbiddenMethod forbiddenCrySLMethod : forbiddenCrySLMethods) {
			CrySLMethod innerCrySLMethod = forbiddenCrySLMethod.getMethod();

			if (isMethodNameMatch(innerCrySLMethod, forbiddenSootMethod)) {

				List<Entry<String, String>> innerCrySLMethodParamaters = innerCrySLMethod.getParameters();
				List<String> crySLMethodParameterTypes = Lists.newArrayList();
				for (Entry<String, String> e : innerCrySLMethodParamaters) {
					crySLMethodParameterTypes.add(e.getValue());
				}

				List<Type> forbiddenSootMethodParameters = forbiddenSootMethod.getParameterTypes();
				List<String> forbiddenSootMethodParameterTypes = Lists.newArrayList();
				for (Type t : forbiddenSootMethodParameters) {
					forbiddenSootMethodParameterTypes.add(t.toString());
				}

				if (isParameterMatch(crySLMethodParameterTypes, forbiddenSootMethodParameterTypes)) {
					return forbiddenCrySLMethod.getAlternatives().get(0).getParameters();
				}
			}

		}
		return null;
	}

	private boolean isMethodNameMatch(CrySLMethod crySLMethod, SootMethod sootMethod) {
		if (isCrySLMethodConstructor(crySLMethod) && sootMethod.isConstructor()) {
			if (StringUtils.substringBeforeLast(crySLMethod.getMethodName(), ".")
					.equals(sootMethod.getDeclaringClass().toString())) {
				return true;
			} else {
				return false;
			}
		} else if (crySLMethod.getMethodName()
				.equals(sootMethod.getDeclaringClass().toString() + "." + sootMethod.getName())) {
			return true;
		} else {
			return false;
		}
	}

	private boolean isParameterMatch(List<String> crySLMethodParameterTypes, List<String> sootMethodParameterTypes) {
		return crySLMethodParameterTypes.equals(sootMethodParameterTypes);
	}

	private boolean isCrySLMethodConstructor(CrySLMethod method) {
		String shortClassName = StringUtils.substringAfterLast(violatedCrySLRule.getClassName(), ".");
		return shortClassName.equals(method.getShortMethodName());
	}

	@Override
	public String toString() {
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("\nForbiddenMethodPatch [\n");
		strBuilder.append("error = " + error.toString() + "\n");
		strBuilder.append("violatedCrySLRule = " + violatedCrySLRule.getClassName() + "\n");
		strBuilder.append("brokenJimpleCode = " + brokenJimpleCode + "\n");
		strBuilder.append("brokenJimpleCode replaced by = " + patchValue + "\n");
		return strBuilder.toString();
	}

	private void setErrorInformation() {
		this.violatedCrySLRule = error.getRule();
		this.brokenJimpleCode = error.getErrorLocation().getUnit().get().getInvokeExpr().toString();
	}
}
