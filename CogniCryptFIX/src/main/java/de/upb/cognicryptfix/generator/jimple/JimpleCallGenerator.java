package de.upb.cognicryptfix.generator.jimple;

import java.util.List;
import java.util.Map;

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

	private Body body;
	private JimpleLocalGenerator localGenerator;
	private JimpleInvokeGenerator invokeGenerator;
	private JimpleAssignGenerator assignGenerator;

	public JimpleCallGenerator(Body body) {
		this.body = body;
		this.localGenerator = new JimpleLocalGenerator(body);
		this.invokeGenerator = new JimpleInvokeGenerator();
		this.assignGenerator = new JimpleAssignGenerator();
	}

	public Map<Local, List<Unit>> generateCallUnits(Local var, SootMethod method, Local... parameterLocals) {
		return generateCallUnits(var, null, method, parameterLocals);
	}

	public Map<Local, List<Unit>> generateCallUnits(Local var, Local returnVar, SootMethod method, Local... parameterLocals) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();

		if (method.isConstructor()) { //init call
			generatedUnits.putAll(generateConstructorCallUnits(var, method, parameterLocals));
		} else if (var.getType() == method.getReturnType()) {// i.e. getInstance()
			generatedUnits.putAll(generateSelfInitializeMethodCallUnits(var, method, parameterLocals));
		} else if (returnVar == null) {
			generatedUnits.putAll(generateVoidMethodCallUnits(var, method, parameterLocals));
		} else {
			generatedUnits.putAll(generateMethodCallWithReturnUnits(var, returnVar, method, parameterLocals));
		}
		return generatedUnits;
	}

	public Map<Local, List<Unit>> generateVoidMethodCallUnits(Local var, SootMethod method,
			Local... parameterLocals) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		List<Unit> units = Lists.newArrayList();
		units.add(invokeGenerator.generateInvokeStmt(var, method, parameterLocals));
		generatedUnits.put(var, units);
		return generatedUnits;
	}

	public Map<Local, List<Unit>> generateMethodCallWithReturnUnits(Local var, Local returnVar, SootMethod method,
			Local... parameterLocals) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();

		InvokeStmt invoke = (InvokeStmt) invokeGenerator.generateInvokeStmt(var, method, parameterLocals);
		Unit assign = assignGenerator.generateAssignStmt(returnVar, invoke.getInvokeExpr());

		List<Unit> units = Lists.newArrayList();
		units.add(assign);
		generatedUnits.put(returnVar, units);

		return generatedUnits;
	}

	// REFATOR check necessary!!!!
	public Map<Local, List<Unit>> generateSelfInitializeMethodCallUnits(Local var, SootMethod method, Local... parameterLocals) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();

		InvokeStmt invoke = (InvokeStmt) invokeGenerator.generateInvokeStmt(var, method, parameterLocals);
		Unit assign = assignGenerator.generateAssignStmt(var, invoke.getInvokeExpr());

		List<Unit> units = Lists.newArrayList();
		units.add(assign);
		generatedUnits.put(var, units);

		return generatedUnits;
	}

	public Map<Local, List<Unit>> generateConstructorCallUnits(Local var, SootMethod method, Local... parameterLocals) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();

			Local temp = localGenerator.generateFreshLocal(var.getType());
			Unit typeAssign = assignGenerator.generateVariableTypeAssignStmt(temp);
			Unit constructorInvoke = invokeGenerator.generateInvokeStmt(temp, method, parameterLocals);
			Unit assign = assignGenerator.generateAssignStmt(var, temp);

			List<Unit> units = Lists.newArrayList();
			units.add(typeAssign);
			units.add(constructorInvoke);
			units.add(assign);
			generatedUnits.put(var, units);
	
		return generatedUnits;
	}
}
