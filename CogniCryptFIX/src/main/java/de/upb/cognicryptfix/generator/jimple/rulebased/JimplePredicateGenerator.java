package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLPredicateFilter;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.exception.NoEnsuredPredicateException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleAssignGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.Type;
import soot.Unit;

public class JimplePredicateGenerator {

	private static final Logger logger = LogManager.getLogger(JimplePredicateGenerator.class.getSimpleName());
	private JimpleCodeGeneratorByRule codeGenerator;
	private JimpleLocalGenerator localGenerator;
	private JimpleAssignGenerator assignGenerator;

	public JimplePredicateGenerator(Body body, JimpleCodeGeneratorByRule codeGenerator) {
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
		this.codeGenerator = codeGenerator;
	}
	
	public Map<Local, List<Unit>> generatePredicateUnits(CrySLEntity entity, CrySLVariable variable, Local usageLocal) {	
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		if (entity.requiresPredicate(variable)) {
			List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(variable);			
			try {
				generatedUnits.putAll(generatePredicateUnits(requiredPredicates, usageLocal));
			} catch (NoEnsuredPredicateException e) {
				System.err.println("No Rule provides following predicate: "+requiredPredicates.toString());
				e.printStackTrace();
			}			
		}	
		return generatedUnits;
	}
	
	public Map<Local, List<Unit>> generatePredicateUnits(List<CrySLPredicate> predicates, Local... usageLocal) throws NoEnsuredPredicateException{
		CrySLPredicate predicate = null;
		List<CrySLMethodCall> predicatePath = Lists.newLinkedList();
		Entry<CrySLPredicate, LinkedList<CrySLMethodCall>> predicateEntry = CrySLPredicateFilter.applyFilters(predicates);
		predicate = predicateEntry.getKey();
		predicatePath = predicateEntry.getValue();
		return generatePredicateUnits(predicate, predicatePath, usageLocal);
	}

	public Map<Local, List<Unit>> generatePredicateUnits(CrySLPredicate predicate, List<CrySLMethodCall> predicatePath,
			Local... usageLocal) throws NoEnsuredPredicateException {
		Map<Local, List<Unit>> generatedUnitsForPredicate = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Local initLocal = null;
		Local predicateLocal = null;
		CrySLVariable firstPredicateParamter = predicate.getPredicateParameters().get(0);
		

		if (predicate.getProducer().isInterfaceOrAbstract()) {
			List<CrySLPredicate> producerPredicates = predicate.getProducer().canProducedByPredicates();
			generatedUnitsForPredicate.putAll(generatePredicateUnits(producerPredicates));
			initLocal = generatedUnitsForPredicate.keySet().iterator().next();
		}

		for (CrySLMethodCall call : predicatePath) {
			Map<Local, List<Unit>> generatedCallUnits = Maps.newLinkedHashMap();

			if (call.isInitCall()) {
				CrySLVariable returnVariable = call.getCallReturn();
				initLocal = localGenerator.generateFreshLocal(returnVariable.getType(), returnVariable.getName());
				generatedUnitsForPredicate.put(initLocal, Lists.newArrayList());
				generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, true);

				if (predicateLocal == null && firstPredicateParamter.getName().equals("this")) {
					predicateLocal = initLocal;
				} else {
					if (predicateLocal == null
							&& containsPredicateLocal(firstPredicateParamter, generatedCallUnits.keySet())) {
						predicateLocal = getPredicateLocal(firstPredicateParamter, generatedCallUnits.keySet());
					}
				}
			} else {
				if (call.getCallReturn().equals(firstPredicateParamter)) {
					generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, true);
				} else if (call.getCallParameters().contains(firstPredicateParamter)) {
					generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, false);
					if (usageLocal.length > 0) {
						if (firstPredicateParamter.getType() == Scene.v().getType("java.lang.String")
								|| !(firstPredicateParamter.getType() instanceof RefType)) {
							Local genPredLocal = Lists.newArrayList(generatedCallUnits.keySet())
									.get(call.getParamterIndex(firstPredicateParamter));
							Unit assignStmt = assignGenerator.generateAssignStmt(genPredLocal, usageLocal[0]);
							generatedCallUnits.get(genPredLocal).set(0, assignStmt);
							predicateLocal = usageLocal[0];
						}
					}
				} else {
					generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, false);
				}

				if (predicateLocal == null
						&& containsPredicateLocal(firstPredicateParamter, generatedCallUnits.keySet())) {
					predicateLocal = getPredicateLocal(firstPredicateParamter, generatedCallUnits.keySet());
				}
			}
			generatedUnitsForPredicate.get(initLocal).addAll(Utils.summarizeUnitLists(generatedCallUnits.values()));
		}

		generatedUnits.put(predicateLocal, Lists.newArrayList());
		generatedUnits.get(predicateLocal).addAll(Utils.summarizeUnitLists(generatedUnitsForPredicate.values()));
		return generatedUnits;
	}

	private Local getPredicateLocal(CrySLVariable firstPredicateParamter, Set<Local> candidates) {
		for (Local candidate : candidates) {
			if (isPredicate(firstPredicateParamter, candidate)) {
				return candidate;
			}
		}
		return null;
	}

	private boolean containsPredicateLocal(CrySLVariable firstPredicateParamter, Set<Local> candidates) {
		for (Local candidate : candidates) {
			if (isPredicate(firstPredicateParamter, candidate)) {
				return true;
			}
		}
		return false;
	}

	private boolean isPredicate(CrySLVariable firstPredicateParamter, Local candidate) {
		String parameterName = firstPredicateParamter.getName();
		Type parameterType = firstPredicateParamter.getType();

		if (candidate.getName().contains(parameterName) && parameterType == candidate.getType()) {
			return true;
		} else {
			return false;
		}
	}

	public String getPredicateGenerationString(CrySLPredicate predicate, List<CrySLMethodCall> predicatePath) {
		StringBuilder builder = new StringBuilder();
		builder.append("generate Predicate:\n");
		builder.append(predicate.toString());
		builder.append("\nPath: \n");
		for (CrySLMethodCall call : predicatePath) {
			builder.append(call.getCrySLMethod() + "\n");
		}
		return builder.toString();
	}

}
