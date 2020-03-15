package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLEntityPool;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.generator.jimple.JimpleArrayGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleAssignGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleParameterGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.PrimType;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;

/**
 * @author Andre Sonntag
 * @date 09.03.2020
 */
public class JimpleParameterGeneratorByRule {

	private CrySLEntityPool pool;
	private JimpleLocalGenerator localGenerator;
	private JimpleAssignGenerator assignGenerator;
	private JimpleCallGenerator callGenerator;
	private JimplePredicateGenerator predicateGenerator;
	private JimpleArrayGenerator arrayGenerator;
	private JimpleParameterGenerator parameterGenerator;

	public JimpleParameterGeneratorByRule(Body body, CrySLEntityPool pool, JimplePredicateGenerator predicateGenerator) {
		this.pool = pool;
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
		this.arrayGenerator = new JimpleArrayGenerator(body);
		this.callGenerator = new JimpleCallGenerator(body);
		this.parameterGenerator = new JimpleParameterGenerator(body);
		this.predicateGenerator = predicateGenerator;
	}

	public HashMap<Local, List<Unit>> generateParameterUnits(CrySLMethodCall call) {
		LinkedHashMap<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		
		if(call.getCallParameters().size() == 0) {
			return generatedUnits;
		}
		
		CrySLEntity entity = pool.getEntityByClassName(call.getRule().getClassName());
		for (CrySLVariable parameter : call.getCallParameters()) {
			if(entity.requiresPredicate(parameter)) {
				generatedUnits.putAll(predicateGenerator.generatePredicateUnits(entity.getRequiredPredicateForVariable(parameter)));
			}else {
				generatedUnits.putAll(generateParameterUnit(parameter));
			}
		}
	return generatedUnits;
	}

	private HashMap<Local, List<Unit>> generateParameterUnit(CrySLVariable variable) {
		HashMap<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		String variableName = variable.getVariable();
		Type variableType = variable.getType();
		Value variableValue = variable.getValue();
		
		if (variableType instanceof PrimType || variableType == Scene.v().getType("java.lang.String")) {
			Type primType = variableType;
			Local primeLocal = localGenerator.generateFreshLocal(primType, variableName);
			
			Value primeValue = null;
			if(variableValue != null) {
				 primeValue = variableValue;
			} else {
				primeValue = JimpleUtils.generateDummyValueForType(primType);
			}
			Unit primeAssignStmt = assignGenerator.generateAssignStmt(primeLocal, primeValue);
			
			List<Unit> primeUnits = Lists.newArrayList();
			primeUnits.add(primeAssignStmt);
			generatedUnits.put(primeLocal, primeUnits);
			return generatedUnits;

		} else if (variableType instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) variableType;
			
			if(variableValue != null) {
				generatedUnits.putAll(arrayGenerator.generateArrayUnits(arrayType.baseType, Lists.newArrayList(variable.getValue())));
			} else {
				Local arrayLocal = localGenerator.genereateFreshArrayLocal(arrayType.baseType, variableName, 1);
				Unit arrayAssign = assignGenerator.generateArrayAssignStmt(arrayLocal, 16);
				List<Unit> arrayUnits = Lists.newArrayList();
				arrayUnits.add(arrayAssign);
				generatedUnits.put(arrayLocal, arrayUnits);
			}
			return generatedUnits;
		} 
		else if (variableType instanceof RefType) {
			
			List<CrySLPredicate> predicates = pool.getEnsuredPredicatesByVariableType(variableType);
			if(!predicates.isEmpty()) {
				generatedUnits.putAll(predicateGenerator.generatePredicateUnits(predicates));
				return generatedUnits;
			}
			
			
			
			RefType refType = (RefType) variableType;
			Local refTypeLocal = localGenerator.generateFreshLocal(refType, variableName);
			SootClass clazz = refType.getSootClass();
			SootMethod initMethod = JimpleUtils.getBestInitializationMethod(clazz);
	
			
			
			//first check if any rule an key provided before find the beste init method
			
			
//			if (clazz.isAbstract()) {
//				clazz = JimpleUtils.getSubClass(clazz);
//				initMethod = JimpleUtils.getBestInitializationMethod(clazz);
//			} else if (clazz.isInterface()) {
//				clazz = null;
//				initMethod = JimpleUtils.getBestInitializationMethod(clazz);
//			} else {
//				initMethod = JimpleUtils.getBestInitializationMethod(clazz);
//			}

			HashMap<Local, List<Unit>> generatedRefTypeParameterUnits = Maps.newHashMap();
			generatedRefTypeParameterUnits.putAll(parameterGenerator.generateParameterUnits(initMethod, null, null));
			
			Local[] RefTypeParameterLocals = generatedRefTypeParameterUnits.keySet().toArray(new Local[0]);
			callGenerator.generateCallUnits(refTypeLocal, initMethod, RefTypeParameterLocals);
		}
		return generatedUnits;
	}

}
