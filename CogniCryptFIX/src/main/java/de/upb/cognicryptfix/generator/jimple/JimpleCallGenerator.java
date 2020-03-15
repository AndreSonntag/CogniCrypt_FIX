package de.upb.cognicryptfix.generator.jimple;

import java.util.HashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import soot.Body;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeStmt;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleCallGenerator {

	/*-
	 * Cases:
	 * A x = new A()
	 * A x = A.getInstance();
	 * x.init() 
	 * K y = x.getKey();
	 */

	private JimpleLocalGenerator localGenerator;
	private JimpleInvokeGenerator invokeGenerator;
	private JimpleAssignGenerator assignGenerator;

	public JimpleCallGenerator(Body body) {
		this.localGenerator = new JimpleLocalGenerator(body);
		this.invokeGenerator = new JimpleInvokeGenerator();
		this.assignGenerator = new JimpleAssignGenerator();
	}

	public HashMap<Local, List<Unit>> generateCallUnits(Local var, SootMethod method, Local... parameterLocals) {
		return generateCallUnits(var, null, method, parameterLocals);
	}

	public HashMap<Local, List<Unit>> generateCallUnits(Local var, Local returnVar, SootMethod method, Local... parameterLocals) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		if (var.getType() == method.getReturnType()) {
			if (method.isConstructor()) {
				generatedUnits.putAll(generateConstructorCallUnits(returnVar, method, parameterLocals));
			} else { // i.e. getInstance()
				generatedUnits.putAll(generateSelfInitializeMethodCallUnits(returnVar, method, parameterLocals));
			}
		} else if (returnVar == null) {
			generatedUnits.putAll(generateVoidMethodCallUnits(var, method, parameterLocals));
		} else {
			generatedUnits.putAll(generateMethodCallWithReturnUnits(var, returnVar, method, parameterLocals));
		}
		return generatedUnits;
	}

	public HashMap<Local, List<Unit>> generateVoidMethodCallUnits(Local var, SootMethod method,
			Local... parameterLocals) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();
		List<Unit> units = Lists.newArrayList();
		units.add(invokeGenerator.generateInvokeStmt(var, method, parameterLocals));
		generatedUnits.put(var, units);
		return generatedUnits;
	}

	public HashMap<Local, List<Unit>> generateMethodCallWithReturnUnits(Local var, Local returnVar, SootMethod method,
			Local... parameterLocals) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		InvokeStmt invoke = (InvokeStmt) invokeGenerator.generateInvokeStmt(var, method, parameterLocals);
		Unit assign = assignGenerator.generateAssignStmt(returnVar, invoke.getInvokeExpr());

		List<Unit> units = Lists.newArrayList();
		units.add(assign);
		generatedUnits.put(returnVar, units);

		return generatedUnits;
	}

	// REFATOR check necessary!!!!
	public HashMap<Local, List<Unit>> generateSelfInitializeMethodCallUnits(Local var, SootMethod method, Local... parameterLocals) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		InvokeStmt invoke = (InvokeStmt) invokeGenerator.generateInvokeStmt(var, method, parameterLocals);
		Unit assign = assignGenerator.generateAssignStmt(var, invoke.getInvokeExpr());

		List<Unit> units = Lists.newArrayList();
		units.add(assign);
		generatedUnits.put(var, units);

		return generatedUnits;
	}

	public HashMap<Local, List<Unit>> generateConstructorCallUnits(Local var, SootMethod method,
			Local... parameterLocals) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		if (method.isConstructor()) {
			Local temp = localGenerator.generateFreshLocal(var.getType());
			Unit typeAssign = assignGenerator.generateVariableTypeAssignStmt(temp);
			Unit constructorInvoke = invokeGenerator.generateInvokeStmt(temp, method, parameterLocals);
			Unit assign = assignGenerator.generateAssignStmt(var, temp);

			List<Unit> units = Lists.newArrayList();
			units.add(typeAssign);
			units.add(constructorInvoke);
			units.add(assign);
			generatedUnits.put(var, units);
		} else {
			throw new RuntimeException("Method is not a constructor: " + method.getSignature());
		}
		return generatedUnits;
	}
}
