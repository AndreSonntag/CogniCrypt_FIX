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
import de.upb.cognicryptfix.generator.JimpleCodeGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
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

/*
 * Testing classes: Cipher, KeyGenerator, PBEKeySpec 
 * Problems: CrySL forbidden block 
 * TODO: generate return variable of alternative method call
 */

public class ForbiddenMethodPatch extends AbstractPatch {
	private static final Logger logger = LogManager.getLogger(ForbiddenMethodPatch.class.getSimpleName());
	private ForbiddenMethodError error;
	private CrySLRule violatedCrySLRule;
	private String brokenJimpleCode;
	private String patchValue;
	private JimpleCodeGenerator codeGenerator;
	
	public ForbiddenMethodPatch(ForbiddenMethodError error) {
		this.error = error;
		this.codeGenerator = JimpleCodeGenerator.getInstance(error.getErrorLocation().getMethod().getActiveBody());
		setErrorInformation();
	}

	@Override
	public Body getPatch() {

		Body methodBody = error.getErrorLocation().getMethod().getActiveBody();
		Unit forbiddenUnit = error.getErrorLocation().getUnit().get();
		SootMethod forbiddenMethod = error.getCalledMethod();
		ArrayList<SootMethod> alternativeMethods = new ArrayList<>(error.getAlternatives());

		if (Utils.isNullOrEmpty(alternativeMethods)) {
			methodBody.getUnits().remove(forbiddenUnit);
		} else {
			replaceForbiddenMethod(methodBody, forbiddenMethod, forbiddenUnit, alternativeMethods.get(0));
		}
		return methodBody;
	}

	private void replaceForbiddenMethod(Body body, SootMethod forbiddenMethod, Unit forbiddenUnit,
			SootMethod alternativeMethod) {

		HashMap<Local, List<Unit>> generatedParameterUnits = generateParameterUnits(body, forbiddenMethod, forbiddenUnit, alternativeMethod);
		
		Local[] parameterLocals = generatedParameterUnits.keySet().toArray(new Local[0]);
		generateAndInsertAlternativeInvokeExpr(body, forbiddenMethod, forbiddenUnit, alternativeMethod, parameterLocals);
//		alternatives.get(0).addException(Scene.v().getSootClass("java.io.IOException"));	//Test purpose
		generateTraps(Arrays.asList(forbiddenUnit));

		for (List<Unit> unitList : generatedParameterUnits.values()) {
			generateTraps(unitList);
		}
	}

	private void generateAndInsertAlternativeInvokeExpr(Body body, SootMethod forbiddenMethod, Unit forbiddenUnit,
			SootMethod alternativeMethod, Local[] alternativeParameterLocals) {

		Local invokeLocal = JimpleUtils.getInvokeLocal(forbiddenUnit);
		
		// generate the alternative InvokeExpr for alternative Method
		HashMap<Local, List<Unit>> generatedUnits = codeGenerator.generateCall(invokeLocal, alternativeMethod, alternativeParameterLocals);

		Unit alternativeInvokeUnit = null;
		InvokeExpr alternativeInvokeExpr = null;
		for(List<Unit> l : generatedUnits.values()) {
			alternativeInvokeUnit = l.get(0);
		}
		
		if(alternativeMethod.isConstructor()) {
			if(forbiddenMethod.isConstructor()) {
				alternativeInvokeExpr = ((InvokeStmt) alternativeInvokeUnit).getInvokeExpr();
				
				// replace forbidden InvokeExpr by alternative InvokeExpr
				if(forbiddenUnit instanceof AssignStmt) {
					AssignStmt assign = (AssignStmt) forbiddenUnit;
					assign.getRightOpBox().setValue((Value) alternativeInvokeExpr);
				} else {
					InvokeStmt invoke = (InvokeStmt) forbiddenUnit;
					invoke.setInvokeExpr(alternativeInvokeExpr);
				}
			}
			else {
				body.getUnits().insertBefore(generatedUnits.get(0), forbiddenUnit);
				body.getUnits().remove(forbiddenUnit);
			}
		}
		else {
			alternativeInvokeExpr = ((InvokeStmt) alternativeInvokeUnit).getInvokeExpr();
			
			if(forbiddenUnit instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) forbiddenUnit;
				assign.getRightOpBox().setValue((Value) alternativeInvokeExpr);

			} else {
				InvokeStmt invoke = (InvokeStmt) forbiddenUnit;
				invoke.setInvokeExpr(alternativeInvokeExpr);
			}
		}
		patchValue = alternativeInvokeExpr.toString();
	}

	private HashMap<Local, List<Unit>> generateParameterUnits(Body body, SootMethod forbiddenMethod, Unit forbiddenUnit, SootMethod alternativeMethod) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();
		
		// extract and use CrySL rule variable names for new generated parameters 
		List<Entry<String, String>> parameterNames = extractParameterVarNameFromRule(forbiddenMethod, alternativeMethod);
		List<String> parameterNameList = Lists.newArrayList();
		for (Entry<String, String> names : parameterNames) {
			parameterNameList.add(names.getKey());
		}
		
		// generate parameter units
		generatedUnits = codeGenerator.generateParametersForCall(alternativeMethod, parameterNameList, null);

		List<Unit> parameterUnits = Lists.newArrayList();
		for (List<Unit> l : generatedUnits.values()) {
			parameterUnits.addAll(l);
		}

		if(!parameterUnits.isEmpty()) {
			body.getUnits().insertBefore(parameterUnits, forbiddenUnit);
		}
		return generatedUnits;
	}

	private void generateTraps(List<Unit> units) {

		for (Unit u : units) {
			if (u instanceof AssignStmt) {
				AssignStmt assign = (AssignStmt) u;
				Value rightBox = assign.getRightOpBox().getValue();
				if (rightBox instanceof InvokeExpr) {
					InvokeExpr expr = (InvokeExpr) rightBox;
					codeGenerator.generateTryCatch(expr.getMethod(), units);
				}
			} else if (u instanceof InvokeStmt) {
				InvokeStmt invoke = (InvokeStmt) u;
				InvokeExpr expr = invoke.getInvokeExpr();
				codeGenerator.generateTryCatch(expr.getMethod(), units);
			}
		}

	}

	private List<Entry<String, String>> extractParameterVarNameFromRule(SootMethod forbiddenMethod,
			SootMethod alternativeMethod) {

		
		List<CrySLForbiddenMethod> forbiddenCrySLMethods = violatedCrySLRule.getForbiddenMethods();
		for (CrySLForbiddenMethod forbiddenCrySLMethod : forbiddenCrySLMethods) {
			CrySLMethod innerCrySLMethod = forbiddenCrySLMethod.getMethod();

			if (isMethodNameMatch(innerCrySLMethod, forbiddenMethod)) {

				List<Entry<String, String>> innerCrySLMethodParamaters = innerCrySLMethod.getParameters();
				List<String> crySLMethodParameterTypes = Lists.newArrayList();
				for (Entry<String, String> e : innerCrySLMethodParamaters) {
					crySLMethodParameterTypes.add(e.getValue());
				}

				List<Type> forbiddenSootMethodParameters = forbiddenMethod.getParameterTypes();
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
