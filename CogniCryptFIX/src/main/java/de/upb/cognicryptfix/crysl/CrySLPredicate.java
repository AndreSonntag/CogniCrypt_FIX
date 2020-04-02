package de.upb.cognicryptfix.crysl;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;

import crypto.interfaces.ICrySLPredicateParameter;
import crypto.rules.CrySLCondPredicate;
import crypto.rules.CrySLObject;
import crypto.rules.StateNode;
import de.upb.cognicryptfix.utils.Utils;
import soot.Scene;
import soot.Type;

public class CrySLPredicate {

	private crypto.rules.CrySLPredicate predicate_;
	private String predicateName;
	private List<CrySLVariable> predicateParameters;
	private List<LinkedList<CrySLMethodCall>> generationPaths;
	private CrySLEntity producer;
	private boolean negated;

	public CrySLPredicate(crypto.rules.CrySLPredicate predicate_, CrySLEntity producer) {
		this.predicate_ = predicate_;
		this.producer = producer;
		this.predicateName = predicate_.getPredName();
		this.predicateParameters = extractPredicateParameters();
		this.generationPaths = Lists.newLinkedList();
		this.negated = predicate_.isNegated(); 
	}

	public List<LinkedList<CrySLMethodCall>> getPaths(){
		if (generationPaths.isEmpty()) {
			generationPaths = calcPaths();
		}
		
		return generationPaths;
	}

	private List<LinkedList<CrySLMethodCall>> calcPaths() {
		if (predicate_ instanceof CrySLCondPredicate) {
			CrySLCondPredicate conPred = (CrySLCondPredicate) predicate_;
			Set<StateNode> afterStates = conPred.getConditionalMethods();
			List<LinkedList<CrySLMethodCall>> paths = producer.getFSM()
					.calcBestPathsForPredicateGenerationFromState(afterStates.iterator().next(), predicateParameters);
			return paths;
		} else {
			List<LinkedList<CrySLMethodCall>> paths = producer.getFSM()
					.calcBestPathsForPredicateGenerationFromFinalStates(predicateParameters);
			return paths;
		}
	}

	private List<CrySLVariable> extractPredicateParameters() {
		List<CrySLVariable> predicateParameters = Lists.newArrayList();
		List<ICrySLPredicateParameter> parameters = predicate_.getParameters();
		for (ICrySLPredicateParameter parameter : parameters) {
			CrySLObject predicateParameter = (CrySLObject) parameter;
			String varName = predicateParameter.getVarName();
			predicateParameters.add(producer.getVariableByName(varName));
		}
		return predicateParameters;
	}

	public CrySLEntity getProducer() {
		return producer;
	}

	public crypto.rules.CrySLPredicate getPredicate_() {
		return predicate_;
	}

	public String getPredicateName() {
		return predicateName;
	}

	public List<CrySLVariable> getPredicateParameters() {
		return predicateParameters;
	}

	public boolean isNegated() {
		return negated;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CrySLPredicate [");
		builder.append("\n producer=");
		builder.append(producer.getRule().getClassName());
		builder.append(",\n predicateName=");
		builder.append(predicateName);
		builder.append(",\n predicateParameters=");
		builder.append(predicateParameters+"]");
		return builder.toString();
	}
}
