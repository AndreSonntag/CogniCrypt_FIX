package de.upb.cognicryptfix.crysl.test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.HeadlessRepairer;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.test.ConstraintPool;
import de.upb.cognicryptfix.crysl.test.CrySLEntityNew;
import de.upb.cognicryptfix.crysl.test.VariablePool;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Type;

public class CrySLEntityPoolNew {

//	private static CrySLEntityPoolNew instance;
//	private List<CrySLRule> rules;
//	private List<CrySLEntityNew> entities;
//	private List<CrySLPredicate> predicates;
//	private Map<String, CrySLEntityNew> classNameCrySLEntityMap;
//
//	private CrySLEntityPoolNew() {
//		this.rules = HeadlessRepairer.getCrySLRules();
//		this.entities = Lists.newArrayList();
//		this.predicates = Lists.newArrayList();
//		this.classNameCrySLEntityMap = Maps.newHashMap();
//		createEntities();
//	}
//	
//	public static CrySLEntityPoolNew getInstance() {
//		if (CrySLEntityPoolNew.instance == null) {
//			CrySLEntityPoolNew.instance = new CrySLEntityPoolNew();
//		}
//		return CrySLEntityPoolNew.instance;
//	}
//	
//	
//	private void createEntities() {
//		Instant start = Instant.now();
//		
//		Map<String, CrySLEntityNew> nameEntityMap = Maps.newHashMap();
//		Map<String, VariablePool> nameVariablePoolMap = Maps.newHashMap();
//
//		for(CrySLRule rule : rules) {
//			CrySLEntityNew entity = new CrySLEntityNew(rule);
//			nameEntityMap.put(rule.getClassName(), entity);
//			
//			VariablePool varPool = new VariablePool(rule);
//			nameVariablePoolMap.put(rule.getClassName(), varPool);
//			
//			ConstraintPool conPool = new ConstraintPool(rule, varPool);
//			
//			for (crypto.rules.CrySLPredicate predicate : rule.getPredicates()) {
//				if (!predicate.isNegated()) {
//					CrySLPredicate ensuredPredicate = new CrySLPredicate(predicate, entity2);
//					predicates.add(ensuredPredicate);
//				}
//			}	
//		}
//		
//		for(CrySLEntityNew entity : nameEntityMap.values()) {
//			for(crypto.rules.CrySLPredicate predicate_ : entity.getRule().getRequiredPredicates()) {
//				for(CrySLPredicate predicate : predicates) {
//					if(predicate.getPredicate_().equals(predicate_)){
//						entity.addRequiredPredicate (new CrySLVariable(predicate_.getParameters().get(0).getName(),Utils.getType(entity.getRule(), predicate_.getParameters().get(0).getName())), predicate);
//					}
//				}
//			}	
//		}
//		
//		
//		
//		
//		Instant finish = Instant.now();
//		long timeElapsed = Duration.between(start, finish).toMillis();
//		System.out.println("Required time for CrySLEntityPool creation: "+timeElapsed+" ms");
//	}
//	
//	public CrySLEntity getEntityByClassName(String className) {
//		return classNameCrySLEntityMap.get(className);
//	}
//	
//	public List<CrySLPredicate> getPossiblePredicateCandidatesForType(Type type) {
//		
//		if(!existPossiblePredicateCandidate(type)) {
//			return Lists.newArrayList();
//		}
//		
//		List<CrySLPredicate> candidates = Lists.newArrayList();
//		
//		for(CrySLPredicate predicate : predicates) {
//			CrySLVariable firstPredicateParameter = predicate.getPredicateParameters().get(0);
//			if (JimpleUtils.isEqualOrSubClassOrSubInterface(type, firstPredicateParameter.getType())) {
//				candidates.add(predicate);
//			}
//		}
//		return candidates;
//	}
//	
//	private boolean existPossiblePredicateCandidate(Type type) {
//		for(String name : classNameCrySLEntityMap.keySet()) {
//			if(type.toQuotedString().equals(name)) {
//				return true;
//			}
//		}
//		return false;
//	}
//	
}
