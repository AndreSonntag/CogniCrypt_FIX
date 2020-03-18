package de.upb.cognicryptfix.crysl.fsm.call;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLMethodCallCriteria;
import de.upb.cognicryptfix.crysl.CrySLVariable;

/**
 * TODO: documentation
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLPathFilter {

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestRequiredPredicates(List<LinkedList<CrySLMethodCall>> paths, CrySLEntity entity) {
		
		if(paths.size() < 2) {
			return paths;
		}
		
		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> requiredPredicatesPathMap = Maps.newHashMap();
		for(LinkedList<CrySLMethodCall> path : paths) {
			int countRequiredPredicates = 0;
			
			for(CrySLMethodCall call : path) {
				for(CrySLVariable parameter : call.getCallParameters()) {
					if(!entity.getRequiredPredicateForVariable(parameter).isEmpty()) {
						countRequiredPredicates++;
					}
				}
			}
			
			if(requiredPredicatesPathMap.containsKey(countRequiredPredicates)) {
				requiredPredicatesPathMap.get(countRequiredPredicates).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> l = Lists.newArrayList();
				l.add(path);
				requiredPredicatesPathMap.put(countRequiredPredicates,l);
			}
		}
		
		int fewestRequiredPredicateGenerations = Collections.min(requiredPredicatesPathMap.keySet());
		return requiredPredicatesPathMap.get(fewestRequiredPredicateGenerations);
	}
	
	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestUserInteractions(List<LinkedList<CrySLMethodCall>> paths) {
		
		if(paths.size() < 2) {
			return paths;
		}
		
		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> userInteractionPathMap = Maps.newHashMap();
		for(LinkedList<CrySLMethodCall> path : paths) {
			int countRequiredUserInteractions = 0;
			
			for(CrySLMethodCall call : path) {
				countRequiredUserInteractions += call.getCallCriteria().getRequiredUserInteractions();
			}
			
			if(userInteractionPathMap.containsKey(countRequiredUserInteractions)) {
				userInteractionPathMap.get(countRequiredUserInteractions).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				userInteractionPathMap.put(countRequiredUserInteractions,arrayList);
			}
		}
		
		int fewestUserInteractions = Collections.min(userInteractionPathMap.keySet());
		return userInteractionPathMap.get(fewestUserInteractions);
	}
	
	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestRefTypeGeneration(List<LinkedList<CrySLMethodCall>> paths) {
		
		if(paths.size() < 2) {
			return paths;
		}
		
		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> refTypeGenerationPathMap = Maps.newHashMap();
		for(LinkedList<CrySLMethodCall> path : paths) {
			int countRefTypeGeneration = 0;
			
			for(CrySLMethodCall call : path) {
				countRefTypeGeneration += call.getCallCriteria().getRequiredRefTypeGenerations();
			}
			
			if(refTypeGenerationPathMap.containsKey(countRefTypeGeneration)) {
				refTypeGenerationPathMap.get(countRefTypeGeneration).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				refTypeGenerationPathMap.put(countRefTypeGeneration,arrayList);
			}
		}
		
		int fewestRefTypeGenerations = Collections.min(refTypeGenerationPathMap.keySet());
		return refTypeGenerationPathMap.get(fewestRefTypeGenerations);
	}
	
	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByUsedPredicateParameters(List<LinkedList<CrySLMethodCall>> paths, List<CrySLVariable> predicatePramameters) {
	
		List<CrySLVariable> filteredPredicateParameters = Lists.newArrayList(predicatePramameters);
		for (CrySLVariable parameter : filteredPredicateParameters) {
			if (parameter.getVariable().equals("_")) {
				predicatePramameters.remove(parameter);
			} else if(parameter.getVariable().equals("this")) {
				return paths;
			}
		}
		
		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> usedPredicateParametersPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {			
			int countParameterUsed = 0;
			
			for(CrySLMethodCall call : path) {
				for (CrySLVariable predicateParameter : filteredPredicateParameters) {					
					if(call.getCallParameters().contains(predicateParameter) || call.getCallReturn().equals(predicateParameter)) {
						countParameterUsed++;
					}
				}
			}
			
			
			if(usedPredicateParametersPathMap.containsKey(countParameterUsed)) {
				usedPredicateParametersPathMap.get(countParameterUsed).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				usedPredicateParametersPathMap.put(countParameterUsed,arrayList);
			}			
		}
		return usedPredicateParametersPathMap.get(filteredPredicateParameters.size());
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestCalls(List<LinkedList<CrySLMethodCall>> paths) {
		
		if(paths.size() < 2) {
			return paths;
		}
		
		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> lengthPathMap = Maps.newHashMap();
		for(LinkedList<CrySLMethodCall> path : paths) {
			
			if(lengthPathMap.containsKey(path.size())) {
				lengthPathMap.get(path.size()).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				lengthPathMap.put(path.size(),arrayList);
			}
		}
		
		int minPathLength = Collections.min(lengthPathMap.keySet());
		return lengthPathMap.get(minPathLength);
	}
	
	@Deprecated
	public static void filterTransitionsByFewestMethodCallUserInteractions(Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitions) {
		
		List<Integer> userInteractions = Lists.newArrayList();
		for (CrySLMethodCallCriteria key : transitions.keySet()) {
			userInteractions.add(key.getRequiredUserInteractions());
		}
		int minUserInteractions = Collections.min(userInteractions);
		for (CrySLMethodCallCriteria key : Sets.newHashSet(transitions.keySet())) {
			if (key.getRequiredUserInteractions() > minUserInteractions) {
				transitions.remove(key);
			}
		}
	}

	@Deprecated
	public static void filterTransitionsByFewestRefTypeGenerations(Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitions) {
	
		List<Integer> refTypeGenerations = Lists.newArrayList();
		for (CrySLMethodCallCriteria key : transitions.keySet()) {
			refTypeGenerations.add(key.getRequiredRefTypeGenerations());
		}
		
		int minRefTypeGenerations = Collections.min(refTypeGenerations);
		for (CrySLMethodCallCriteria key : Sets.newHashSet(transitions.keySet())) {
			if (key.getRequiredRefTypeGenerations() > minRefTypeGenerations) {
				transitions.remove(key);
			}
		}
	}
	
}
