package de.upb.cognicryptfix.crysl.fsm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.Constants;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.exception.path.EmptyPathListException;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import soot.ArrayType;
import soot.RefType;
import soot.Scene;

/**
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLPathFilter {

	/*
	 * check how many refs and if same count, check for predicates
	 */

	private static CrySLEntityPool pool = CrySLEntityPool.getInstance();

	public static List<LinkedList<CrySLMethodCall>> applyPathCriteriaFilters(List<LinkedList<CrySLMethodCall>> paths)
			throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		}

		List<LinkedList<CrySLMethodCall>> filterPaths = Lists.newArrayList(paths);
		filterPaths = CrySLPathFilter.filterCallPathsByNotUseOfUnsupportedPredicates(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByNotUseOfUnsupportedParameterTypes(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByNotUseOfUnsupportedPredicateProviders(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByNotUseOfSelfProducedRequiredPredicate(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByFewestRefTypeGeneration(filterPaths);
//		filterPaths = CrySLPathFilter.filterCallPathsByFewestArrayTypeGeneration(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByRefTypeRequiredPredicateRatio(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByFewestUserInteractions(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByFewestParameters(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByFewestCalls(filterPaths);
		filterPaths = CrySLPathFilter.filterCallPathsByPrefedCallsOfEntity(filterPaths);
		return filterPaths;
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByNotUseOfSelfProducedRequiredPredicate(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();

		for (LinkedList<CrySLMethodCall> path : paths) {
			CrySLEntity entity = pool.getEntityByClassName(path.get(0).getRule().getClassName());
			List<CrySLPredicate> ensuredPredicates = entity.getEnsuredPredicates();
			boolean reqEnsuredPredicate = false;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(parameter);
					for (CrySLPredicate ensPredicate : ensuredPredicates) {
						for (CrySLPredicate reqPredicate : requiredPredicates) {
							if (ensPredicate.getPredicateName().equals(reqPredicate.getPredicateName())
									&& JimpleUtils.equals(ensPredicate.getPredicateParameters().get(0).getType(),
											reqPredicate.getPredicateParameters().get(0).getType())) {
								reqEnsuredPredicate = true;
							}
						}
					}
				}
			}

			if (!reqEnsuredPredicate) {
				allowedPaths.add(path);
			}
		}

		return allowedPaths;
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByRefTypeRequiredPredicateRatio(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		Map<Double, List<LinkedList<CrySLMethodCall>>> refTypeReqPredRatioPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			CrySLEntity entity = pool.getEntityByClassName(path.get(0).getRule().getClassName());

			int countRequiredPredicates = 0;
			int countRequiredRefTypes = 0;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {

					if ((parameter.getType() instanceof RefType
							&& !JimpleUtils.equals(parameter.getType(), Scene.v().getRefType("java.lang.String")))) {
						List<CrySLPredicate> requiredPredicates = entity
								.getRequiredPredicateForVariableByType(parameter);
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

		if (refTypeReqPredRatioPathMap.isEmpty()) {
			return paths;
		} else {
			double refTypeReqPredRatio = Collections.max(refTypeReqPredRatioPathMap.keySet());
			return refTypeReqPredRatioPathMap.get(refTypeReqPredRatio);
		}
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestRequiredPredicates(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> requiredPredicatesPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			CrySLEntity entity = pool.getEntityByClassName(path.get(0).getRule().getClassName());

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

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestParameters(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> countParameterPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			int countParameter = 0;

			for (CrySLMethodCall call : path) {
				countParameter += call.getCallParameters().size();
			}

			if (countParameterPathMap.containsKey(countParameter)) {
				countParameterPathMap.get(countParameter).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				countParameterPathMap.put(countParameter, arrayList);
			}
		}

		int lowestNumberOfParameter = Collections.min(countParameterPathMap.keySet());
		return countParameterPathMap.get(lowestNumberOfParameter);
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestUserInteractions(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> userInteractionPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			int countRequiredUserInteractions = 0;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					if (parameter.getName().equals("_")) {
						countRequiredUserInteractions += 2;
					} else {
						if (call.getPool().getVariableConstraint(parameter) == null) {
							countRequiredUserInteractions++;
						}
					}
				}
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
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> refTypeGenerationPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			int countRefTypeGeneration = 0;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					if (parameter.getType() instanceof RefType
							&& parameter.getType() != Scene.v().getType("java.lang.String")) {
						countRefTypeGeneration++;
					}
				}
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

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestArrayTypeGeneration(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> arrayTypeGenerationPathMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {
			int countArrayTypeGeneration = 0;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					if (parameter.getType() instanceof ArrayType) {
						countArrayTypeGeneration++;
					}
				}
			}
			if (arrayTypeGenerationPathMap.containsKey(countArrayTypeGeneration)) {
				arrayTypeGenerationPathMap.get(countArrayTypeGeneration).add(path);
			} else {
				ArrayList<LinkedList<CrySLMethodCall>> arrayList = Lists.newArrayList();
				arrayList.add(path);
				arrayTypeGenerationPathMap.put(countArrayTypeGeneration, arrayList);
			}
		}

		int fewestArrayTypeGenerations = Collections.min(arrayTypeGenerationPathMap.keySet());
		return arrayTypeGenerationPathMap.get(fewestArrayTypeGenerations);
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByFewestCalls(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
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

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByPrefedCallsOfEntity(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		CrySLEntityPool pool = CrySLEntityPool.getInstance();
		Map<Integer, ArrayList<LinkedList<CrySLMethodCall>>> countEntityCallMap = Maps.newHashMap();
		for (LinkedList<CrySLMethodCall> path : paths) {

			int countEntityCall = 0;

			for (CrySLMethodCall call : path) {
				String className = call.getSootMethod().getDeclaringClass().getName();
				CrySLEntity temp = pool.getEntityByClassName(className);
				if (temp != null) {
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

	/**
	 * Returns all paths in which the predicate parameters is used. This method is
	 * used for the Predicate path calculation.
	 * 
	 * @param paths
	 * @param predicatePramameters
	 * @return
	 * @throws EmptyPathListException
	 */
	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByUsedPredicateParameters(
			List<LinkedList<CrySLMethodCall>> paths, List<CrySLVariable> predicatePramameters)
			throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		List<CrySLVariable> filteredPredicateParameters = Lists.newArrayList(predicatePramameters);
		for (CrySLVariable parameter : filteredPredicateParameters) {
			if (parameter.getName().equals("_")) {
				predicatePramameters.remove(parameter);
			} else if (parameter.getName().equals("this")) {
				return paths;
			}
		}

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();
		for (LinkedList<CrySLMethodCall> path : paths) {
			boolean useOfPredicateParamter = false;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable predicateParameter : filteredPredicateParameters) {
					if (call.getCallParameters().contains(predicateParameter)
							|| call.getCallReturn().equals(predicateParameter)) {
						useOfPredicateParamter = true;
					}
				}
			}

			if (useOfPredicateParamter) {
				allowedPaths.add(path);
			}

		}
		return allowedPaths;
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByNotRequiredPredicate(
			List<LinkedList<CrySLMethodCall>> paths, CrySLPredicate predicate) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();
		CrySLEntityPool pool = CrySLEntityPool.getInstance();
		for (LinkedList<CrySLMethodCall> path : paths) {

			boolean useOfPredicate = false;

			for (CrySLMethodCall call : path) {
				String className = call.getSootMethod().getDeclaringClass().getName();
				CrySLEntity entity = pool.getEntityByClassName(className);
				if (entity != null) {
					for (CrySLVariable parameter : call.getCallParameters()) {
						List<CrySLPredicate> reqPredicates = entity.getRequiredPredicateForVariableByType(parameter);
						for (CrySLPredicate reqPredicate : reqPredicates) {
							if (reqPredicate.getPredicateName().equals(predicate.getPredicateName())
									&& JimpleUtils.equals(reqPredicate.getPredicateParameters().get(0).getType(),
											predicate.getPredicateParameters().get(0).getType())) {
								useOfPredicate = true;
								continue;
							}
						}
					}
				}
			}

			if (!useOfPredicate) {
				allowedPaths.add(path);
			}
		}
		return allowedPaths;
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByNotUseOfProducer(
			List<LinkedList<CrySLMethodCall>> paths, CrySLEntity entity) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();
		for (LinkedList<CrySLMethodCall> path : paths) {
			boolean useOfProducer = false;

			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					if (JimpleUtils.equals(parameter.getType(), entity.getSootClass().getType())) {
						useOfProducer = true;
					}
				}
			}

			if (!useOfProducer) {
				allowedPaths.add(path);
			}

		}
		return allowedPaths;
	}

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByNotUseOfUnsupportedPredicates(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();
		for (LinkedList<CrySLMethodCall> path : paths) {
			CrySLEntity entity = pool.getEntityByClassName(path.get(0).getRule().getClassName());
			boolean unsupportedPredicate = false;
			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					List<CrySLPredicate> requiredPredicates = entity.getRequiredPredicateForVariableByType(parameter);
					if (!requiredPredicates.isEmpty()) {
						for (CrySLPredicate requiredPredicate : requiredPredicates) {
							if (Constants.NOT_SUPPORTED_PREDICATES.contains(requiredPredicate.getPredicateName())) {
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

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByNotUseOfUnsupportedParameterTypes(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();
		for (LinkedList<CrySLMethodCall> path : paths) {
			boolean unsupportedParameterType = false;
			for (CrySLMethodCall call : path) {
				for (CrySLVariable parameter : call.getCallParameters()) {
					if (Constants.NOT_SUPPORTED_PARAMETER_TYPES.contains(parameter.getType().toQuotedString())) {
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

	public static List<LinkedList<CrySLMethodCall>> filterCallPathsByNotUseOfUnsupportedPredicateProviders(
			List<LinkedList<CrySLMethodCall>> paths) throws EmptyPathListException {

		if (CollectionUtils.isEmpty(paths)) {
			throw new EmptyPathListException("Path list is empty or null");
		} else if (paths.size() == 1) {
			return paths;
		}

		List<LinkedList<CrySLMethodCall>> allowedPaths = Lists.newArrayList();
		for (LinkedList<CrySLMethodCall> path : paths) {
			boolean unsupportedPredicateProvider = false;
			if (Constants.NOT_SUPPORTED_PREDICATE_PROVIDER.contains(path.get(0).getRule().getClassName())) {
				unsupportedPredicateProvider = true;
			}

			if (unsupportedPredicateProvider == false) {
				allowedPaths.add(path);
			}
		}

		return allowedPaths;

	}
}
