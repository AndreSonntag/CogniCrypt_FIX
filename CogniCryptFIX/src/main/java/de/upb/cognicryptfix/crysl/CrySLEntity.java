package de.upb.cognicryptfix.crysl;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.crysl.fsm.CrySLCallFSM;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.crysl.pool.CrySLVariablePool;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import soot.Scene;
import soot.SootClass;

public class CrySLEntity {

	private CrySLRule rule;
	private CrySLCallFSM fsm;
	private CrySLVariablePool pool;
	
	private List<CrySLPredicate> ensuredPredicates;
	private List<CrySLPredicate> requiredPredicates;
	private Map<CrySLVariable, List<CrySLPredicate>> variableRequiredPredicateMap;
	
	private SootClass clazz;
	private boolean interfaceOrAbstract;

	public CrySLEntity(CrySLRule rule) {
		this.clazz = Scene.v().getSootClass(rule.getClassName());
		this.interfaceOrAbstract = clazz.isAbstract() || clazz.isInterface() ? true : false;
		this.rule = rule;
		this.pool = new CrySLVariablePool(rule);
		this.fsm = new CrySLCallFSM(this, pool);
		this.ensuredPredicates = Lists.newArrayList();
		this.requiredPredicates = Lists.newArrayList();
		this.variableRequiredPredicateMap = Maps.newHashMap();
		generateEnsuredPredicates();
	}
	
	private void generateEnsuredPredicates() {
		for (crypto.rules.CrySLPredicate predicate : rule.getPredicates()) {
			if (!predicate.isNegated()) {
				CrySLPredicate ensuredPredicate = new CrySLPredicate(predicate, this);
				ensuredPredicates.add(ensuredPredicate);
			}
		}
	}

	public void addRequiredPredicate(CrySLVariable var, CrySLPredicate predicate) {
		requiredPredicates.add(predicate);
		if (variableRequiredPredicateMap.containsKey(var)) {
			variableRequiredPredicateMap.get(var).add(predicate);
		} else {
			List<CrySLPredicate> requiredPredicateList = Lists.newArrayList();
			requiredPredicateList.add(predicate);
			variableRequiredPredicateMap.put(var, requiredPredicateList);
		}
	}

	public boolean requiresPredicate(CrySLVariable variable) {
		if (variableRequiredPredicateMap.get(variable) == null) {
			return false;
		} else {
			return true;
		}
	}
	
	public List<CrySLPredicate> getRequiredPredicateForVariable(CrySLVariable variable) {
		//TODO improve by real hierrachy
		
		if (variableRequiredPredicateMap.get(variable) == null) {
			return Lists.newArrayList();
		} else {
			List<CrySLPredicate> predicates = Lists.newArrayList(variableRequiredPredicateMap.get(variable));
			for (CrySLPredicate predicate : Lists.newArrayList(predicates)) {
				// TODO: really just the first parameter ?
				CrySLVariable firstPredicateParameter = predicate.getPredicateParameters().get(0);
				if (!JimpleUtils.isEqualOrSubClassOrSubInterface(variable.getType(), firstPredicateParameter.getType())) {
					predicates.remove(predicate);
				}
			}
			return predicates;
		}
	}

	public CrySLRule getRule() {
		return rule;
	}

	public CrySLCallFSM getFSM() {
		return fsm;
	}

	public List<CrySLPredicate> getRequiredPredicates() {
		return requiredPredicates;
	}

	public List<CrySLPredicate> getEnsuredPredicates() {
		return ensuredPredicates;
	}

	public List<crypto.rules.CrySLPredicate> getRequiredPredicates_() {
		return rule.getRequiredPredicates();
	}

	public SootClass getSootClass() {
		return clazz;
	}
	
	public boolean isInterfaceOrAbstract() {
		return interfaceOrAbstract;
	}
	
	public List<CrySLPredicate> canProducedByPredicates(){
		List<CrySLPredicate> producerPredicates= Lists.newArrayList();
		if(isInterfaceOrAbstract()) {
			List<CrySLPredicate> predicates = CrySLEntityPool.getInstance().getPossiblePredicateCandidatesForType(clazz.getType());
			if (!predicates.isEmpty()) {
				producerPredicates = predicates;
			}
		}
		return producerPredicates;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CrySLEntity [rule=");
		builder.append(rule.getClassName());
		builder.append(", ensuredPredicates=");
		builder.append(ensuredPredicates);
		builder.append(", requiredPredicates=");
		builder.append(requiredPredicates);
		builder.append("]");
		return builder.toString();
	}

}
