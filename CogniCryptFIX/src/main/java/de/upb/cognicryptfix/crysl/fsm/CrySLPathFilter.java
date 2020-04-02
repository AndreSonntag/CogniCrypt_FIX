package de.upb.cognicryptfix.crysl.fsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLMethodCallCriteria;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.utils.Utils;
import soot.RefType;
import soot.Scene;
import soot.SootClass;

/** 
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLPathFilter {

	/*
	 * check how many refs and if same count, check for predicates
	 */

	private static CrySLEntityPool pool = CrySLEntityPool.getInstance();

	public static List<LinkedList<CrySLMethodCall>> applyCriteriaFilters(List<LinkedList<CrySLMethodCall>> paths) {

		if (Utils.isNullOrEmpty(paths)) {
			return Lists.newArrayList();
		}

		List<LinkedList<CrySLMethodCall>> filterPaths = Lists.newArrayList(paths);
		filterPaths = CrySLPathFilter.filterCallPathsByUnsupportedPredicates(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByUnsupportedParameterTypes(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByFewestRefTypeGeneration(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByRefTypeRequiredPredicateRatio(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByFewestUserInteractions(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByFewestCalls(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByPrefedCallsOfEntity(filterPaths);
		return filterPaths;
	}
	
	
	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByPrefedCallsOfEntity( List<LinkedList<CrySLMethodCall>> paths) {
		
		if (paths.size() < 2) {
			return paths;
		}

		CrySLEntityPool pool = CrySLEntityPool.getInstance();
		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> countEntityCallMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			
			int countEntityCall = 0;
			
			for (CrySLMethodCall call : path) {				
				String className = call.getSootMethod().getDeclaringClass().getName();
				CrySLEntity temp = pool.getEntityByClassName(className);
				if(temp != null) {
					countEntityCall++;
				}
			}
			if (countEntityCallMap.containsKey(countEntityCall)) {
				countEntityCallMap.get(countEntityCall).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> l = Lists.newArrayList();
				l.add(path);
				countEntityCallMap.put(countEntityCall, l);
			}
		}

		int mostEntityCalls = Collections.max(countEntityCallMap.keySet());
		return countEntityCallMap.get(mostEntityCalls);		
	}
	

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByUnsupportedPredicates(List<LinkedList<CrySLMethodCall>> paths) {

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();
		for (LinkedList<CrySLMethodCall> path : paths) {			
			CrySLEntity entity = pool.getEntityByClassName(path.get(0).getRule().getClassName());
			boolean unsupportedPredicate = false;
			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(parameter);
					if (!requiredPredicates.isEmpty()) {
						for (CrySLPredicate requiredPredicate : requiredPredicates) {
							if (Constants.notSupportedPredicates.contains(requiredPredicate.getPredicateName())) {
								unsupportedPredicate = true;
							}
						}
					}
				}
			}
			if (unsupportedPredicate == false) {
				allowedPaths.add(path);
			}
		}
		return allowedPaths;
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByUnsupportedParameterTypes(
			List<LinkedList<CrySLMethodCall>> paths) {

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();
		for (LinkedList<CrySLMethodCall> path : paths) {
			boolean unsupportedParameterType = false;
			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					if (Constants.notSupportedParameterTypes.contains(parameter.getType().toQuotedString())) {
						unsupportedParameterType = true;
					}

				}
			}

			if (unsupportedParameterType == false) {
				allowedPaths.add(path);
			}
		}

		return allowedPaths;

	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByRefTypeRequiredPredicateRatio(
			List<LinkedList<CrySLMethodCall>> paths) {

		if (paths.size() < 2) {
			return paths;
		}

		Map<Double, List<LinkedList<CrySLMethodCall>>> refTypeReqPredRatioPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			CrySLEntity entity = pool.getEntityByClassName(path.get(0).getRule().getClassName());

			int countRequiredPredicates = 0;
			int countRequiredRefTypes = 0;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {

					if (parameter.getType() instanceof RefType && parameter.getType() != Scene.v().getRefType("java.lang.String")) {
						List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(parameter);
						countRequiredRefTypes++;
						if (!requiredPredicates.isEmpty()) {
							countRequiredPredicates++;
						}
					}
				}
			}

			if (countRequiredRefTypes > 0) {
				double ratio = countRequiredPredicates / countRequiredRefTypes;

				if (refTypeReqPredRatioPathMap.containsKey(ratio)) {
					refTypeReqPredRatioPathMap.get(ratio).add(path);
				} else {
					ArrayList<LinkedList<CrySLMethodCall>> l = Lists.newArrayList();
					l.add(path);
					refTypeReqPredRatioPathMap.put(ratio, l);
				}
			}
		}

		if(refTypeReqPredRatioPathMap.isEmpty()) {
			return paths;
		} else {
			double refTypeReqPredRatio = Collections.max(refTypeReqPredRatioPathMap.keySet());
			return refTypeReqPredRatioPathMap.get(refTypeReqPredRatio);
		}
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestRequiredPredicates(
			List<LinkedList<CrySLMethodCall>> paths) {

		if (paths.size() < 2) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> requiredPredicatesPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			CrySLEntity entity = pool.getEntityByClassName(path.get(0).getRule().getClassName());

			boolean unsupportedPredicate = false;
			int countRequiredPredicates = 0;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(parameter);
					if (!requiredPredicates.isEmpty()) {
						countRequiredPredicates++;
					}
				}
			}

			if (requiredPredicatesPathMap.containsKey(countRequiredPredicates)) {
				requiredPredicatesPathMap.get(countRequiredPredicates).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> l = Lists.newArrayList();
				l.add(path);
				requiredPredicatesPathMap.put(countRequiredPredicates, l);
			}
		}

		int fewestRequiredPredicateGenerations = Collections.min(requiredPredicatesPathMap.keySet());
		return requiredPredicatesPathMap.get(fewestRequiredPredicateGenerations);
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestUserInteractions(
			List<LinkedList<CrySLMethodCall>> paths) {

		if (paths.size() < 2) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> userInteractionPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			int countRequiredUserInteractions = 0;

			for (CrySLMethodCall call : path) {
				countRequiredUserInteractions += call.getCallCriteria().getRequiredUserInteractions();
			}

			if (userInteractionPathMap.containsKey(countRequiredUserInteractions)) {
				userInteractionPathMap.get(countRequiredUserInteractions).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				userInteractionPathMap.put(countRequiredUserInteractions, arrayList);
			}
		}

		int fewestUserInteractions = Collections.min(userInteractionPathMap.keySet());
		return userInteractionPathMap.get(fewestUserInteractions);
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestRefTypeGeneration(
			List<LinkedList<CrySLMethodCall>> paths) {

		if (paths.size() < 2) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> refTypeGenerationPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			int countRefTypeGeneration = 0;

			for (CrySLMethodCall call : path) {
				countRefTypeGeneration += call.getCallCriteria().getRequiredRefTypeGenerations();
			}
			if (refTypeGenerationPathMap.containsKey(countRefTypeGeneration)) {
				refTypeGenerationPathMap.get(countRefTypeGeneration).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				refTypeGenerationPathMap.put(countRefTypeGeneration, arrayList);
			}
		}

		int fewestRefTypeGenerations = Collections.min(refTypeGenerationPathMap.keySet());
		return refTypeGenerationPathMap.get(fewestRefTypeGenerations);
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByUsedPredicateParameters(
			List<LinkedList<CrySLMethodCall>> paths, List<CrySLVariable> predicatePramameters) {

		List<CrySLVariable> filteredPredicateParameters = Lists.newArrayList(predicatePramameters);
		for (CrySLVariable parameter : filteredPredicateParameters) {
			if (parameter.getName().equals("_")) {
				predicatePramameters.remove(parameter);
			} else if (parameter.getName().equals("this")) {
				return paths;
			}
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> usedPredicateParametersPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			int countParameterUsed = 0;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable predicateParameter : filteredPredicateParameters) {
					if (call.getCallParameters().contains(predicateParameter)
							|| call.getCallReturn().equals(predicateParameter)) {
						countParameterUsed++;
					}
				}
			}

			if (countParameterUsed >= filteredPredicateParameters.size()
					&& usedPredicateParametersPathMap.containsKey(countParameterUsed)) {
				usedPredicateParametersPathMap.get(countParameterUsed).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				usedPredicateParametersPathMap.put(countParameterUsed, arrayList);
			}
		}
		return usedPredicateParametersPathMap.get(filteredPredicateParameters.size());
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestCalls(
			List<LinkedList<CrySLMethodCall>> paths) {

		if (paths.size() < 2) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> lengthPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {

			if (lengthPathMap.containsKey(path.size())) {
				lengthPathMap.get(path.size()).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				lengthPathMap.put(path.size(), arrayList);
			}
		}

		int minPathLength = Collections.min(lengthPathMap.keySet());
		return lengthPathMap.get(minPathLength);
	}

	public static List<CrySLMethodCallTransition> filterTransitionsByFewestMethodCallUserInteractions(
			Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitions) {

		if (transitions.keySet().size() < 2) {
			return transitions.values().iterator().next();
		}

		Map<Integer, CrySLMethodCallCriteria> userInteractionCriteriaMap = Maps.newHashMap();
		for (CrySLMethodCallCriteria key : transitions.keySet()) {
			userInteractionCriteriaMap.put(key.getRequiredUserInteractions(), key);
		}

		int minUserInteractions = Collections.min(userInteractionCriteriaMap.keySet());
		return transitions.get(userInteractionCriteriaMap.get(minUserInteractions));
	}

	public static List<CrySLMethodCallTransition> filterTransitionsByFewestRefTypeGenerations(
			Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitions) {

		if (transitions.keySet().size() < 2) {
			return transitions.values().iterator().next();
		}

		Map<Integer, CrySLMethodCallCriteria> refTypeGenerationCriteriaMap = Maps.newHashMap();
		for (CrySLMethodCallCriteria key : transitions.keySet()) {
			refTypeGenerationCriteriaMap.put(key.getRequiredRefTypeGenerations(), key);
		}

		int minRefTypeGeneration = Collections.min(refTypeGenerationCriteriaMap.keySet());
		return transitions.get(refTypeGenerationCriteriaMap.get(minRefTypeGeneration));
	}

}
