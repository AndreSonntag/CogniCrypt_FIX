package de.upb.cognicryptfix.crysl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.AbstractMap.SimpleEntry;
import java.util.Map.Entry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.fsm.CrySLPathFilter;
import de.upb.cognicryptfix.exception.NoEnsuredPredicateException;
import de.upb.cognicryptfix.utils.Utils;

public class CrySLPredicateFilter {

	public static Entry<CrySLPredicate, LinkedList<CrySLMethodCall>> applyFilters(List<CrySLPredicate> predicates) throws NoEnsuredPredicateException {
		
		if (Utils.isNullOrEmpty(predicates)) {
			throw new NoEnsuredPredicateException();
		} 
		
		Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> predicatePathsMap = createPredicatePathsMap(predicates);
		List<LinkedList<CrySLMethodCall>> values = summarizeValuesToList(predicatePathsMap.values());
		List<LinkedList<CrySLMethodCall>> filterdPaths = CrySLPathFilter.applyCriteriaFilters(values);
		
		for(CrySLPredicate predicate : predicatePathsMap.keySet()) {
			List<LinkedList<CrySLMethodCall>> predicatePaths = predicatePathsMap.get(predicate);
			if(predicatePaths.contains(filterdPaths.get(0))) {
				return new SimpleEntry<CrySLPredicate, LinkedList<CrySLMethodCall>>(predicate,filterdPaths.get(0));
			}
		}
		
		return null;
	}
		
	private static Map<CrySLPredicate, List<LinkedList<CrySLMethodCall>>> createPredicatePathsMap(List<CrySLPredicate> predicates) {
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
