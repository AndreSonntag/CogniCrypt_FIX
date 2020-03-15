package de.upb.cognicryptfix.crysl;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.rules.CrySLRule;
import de.upb.cognicryptfix.crysl.fsm.call.CrySLCallFSM;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;


public class CrySLEntity {

	private CrySLRule rule;
	private CrySLCallFSM fsm;
	private CrySLVariableConstraintPool pool;
	private List<CrySLPredicate> ensuredPredicates;
	private List<CrySLPredicate> requiredPredicates;
	private Map<CrySLVariable, List<CrySLPredicate>> variableRequiredPredicateMap;
	
	public CrySLEntity(CrySLRule rule) {
		this.rule = rule;
		this.pool = new CrySLVariableConstraintPool(rule);
		this.fsm = new CrySLCallFSM(rule, pool);
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
		if (variableRequiredPredicateMap.get(variable) == null) {
			return Lists.newArrayList();
		} else {
			List<CrySLPredicate> predicates = Lists.newArrayList(variableRequiredPredicateMap.get(variable));
			for (CrySLPredicate predicate : predicates) {
				// TODO: really just the first parameter ?
				CrySLVariable firstPredicateParameter = predicate.getPredicateParameters().get(0);
				if (!JimpleUtils.isSubClassOrSubInterface(variable.getType(), firstPredicateParameter.getType())) {
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
