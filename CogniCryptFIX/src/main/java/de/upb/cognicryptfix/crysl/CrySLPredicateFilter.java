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

public class CrySLPredicateFilter {

	public static Entry<CrySLPredicate, LinkedList<CrySLMethodCall>> applyFilters(List<CrySLPredicate> predicates) throws EmptyPredicateListException, PathException {
		
		if (CollectionUtils.isEmpty(predicates)) {
			throw new EmptyPredicateListException("Predicate list is empty");
		} 
		
		Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> predicatePathsMap = createPredicatePathsMap(predicates);
		List<LinkedList<CrySLMethodCall>> values = summarizeValuesToList(predicatePathsMap.values());
		List<LinkedList<CrySLMethodCall>> filterdPaths = CrySLPathFilter.applyPathCriteriaFilters(values);
		
		for(CrySLPredicate predicate : predicatePathsMap.keySet()) {
			List<LinkedList<CrySLMethodCall>> predicatePaths = predicatePathsMap.get(predicate);
			if(predicatePaths.contains(filterdPaths.get(0))) {
				return new SimpleEntry<CrySLPredicate, LinkedList<CrySLMethodCall>>(predicate,filterdPaths.get(0));
			}
		}
		
		return null;
	}
	
	
	public static List<CrySLPredicate> filterPredicatesByPathWihtoutUseOfSpecifiedPredicate(CrySLPredicate spec, List<CrySLPredicate> predicates) throws EmptyPredicateListException, PathException{
		
		if (CollectionUtils.isEmpty(predicates)) {
			throw new EmptyPredicateListException("Predicate list is empty");
		} 
		
		List<CrySLPredicate> interfaceProducer = Lists.newArrayList();
		for(CrySLPredicate predicate : predicates) {
			if(predicate.getProducer().isInterfaceOrAbstract()) {
				interfaceProducer.add(predicate);
			}
		}
		
		predicates.removeAll(interfaceProducer);
		Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> predicatePathsMap = createPredicatePathsMap(predicates);
		List<LinkedList<CrySLMethodCall>> paths = summarizeValuesToList(predicatePathsMap.values());
		List<LinkedList<CrySLMethodCall>> pathWithUseOfSpecPred = CrySLPathFilter.filterCallPathsByUsedPredicateParameters(paths, spec.getPredicateParameters());
		pathWithUseOfSpecPred.retainAll(paths);
		paths.removeAll(pathWithUseOfSpecPred);

		Set<CrySLPredicate> filteredPredicates = Sets.newHashSet();
		for(CrySLPredicate predicate : predicatePathsMap.keySet()) {
			List<LinkedList<CrySLMethodCall>> predicatePaths = predicatePathsMap.get(predicate);
			for(LinkedList<CrySLMethodCall> path : paths) {
				if(predicatePaths.contains(path)) {
					filteredPredicates.add(predicate);
					continue;
				}
			}
		}
		
		return Lists.newArrayList(filteredPredicates);
	}
	
	
	private static Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> createPredicatePathsMap(List<CrySLPredicate> predicates) throws PathException {
		Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> predicatePathsMap = Maps.newHashMap();
		for (CrySLPredicate predicate : predicates) {
			predicatePathsMap.put(predicate, predicate.getPaths());
		}
		return predicatePathsMap;
	}

	private static List<LinkedList<CrySLMethodCall>> summarizeValuesToList(Collection<List<LinkedList<CrySLMethodCall>>> values){
		List<LinkedList<CrySLMethodCall>> retList = Lists.newArrayList();
		for(List<LinkedList<CrySLMethodCall>> lists : values) {
			retList.addAll(lists);
		}		
		return retList;
	}

}
