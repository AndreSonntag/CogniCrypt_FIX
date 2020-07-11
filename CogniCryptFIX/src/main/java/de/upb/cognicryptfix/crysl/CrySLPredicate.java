package de.upb.cognicryptfix.crysl;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;

import crypto.interfaces.ICrySLPredicateParameter;
import crypto.rules.CrySLCondPredicate;
import crypto.rules.CrySLObject;
import crypto.rules.StateNode;
import de.upb.cognicryptfix.exception.path.EmptyPathListException;
import de.upb.cognicryptfix.exception.path.PathException;

public class CrySLPredicate {

	private static final Logger LOGGER = LogManager.getLogger(CrySLPredicate.class);

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

	public List<LinkedList<CrySLMethodCall>> getPaths() throws PathException {
		if (generationPaths.isEmpty()) {
			generationPaths = calcPaths();
		}

		return generationPaths;
	}

	private List<LinkedList<CrySLMethodCall>> calcPaths() throws PathException{
		List<LinkedList<CrySLMethodCall>> paths = Lists.newArrayList();

		try {
			if (predicate_ instanceof CrySLCondPredicate) {
				CrySLCondPredicate conPred = (CrySLCondPredicate) predicate_;
				Set<StateNode> afterStates = conPred.getConditionalMethods();
				paths = producer.getFSM().calcPathsForPredicateGenerationFromState(afterStates.iterator().next(),
						predicateParameters);
			} else {
				paths = producer.getFSM().calcPathsForPredicateGenerationFromFinalStates(predicateParameters);
			}
		} catch (EmptyPathListException e) {
			LOGGER.error(producer.getRule().getClassName()+": all calculated generation paths for "+predicateName+" are not valid!");
		}

		return paths;
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
		builder.append(predicateParameters + "]");
		return builder.toString();
	}
}
