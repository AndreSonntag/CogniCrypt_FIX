package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.fsm.CrySLPathFilter;
import de.upb.cognicryptfix.exception.NoEnsuredPredicateException;
import de.upb.cognicryptfix.generator.jimple.JimpleLocalGenerator;
import soot.Body;
import soot.Local;
import soot.Unit;

public class JimplePredicateGenerator {

	private JimpleCodeGeneratorByRule codeGenerator;
	private JimpleLocalGenerator localGenerator;

	public JimplePredicateGenerator(Body body, JimpleCodeGeneratorByRule codeGenerator) {
		this.localGenerator = new JimpleLocalGenerator(body);
		this.codeGenerator = codeGenerator;
	}
	
	public Map<Local, List<Unit>> generatePredicateUnits(List<CrySLPredicate> predicates) {
		Map<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedUnitsForPredicate = Maps.newLinkedHashMap();
		List<CrySLMethodCall> predicateGenerationPath = Lists.newLinkedList();
		Local initLocal = null; 
		Local predicateLocal = null;
		
		CrySLPredicate predicate = null;
		try {
			predicate = filterPredicatesByBestGenerationPath(predicates);
		} catch (NoEnsuredPredicateException e) {
			e.printStackTrace();
		}
		
		if(predicate.getProducer().isInterfaceOrAbstract()) {
			Map<Local, List<Unit>> preUnits = Maps.newHashMap();
			List<CrySLPredicate> interfaceProducerPredicates = predicate.getProducer().canProducedByPredicates();	
			preUnits.putAll(generatePredicateUnits(interfaceProducerPredicates));
			generatedUnits.putAll(preUnits);
			initLocal = preUnits.keySet().iterator().next();
		}
		
		predicateGenerationPath = predicate.getPath();
		for(CrySLMethodCall call : predicateGenerationPath) {
			if(call.isInitCall()) {		
				CrySLVariable returnVariable = call.getCallReturn();
				initLocal = localGenerator.generateFreshLocal(returnVariable.getType(), returnVariable.getVariable());
				generatedUnits.put(initLocal,Lists.newArrayList());
				Map<Local, List<Unit>> generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, true);//predicate generation call
				
				if(predicate.getPredicateParameters().get(0).getVariable().equals("this")) {
					predicateLocal = initLocal;
				} 
				
				for(List<Unit> l : generatedCallUnits.values()) {
					generatedUnits.get(initLocal).addAll(l);	
				}
	
			} else {
				if(call.getCallReturn().equals(predicate.getPredicateParameters().get(0))) {
					Map<Local, List<Unit>> generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call,true);//predicate generation call
					
					for(Local key : generatedCallUnits.keySet()) {
						if(key.getType() == predicate.getPredicateParameters().get(0).getType()){	
							predicateLocal = key;
							break;
						}
					}
					
					for(List<Unit> l : generatedCallUnits.values()) {
						generatedUnits.get(initLocal).addAll(l);	
					}
				} else {
					Map<Local, List<Unit>> generatedCallUnits = codeGenerator.generateCallWithParameter(initLocal, call, false);	//normal call
					for(List<Unit> l : generatedCallUnits.values()) {
						generatedUnits.get(initLocal).addAll(l);	
					}
				}
			}
		}
		
		generatedUnitsForPredicate.put(predicateLocal, Lists.newArrayList());
		for(List<Unit> units : generatedUnits.values()) {
			generatedUnitsForPredicate.get(predicateLocal).addAll(units);
		}
		return generatedUnitsForPredicate;
	}

	private CrySLPredicate filterPredicatesByBestGenerationPath(List<CrySLPredicate> predicates) throws NoEnsuredPredicateException{

		if(predicates.isEmpty()) {
			throw new NoEnsuredPredicateException();
		} else if(predicates.size() < 2) {
			return predicates.get(0);
		}
		
		
		Map<List<CrySLMethodCall>, CrySLPredicate> pathPredicateMap = Maps.newHashMap();
		Map<Integer, List<LinkedList<CrySLMethodCall>>> requiredPredicatePathMap = Maps.newHashMap();

		for (CrySLPredicate predicate : predicates) {
			pathPredicateMap.put(predicate.getPath(), predicate);

			int requiredPredicates = predicate.countRequiredParameterForPath();
			if (requiredPredicatePathMap.containsKey(requiredPredicates)) {
				requiredPredicatePathMap.get(requiredPredicates).add(predicate.getPath());
			} else {
				List<LinkedList<CrySLMethodCall>> l = Lists.newArrayList();
				l.add(predicate.getPath());
				requiredPredicatePathMap.put(requiredPredicates, l);
			}
		}
		
		int minRequiredPredicateGeneration = Collections.min(requiredPredicatePathMap.keySet());
		List<LinkedList<CrySLMethodCall>> paths = requiredPredicatePathMap.get(minRequiredPredicateGeneration);
		
		paths = CrySLPathFilter.applyCriteriaFilters(paths);
		return pathPredicateMap.get(paths.get(0));
	}
	
	
	
	
}
