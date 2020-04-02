package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.NoEnsuredPredicateException;
import de.upb.cognicryptfix.exception.NoImplementerException;
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

	public JimpleParameterGeneratorByRule(Body body, CrySLEntityPool pool,
			JimplePredicateGenerator predicateGenerator) {
		this.pool = pool;
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
		this.arrayGenerator = new JimpleArrayGenerator(body);
		this.callGenerator = new JimpleCallGenerator(body);
		this.parameterGenerator = new JimpleParameterGenerator(body);
		this.predicateGenerator = predicateGenerator;
	}

	public Map<Local, List<Unit>> generateParameterUnits(CrySLMethodCall call) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();

		if (call.getCallParameters().size() == 0) {
			return generatedUnits;
		}

		CrySLEntity entity = pool.getEntityByClassName(call.getRule().getClassName());
		for (CrySLVariable parameter : call.getCallParameters()) {
			if (entity.requiresPredicate(parameter)) {
				List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(parameter);
				try {
					generatedUnits.putAll(predicateGenerator.generatePredicateUnits(requiredPredicates));
				} catch (NoEnsuredPredicateException e) {
					System.err.println("No Rule provides following predicate: "+requiredPredicates.toString());
					e.printStackTrace();
				}
			} else {
				generatedUnits.putAll(generateParameterUnit(parameter));
			}
		}
		return generatedUnits;
	}

	private Map<Local, List<Unit>> generateParameterUnit(CrySLVariable variable) {
		Map<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		String variableName = variable.getName();
		Type variableType = variable.getType();
		Value variableValue = variable.getValue();

		if (variableType instanceof PrimType || variableType == Scene.v().getType("java.lang.String")) {
			Type primType = variableType;
			Local primeLocal = localGenerator.generateFreshLocal(primType, variableName);

			Value primeValue = null;
			if (variableValue != null) {
				primeValue = variableValue;
			} else {
				primeValue = JimpleUtils.generateDummyConstantValue(primType);
			}
			Unit primeAssignStmt = assignGenerator.generateAssignStmt(primeLocal, primeValue);

			List<Unit> primeUnits = Lists.newArrayList();
			primeUnits.add(primeAssignStmt);
			generatedUnits.put(primeLocal, primeUnits);

		} else if (variableType instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) variableType;

			if (variableValue != null) {
				generatedUnits
						.putAll(arrayGenerator.generateArrayUnits(arrayType, Lists.newArrayList(variable.getValue())));
			} else {
				Local arrayLocal = localGenerator.genereateFreshArrayLocal(arrayType, variableName, 1);
				Unit arrayAssign = assignGenerator.generateArrayAssignStmt(arrayLocal, 16);
				List<Unit> arrayUnits = Lists.newArrayList();
				arrayUnits.add(arrayAssign);
				generatedUnits.put(arrayLocal, arrayUnits);
			}
		} else if (variableType instanceof RefType) {
			List<CrySLPredicate> predicates = pool.getPossiblePredicateCandidatesForType(variableType);

			if (!predicates.isEmpty()) {
				try {
					generatedUnits.putAll(predicateGenerator.generatePredicateUnits(predicates));
				} catch (NoEnsuredPredicateException e) {
					System.err.println("No Rule provides following predicate: "+predicates.toString());
					e.printStackTrace();
				}
				return generatedUnits;
			} else {
				RefType refType = (RefType) variableType;
				SootClass refTypeClazz = refType.getSootClass();
				Entry<SootClass, SootMethod> init = null;
				SootClass initClazz = null;
				SootMethod initMethod = null;

				try {
					init = JimpleUtils.getImplementingClassAndInitMethod(refTypeClazz);
					initClazz = init.getKey();
					initMethod = init.getValue();
				} catch (NoImplementerException e) {
					e.printStackTrace();
					initClazz = Scene.v().getSootClass("java.lang.Object");
					initMethod = initClazz.getMethodByName("");
				}

				Local initLocal = localGenerator.generateFreshLocal(initClazz.getType(), variableName);
				generatedUnits.put(initLocal, Lists.newArrayList());

				Map<Local, List<Unit>> generatedParameterUnits = parameterGenerator.generateParameterUnits(initMethod,
						null, null);
				for (List<Unit> l : generatedParameterUnits.values()) {
					generatedUnits.get(initLocal).addAll(l);
				}

				Local[] parameterLocals = generatedParameterUnits.keySet().toArray(new Local[0]);
				Map<Local, List<Unit>> generatedCallUnits = callGenerator.generateCallUnits(initLocal, initMethod,
						parameterLocals);
				for (List<Unit> l : generatedCallUnits.values()) {
					generatedUnits.get(initLocal).addAll(l);
				}
			}
		}
		return generatedUnits;
	}

}
