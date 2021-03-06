package de.upb.cognicryptfix.crysl.pool;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.analysis.CryptoAnalysis;
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLPredicate;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import de.upb.cognicryptfix.utils.Utils;
import soot.Type;

public class CrySLEntityPool {

	private static final Logger logger = LogManager.getLogger(CrySLEntityPool.class);
	private static CrySLEntityPool instance;
	private List<CrySLRule> rules;
	private List<CrySLEntity> entities;
	private List<CrySLPredicate> predicates;
	private Map<String, CrySLEntity> classNameCrySLEntityMap;


	private CrySLEntityPool() {
		this.rules = CryptoAnalysis.getCrySLRules();
		this.entities = Lists.newArrayList();
		this.predicates = Lists.newArrayList();
		this.classNameCrySLEntityMap = Maps.newHashMap();
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
			classNameCrySLEntityMap.put(rule.getClassName(), entity);
		}
		
		for(CrySLEntity entity : entities) {
			for(crypto.rules.CrySLPredicate predicate_ : entity.getRequiredPredicates_()) {
				for(CrySLPredicate predicate : predicates) {
					if(predicate.getPredicate_().equals(predicate_)){
						entity.addRequiredPredicate(new CrySLVariable(predicate_.getParameters().get(0).getName(),Utils.getType(entity.getRule(), predicate_.getParameters().get(0).getName())), predicate);
					}
				}
			}	
		}
		
		Instant finish = Instant.now();
		long timeElapsed = Duration.between(start, finish).toMillis();
		logger.info("Required time for CrySLEntityPool creation: "+timeElapsed+" ms");
	}
	
	public CrySLEntity getEntityByClassName(String className) {
		return classNameCrySLEntityMap.get(className);
	}
	
	public List<CrySLPredicate> getPredicateCandidatesWhichProduceType(Type type) {
		List<CrySLPredicate> candidates = Lists.newArrayList();

		for(CrySLPredicate predicate : predicates) {
			CrySLVariable firstPredicateParameter = predicate.getPredicateParameters().get(0);
			if (JimpleUtils.isSubClassOrImplementer(type, firstPredicateParameter.getType())) {
				candidates.add(predicate);
			}
		}
		return candidates;
	}
	

	public List<CrySLPredicate> getPredicates() {
		return predicates;
	}
	
	
	
}
