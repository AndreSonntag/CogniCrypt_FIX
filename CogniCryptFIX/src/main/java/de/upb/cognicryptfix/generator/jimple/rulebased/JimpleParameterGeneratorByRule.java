package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.generation.GenerationException;
import de.upb.cognicryptfix.exception.generation.NoInterfaceImplementerException;
import de.upb.cognicryptfix.exception.generation.crysl.CrySLGenerationException;
import de.upb.cognicryptfix.exception.generation.crysl.NoPredicateEnsurerException;
import de.upb.cognicryptfix.exception.path.PathException;
import de.upb.cognicryptfix.generator.jimple.JimpleArrayGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleAssignGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleCallGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleParameterGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
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
import soot.tagkit.InnerClassTag;
import soot.tagkit.Tag;

/**
 * @author Andre Sonntag
 * @date 09.03.2020
 */
public class JimpleParameterGeneratorByRule {

	private static final Logger LOGGER = LogManager.getLogger(JimpleParameterGeneratorByRule.class);
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

	public Map<Local, List<Unit>> generateParameterUnits(CrySLMethodCall call,
			Map<Integer, Local>... alreadyGeneratedParameter) throws GenerationException, PathException {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		boolean parameterAvailable = true;

		if (call.getCallParameters().size() == 0) {
			return generatedUnits;
		}

		if (alreadyGeneratedParameter.length == 0) {
			parameterAvailable = false;
		}

		LOGGER.debug("Call parameters to generate: " + call.getCallParameters());
		CrySLEntity entity = pool.getEntityByClassName(call.getRule().getClassName());

		for (int i = 0; i < call.getCallParameters().size(); i++) {
			CrySLVariable parameter = call.getCallParameters().get(i);

			if (!parameterAvailable) {
				if (entity.requiresPredicate(parameter)) {
					LOGGER.debug(parameter.toString() + " requires a predicate");
					List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(parameter);
					if (requiredPredicates.isEmpty()) {
						throw new NoPredicateEnsurerException(
								"No Rule provides following predicate: " + requiredPredicates.toString());
					}
					generatedUnits.putAll(predicateGenerator.generatePredicateUnits(requiredPredicates));
				} else {
					LOGGER.debug(parameter.toString() + " doesn't require a predicate");
					generatedUnits.putAll(generateParameterUnit(parameter));
				}
			} else {
				if (alreadyGeneratedParameter[0].containsKey(i)) {
					generatedUnits.put(alreadyGeneratedParameter[0].get(i), Lists.newArrayList());
				} else {
					if (entity.requiresPredicate(parameter)) {
						List<CrySLPredicate> requiredPredicates = entity
								.getRequiredPredicateForVariableByType(parameter);
						if (requiredPredicates.isEmpty()) {
							throw new NoPredicateEnsurerException(
									"No Rule provides following predicate: " + requiredPredicates.toString());
						}
						generatedUnits.putAll(predicateGenerator.generatePredicateUnits(requiredPredicates));
					} else {
						generatedUnits.putAll(generateParameterUnit(parameter));
					}
				}
			}
		}
		return generatedUnits;
	}

	private Map<Local, List<Unit>> generateParameterUnit(CrySLVariable variable)
			throws PathException, GenerationException {
		LOGGER.debug("Generate parameter: " + variable);

		Map<Local, List<Unit>> generatedUnits = Maps.newHashMap();

		String variableName = variable.getName();
		Type variableType = variable.getType();
		Value variableValue = variable.getValue();

		if (variableType instanceof PrimType
				|| JimpleUtils.equals(variableType, Scene.v().getType("java.lang.String"))) {
			Type primType = variableType;
			Local primeLocal = localGenerator.generateFreshLocal(primType, "varReplacer");

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

			List<CrySLPredicate> predicates = pool.getPredicateCandidatesWhichProduceType(variableType);
			if (!predicates.isEmpty()) {
				generatedUnits.putAll(predicateGenerator.generatePredicateUnits(predicates));
				return generatedUnits;

			} else {
				RefType refType = (RefType) variableType;
				SootClass refTypeClazz = refType.getSootClass();
				Entry<SootClass, SootMethod> init = null;
				SootClass initClazz = null;
				SootMethod initMethod = null;
				init = JimpleUtils.getImplementingClassAndInitMethod(refTypeClazz);
				if (init.getValue() == null) {
					for (Tag t : refTypeClazz.getTags()) {
						if (t instanceof InnerClassTag) {
							InnerClassTag innerTag = (InnerClassTag) t;
							String innerClassName = innerTag.getShortName();
							String fullName = refTypeClazz.getName() + "$" + innerClassName;
							SootClass innerClass = Scene.v().getSootClass(fullName);
							init = JimpleUtils.getImplementingClassAndInitMethod(innerClass);
							break;
						}
					}
				}

				if (init != null) {
					initClazz = init.getKey();
					initMethod = init.getValue();
				} else {
					initClazz = Scene.v().getSootClass("java.lang.Object");
					initMethod = initClazz.getMethodByName("");
				}

				if (variableName.equals("")) {
					variableName = Utils.getAppropriateVarName(initClazz.getName());
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
