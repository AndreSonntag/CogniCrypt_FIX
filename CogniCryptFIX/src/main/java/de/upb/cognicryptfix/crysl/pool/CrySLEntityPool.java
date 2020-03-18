package de.upb.cognicryptfix.crysl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.HeadlessRepairer;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Type;

public class CrySLEntityPool {

	private static CrySLEntityPool instance;
	private List<CrySLRule> rules;
	private List<CrySLEntity> entities;
	private List<CrySLPredicate> predicates;
	private Map<String, CrySLEntity> classNameEntityMap;

	private CrySLEntityPool() {
		this.rules = HeadlessRepairer.getCrySLRules();
		this.entities = Lists.newArrayList();
		this.predicates = Lists.newArrayList();
		this.classNameEntityMap = Maps.newHashMap();
		createEntities();
	}
	
	public static CrySLEntityPool getInstance() {
		if (CrySLEntityPool.instance == null) {
			CrySLEntityPool.instance = new CrySLEntityPool();
		}
		return CrySLEntityPool.instance;
	}
	
	private void createEntities() {
		
		Instant start = Instant.now();
		
		for(CrySLRule rule : rules) {
			CrySLEntity entity = new CrySLEntity(rule);
			entities.add(entity);
			predicates.addAll(entity.getEnsuredPredicates());
			classNameEntityMap.put(rule.getClassName(), entity);
		}
		
		for(CrySLEntity entity : entities) {
			for(crypto.rules.CrySLPredicate predicate_ : entity.getRequiredPredicates_()) {
				for(CrySLPredicate predicate : predicates) {
					if(predicate.getPredicate_().equals(predicate_)){
						entity.addRequiredPredicate
						(new CrySLVariable(predicate_.getParameters().get(0).getName(),Utils.getType(entity.getRule(), predicate_.getParameters().get(0).getName())), predicate);
					}
				}
			}	
		}
		
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		System.out.println("Required time for CrySLEntityFSM creation: "+timeElapsed+" ms");
	}
	
	public CrySLEntity getEntityByClassName(String className) {
		return classNameEntityMap.get(className);
	}
	
	public List<CrySLPredicate> getEnsuredPredicatesByVariableType(Type type) {
		
		List<CrySLPredicate> candidates = Lists.newArrayList();
		
		for(CrySLPredicate predicate : predicates) {
			CrySLVariable firstPredicateParameter = predicate.getPredicateParameters().get(0);
			if (JimpleUtils.isSubClassOrSubInterface(type, firstPredicateParameter.getType())) {
				candidates.add(predicate);
			}
		}
		return candidates;
	}
	
	public List<CrySLPredicate> getPredicates() {
		return predicates;
	}

	public List<CrySLRule> getRules() {
		return rules;
	}
}
