package de.upb.cognicryptfix.crysl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import de.upb.cognicryptfix.crysl.fsm.CrySLPathFilter;
import de.upb.cognicryptfix.exception.generation.crysl.EmptyPredicateListException;
import de.upb.cognicryptfix.exception.generation.crysl.NoPredicateEnsurerException;
import de.upb.cognicryptfix.exception.path.EmptyPathListException;
import de.upb.cognicryptfix.exception.path.PathException;
import de.upb.cognicryptfix.utils.Utils;
import soot.RefType;

public class CrySLPredicateFilter {

	public static Entry<CrySLPredicate, LinkedList<CrySLMethodCall>> applyPredicateCriteriaFilters(List<CrySLPredicate> predicates ,CrySLPredicate... spec) throws EmptyPredicateListException, PathException {
		
		if (CollectionUtils.isEmpty(predicates)) {
			throw new EmptyPredicateListException("Predicate list is empty");
		} 
		
//		List<CrySLPredicate> noInterfaceProducer = filterPredicatesByNotInterfaceProducer(predicates);
		Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> predicatePathsMap = createPredicatePathsMap(predicates);
		List<LinkedList<CrySLMethodCall>> predicateGenPaths = summarizeMapValuesToList(predicatePathsMap.values());
		if(spec.length == 1) {
			List<LinkedList<CrySLMethodCall>> pathWithoutUseOfProducer = CrySLPathFilter.filterCallPathsByNotUseOfProducer(predicateGenPaths, spec[0].getProducer());
			List<LinkedList<CrySLMethodCall>> pathWithoutUseOfSpecPred = CrySLPathFilter.filterCallPathsByNotRequiredPredicate(pathWithoutUseOfProducer, spec[0]);
			predicateGenPaths = pathWithoutUseOfSpecPred;
		}
			
		//sometimes key, sometimes secretKey
		List<LinkedList<CrySLMethodCall>> filterdPaths = CrySLPathFilter.applyPathCriteriaFilters(predicateGenPaths);
		for(CrySLPredicate predicate : predicatePathsMap.keySet()) {
			List<LinkedList<CrySLMethodCall>> paths = predicatePathsMap.get(predicate);
			if(paths.contains(filterdPaths.get(0))) {
				return new SimpleEntry<CrySLPredicate, LinkedList<CrySLMethodCall>>(predicate,filterdPaths.get(0));
			}
		}
		
		return null;
	}
	
	
	/**
	 * Returns all {@link List} with {@link CrySLPredicate} which are not produced by an interface.
	 * @param predicates
	 * @return
	 * @throws EmptyPredicateListException
	 */
	public static List<CrySLPredicate> filterPredicatesByNotInterfaceProducer(List<CrySLPredicate> predicates) throws EmptyPredicateListException{
		
		if (CollectionUtils.isEmpty(predicates)) {
			throw new EmptyPredicateListException("Predicate list is empty");
		} 
	
		List<CrySLPredicate> filteredPredicateProducer = Lists.newArrayList(predicates);
		for(CrySLPredicate predicate : predicates) {
			if(predicate.getProducer().isInterfaceOrAbstract()) {
				filteredPredicateProducer.remove(predicate);
			}
		}		
		return filteredPredicateProducer;
	}
	
	private static Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> createPredicatePathsMap(List<CrySLPredicate> predicates) throws PathException {
		Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> predicatePathsMap = Maps.newHashMap();
		for (CrySLPredicate predicate : predicates) {
			List<LinkedList<CrySLMethodCall>> paths = predicate.getPaths();
			if(!paths.isEmpty()) {
				predicatePathsMap.put(predicate, paths);
			}
		}
		return predicatePathsMap;
	}

	private static List<LinkedList<CrySLMethodCall>> summarizeMapValuesToList(Collection<List<LinkedList<CrySLMethodCall>>> values){
		List<LinkedList<CrySLMethodCall>> retList = Lists.newArrayList();
		for(List<LinkedList<CrySLMethodCall>> lists : values) {
			retList.addAll(lists);
		}		
		return retList;
	}

}
