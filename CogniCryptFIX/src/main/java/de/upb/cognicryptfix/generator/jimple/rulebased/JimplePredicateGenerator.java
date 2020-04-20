package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLPredicateFilter;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.exception.generation.crysl.CrySLGenerationException;
import de.upb.cognicryptfix.exception.generation.crysl.EmptyPredicateListException;
import de.upb.cognicryptfix.exception.generation.crysl.NoPredicateEnsurerException;
import de.upb.cognicryptfix.exception.path.PathException;
import de.upb.cognicryptfix.generator.JimpleCodeGeneratorByRule;
import de.upb.cognicryptfix.generator.jimple.JimpleAssignGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Body;
import soot.Local;
import soot.RefType;
import soot.Scene;
import soot.Type;
import soot.Unit;

public class JimplePredicateGenerator {

	private static final Logger LOGGER = LogManager.getLogger(JimplePredicateGenerator.class);
	private JimpleCodeGeneratorByRule codeGenerator;
	private JimpleLocalGenerator localGenerator;
	private JimpleAssignGenerator assignGenerator;

	public JimplePredicateGenerator(Body body, JimpleCodeGeneratorByRule codeGenerator) {
		this.localGenerator = new JimpleLocalGenerator(body);
		this.assignGenerator = new JimpleAssignGenerator();
		this.codeGenerator = codeGenerator;
	}
	
	public Map<Local, List<Unit>> generatePredicateUnits(CrySLEntity entity, CrySLVariable variable, Local usageLocal) throws CrySLGenerationException, PathException {	
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		
		if (!entity.requiresPredicate(variable)) {
			throw new CrySLGenerationException("Variable: " +variable.getName()+ " doesn't require a predicate");
		}
		
		List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(variable);			
		if(requiredPredicates.isEmpty()) {
			throw new NoPredicateEnsurerException("No Rule provides following predicate: "+requiredPredicates.toString());
		}
			
		generatedUnits.putAll(generatePredicateUnits(requiredPredicates, usageLocal));				
		return generatedUnits;
	}
	
	public Map<Local, List<Unit>> generatePredicateUnits(List<CrySLPredicate> predicates, Local... usageLocal) throws CrySLGenerationException, PathException{
		
		if (CollectionUtils.isEmpty(predicates)) {
			throw new EmptyPredicateListException("Predicate list is empty");
		} 

		Entry<CrySLPredicate, LinkedList<CrySLMethodCall>> predicateEntry = CrySLPredicateFilter.applyFilters(predicates);
		return generatePredicateUnits(predicateEntry.getKey(), predicateEntry.getValue(), usageLocal);
	}

	private Map<Local, List<Unit>> generatePredicateUnits(CrySLPredicate predicate, List<CrySLMethodCall> predicatePath, Local... usageLocal) throws CrySLGenerationException, PathException {
		LOGGER.debug("Generate Predicate: "+predicate+"\n Path: "+predicatePath.toString());
		
		Map<Local, List<Unit>> generatedUnitsForPredicate = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Local initLocal = null;
		Local predicateLocal = null;
		CrySLVariable firstPredicateParamter = predicate.getPredicateParameters().get(0);
		
		//requried i.e. preparedKeyMaterial[]  
		if (predicate.getProducer().isInterfaceOrAbstract()) {
			LOGGER.debug(predicate.getProducer().getSootClass()+" is an interface, and needs to be instantiated by an implementer!");

			List<CrySLPredicate> producerPredicates = predicate.getProducer().canProducedByPredicates();
			producerPredicates = CrySLPredicateFilter.filterPredicatesByPathWihtoutUseOfSpecifiedPredicate(predicate, producerPredicates);
			
			LOGGER.debug("Possible implementer: "+producerPredicates);

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
					if (predicateLocal == null && containsPredicateLocal(firstPredicateParamter, generatedCallUnits.keySet())) {
						predicateLocal = getPredicateLocal(firstPredicateParamter, generatedCallUnits.keySet());
					}
				}
			} else {
				if (call.getCallReturn().equals(firstPredicateParamter)) {
					generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, true);
				} else if (call.getCallParameters().contains(firstPredicateParamter)) {
					generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, false);
					if (usageLocal.length > 0) {
						if (JimpleUtils.equals(firstPredicateParamter.getType(), Scene.v().getType("java.lang.String"))|| !(firstPredicateParamter.getType() instanceof RefType)) {
							Local genPredLocal = Lists.newArrayList(generatedCallUnits.keySet()).get(call.getParamterIndex(firstPredicateParamter));
							Unit assignStmt = assignGenerator.generateAssignStmt(genPredLocal, usageLocal[0]);
							generatedCallUnits.get(genPredLocal).set(0, assignStmt);
							predicateLocal = usageLocal[0];
						}
					}
				} else {
					generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, false);
				}

				if (predicateLocal == null && containsPredicateLocal(firstPredicateParamter, generatedCallUnits.keySet())) {
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

		if (candidate.getName().contains(parameterName) && JimpleUtils.equals(parameterType, candidate.getType())) {
			return true;
		} else {
			return false;
		}
	}

}
