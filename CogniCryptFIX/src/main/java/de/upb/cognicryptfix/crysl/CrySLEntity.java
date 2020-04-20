package de.upb.cognicryptfix.crysl;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.AbstractMap.SimpleEntry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CrySLRule;
import crypto.rules.CrySLValueConstraint;
import de.upb.cognicryptfix.crysl.fsm.CrySLCallFSM;
import de.upb.cognicryptfix.crysl.pool.CrySLEntityPool;
import de.upb.cognicryptfix.crysl.pool.CrySLVariablePool;
import de.upb.cognicryptfix.exception.generation.NoInterfaceImplementerException;
import de.upb.cognicryptfix.generator.jimple.JimpleUtils;
import soot.Scene;
import soot.SootClass;
import soot.Type;

public class CrySLEntity {

	private CrySLRule rule;
	private CrySLCallFSM fsm;
	private CrySLVariablePool pool;
	
	private List<CrySLPredicate> ensuredPredicates;
	private List<CrySLPredicate> requiredPredicates;
	private List<CrySLPredicate> negatedPredicates;

	private Map<CrySLVariable, List<CrySLPredicate>> variableRequiredPredicateMap;
	private SootClass clazz;

	public CrySLEntity(CrySLRule rule) {
		this.clazz = Scene.v().getSootClass(rule.getClassName());
		this.rule = rule;
		this.pool = new CrySLVariablePool(rule);
		this.fsm = new CrySLCallFSM(this, pool);
		this.ensuredPredicates = Lists.newArrayList();
		this.requiredPredicates = Lists.newArrayList();
		this.negatedPredicates = Lists.newArrayList();
		this.variableRequiredPredicateMap = Maps.newHashMap();
		generateEnsuredPredicates();
	}
	
	private void generateEnsuredPredicates() {
		for (crypto.rules.CrySLPredicate predicate : rule.getPredicates()) {
			if (!predicate.isNegated()) {
				CrySLPredicate ensuredPredicate = new CrySLPredicate(predicate, this);
				ensuredPredicates.add(ensuredPredicate);
			} else {
				CrySLPredicate negatedPredicate = new CrySLPredicate(predicate, this);
				negatedPredicates.add(negatedPredicate);
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
	
	public List<CrySLPredicate> getRequiredPredicateForVariableByType(CrySLVariable variable) {	
		
		if (variableRequiredPredicateMap.get(variable) == null) {
			return Lists.newArrayList();
		} else {
			List<CrySLPredicate> firstChoice = Lists.newArrayList();
			List<CrySLPredicate> secondChoice = Lists.newArrayList();
			
			List<CrySLPredicate> calcPredicates = calcRequiredPredicatesBySatisfiedConstraints(variable);
			
			for (CrySLPredicate predicate : Lists.newArrayList(calcPredicates)) {
				Type firstPredicateParameterType = predicate.getPredicateParameters().get(0).getType();
								
				if(JimpleUtils.equals(variable.getType(), firstPredicateParameterType)) {
					firstChoice.add(predicate);
				} else if(JimpleUtils.isSubClassOrImplementer(variable.getType(),firstPredicateParameterType)) {
					secondChoice.add(predicate);
				} 
			}

			if(JimpleUtils.isHighestSuperCryptoInterfaceOrAbstractClass(variable.getType())) {
				return secondChoice;
			} else {
				if(!firstChoice.isEmpty()) {
					return firstChoice;
				} else if (!secondChoice.isEmpty()) {
					return secondChoice;
				} else {
					return Lists.newArrayList();
				}			
			}
		}
	}
	
	private List<CrySLPredicate> calcRequiredPredicatesBySatisfiedConstraints(CrySLVariable var){
		List<ISLConstraint> constraints = rule.getConstraints();
		List<crypto.rules.CrySLPredicate> predicateConstraints = Lists.newArrayList();
		
		for(ISLConstraint con : constraints) {
			if(con instanceof crypto.rules.CrySLPredicate) {
				crypto.rules.CrySLPredicate predCon = (crypto.rules.CrySLPredicate) con;
				if(predCon.getConstraint() instanceof CrySLValueConstraint) {
					predicateConstraints.add(predCon);
				}
			}
		}
		
		if(!predicateConstraints.isEmpty()) {		
			Set<crypto.rules.CrySLPredicate> satisfiedPreds = Sets.newHashSet();
			for(crypto.rules.CrySLPredicate pred : predicateConstraints) {
				CrySLValueConstraint valueCon = (CrySLValueConstraint) pred.getConstraint();
				CrySLVariable variable = pool.getVariableByName(valueCon.getVarName());				
				if(valueCon.getValueRange().contains(variable.getValue().toString().replace("\"", ""))) {
					satisfiedPreds.add(pred);
				}
			}		
			
			List<CrySLPredicate> predicates = variableRequiredPredicateMap.get(var);	
			List<CrySLPredicate> filteredPredicates = Lists.newArrayList();		

			if(!satisfiedPreds.isEmpty()) {
				for(CrySLPredicate predicate : predicates) {
					for(crypto.rules.CrySLPredicate predicate_ : satisfiedPreds) {
						if(predicate.getPredicate_().getPredName().equals(predicate_.getPredName())) {
							filteredPredicates.add(predicate);
						}
					}	
				}
				
				if(!filteredPredicates.isEmpty()) {
					return filteredPredicates;
				} 
			}
		}
			return Lists.newArrayList(variableRequiredPredicateMap.get(var));
	
	}
	
	//TODO: rename!
	public List<CrySLPredicate> canProducedByPredicates(){
		List<CrySLPredicate> producerPredicates= Lists.newArrayList();
		if(isInterfaceOrAbstract()) {
			List<CrySLPredicate> predicates = CrySLEntityPool.getInstance().getPredicateCandidatesWhichProduceType(clazz.getType());
			if (!predicates.isEmpty()) {
				producerPredicates = predicates;
			}
		}
		return producerPredicates;
	}

	public boolean isInterfaceOrAbstract() {
		if(clazz.isInterface()){
			return true;
		} else if(clazz.isAbstract()) {
			try {
				return JimpleUtils.getImplementingClassAndInitMethod(clazz) != null ? false : true;
			} catch (NoInterfaceImplementerException e) {
				return true;
			}
		} else {
			return false;
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
		
	public CrySLVariable getVariableByName(String name) {
		return pool.getVariableByName(name);
	}
	
	public List<CrySLPredicate> getNegatedPredicates() {
		return negatedPredicates;
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
