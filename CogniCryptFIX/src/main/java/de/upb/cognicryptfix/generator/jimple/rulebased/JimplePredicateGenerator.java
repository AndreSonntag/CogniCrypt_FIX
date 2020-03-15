package de.upb.cognicryptfix.generator.jimple.rulebased;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.fsm.call.CrySLPathFilter;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Local;
import soot.Scene;
import soot.Unit;

public class JimplePredicateGenerator {

	private JimpleCodeGeneratorByRule codeGenerator;

	public JimplePredicateGenerator(JimpleCodeGeneratorByRule codeGenerator) {
		this.codeGenerator = codeGenerator;
	}

	//
	public Map<Local, List<Unit>> generatePredicateUnits(List<CrySLPredicate> predicates) {
		LinkedHashMap<Local, List<Unit>> generatedUnits = Maps.newLinkedHashMap();
		Map<Local, List<Unit>> generatedUnitsForPredicate = Maps.newLinkedHashMap();

		CrySLPredicate predicate = filterPredicatesByBestGenerationPath(predicates);
		List<CrySLMethodCall> predicateGenerationPath = predicate.getPath();
		Local classLocal = null;
		for(CrySLMethodCall call : predicateGenerationPath) {
			if(generatedUnits.isEmpty()) {
				generatedUnits.putAll(codeGenerator.generateCallWithParameter(call,true));
				
				for(Local key : generatedUnits.keySet()) {
					if(key.getType() == Scene.v().getType(predicate.getProducer().getRule().getClassName())){
						classLocal = key;
						break;
					}
				}
				
			} else {
				if(call.getCallReturn().equals(predicate.getPredicateParameters().get(0))) {
					generatedUnits.putAll(codeGenerator.generateCallWithParameter(classLocal, call,true));
				} else {
					generatedUnits.putAll(codeGenerator.generateCallWithParameter(classLocal, call, false));
				}
			}
		}
		
		Local predicateLocal = null;
		for(Local key : generatedUnits.keySet()) {
			if(key.getName().contains(predicate.getPredicateParameters().get(0).getVariable())){
				predicateLocal = key;
				break;
			}
		}
		
		generatedUnitsForPredicate.put(predicateLocal, Lists.newArrayList());
		for(List<Unit> units : generatedUnits.values()) {
			generatedUnitsForPredicate.get(predicateLocal).addAll(units);
		}
		return generatedUnitsForPredicate;
	}

	private CrySLPredicate filterPredicatesByBestGenerationPath(List<CrySLPredicate> predicates) {

		Map<LinkedList<CrySLMethodCall>, CrySLPredicate> pathPredicateMap = Maps.newHashMap();
		Map<Integer, List<LinkedList<CrySLMethodCall>>> requiredPredicatePathMap = Maps.newHashMap();

		for (CrySLPredicate predicate : predicates) {
			pathPredicateMap.put(predicate.getPath(), predicate);

			int requiredPredicates = predicate.countRequiredParameterForPath();
			if (requiredPredicatePathMap.containsKey(requiredPredicates)) {
				requiredPredicatePathMap.get(requiredPredicates).add(predicate.getPath());
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> l = Lists.newArrayList();
				l.add(predicate.getPath());
				requiredPredicatePathMap.put(requiredPredicates, l);
			}
		}

		int minRequiredPredicateGeneration = Collections.min(requiredPredicatePathMap.keySet());
		List<LinkedList<CrySLMethodCall>> paths = requiredPredicatePathMap.get(minRequiredPredicateGeneration);
		CrySLPathFilter.filterCallPathsByFewestUserInteractions(paths);
		CrySLPathFilter.filterCallPathsByFewestCalls(paths);

		return pathPredicateMap.get(paths.get(0));
	}
}
