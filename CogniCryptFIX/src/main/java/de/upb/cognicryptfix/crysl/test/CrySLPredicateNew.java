package de.upb.cognicryptfix.crysl.test;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import crypto.interfaces.ICrySLPredicateParameter;
import crypto.rules.CrySLCondPredicate;
import crypto.rules.CrySLObject;
import crypto.rules.StateNode;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.utils.Utils;
import soot.Scene;
import soot.Type;

public class CrySLPredicateNew {

//	private crypto.rules.CrySLPredicate predicate_;
//	private String predicateName;
//	private List<CrySLVariable> predicateParameters;
//	private List<CrySLMethodCall> generationPath;
//	private CrySLEntityNew producer;
//
//	public CrySLPredicateNew(crypto.rules.CrySLPredicate predicate_, CrySLEntityNew producer) {
//		this.predicate_ = predicate_;
//		this.producer = producer;
//		this.predicateName = predicate_.getPredName();
//		this.predicateParameters = extractPredicateParameters();
//		this.generationPath = Lists.newLinkedList();
//	}
//
//	public int countRequiredParameterForPath() {
//		if(generationPath.isEmpty()) {
//			generationPath = getPath();
//		}	
//		
//		int requiredPredicates = 0;
//		for(CrySLMethodCall call : generationPath) {
//				for(CrySLVariable parameter : call.getCallParameters()) {
//					if(parameter.getRequiredPredicate() != null) {
//						requiredPredicates++;
//					}
//				}
//		}
//		return requiredPredicates;
//	}
//	
//	public Map<CrySLVariable, List<CrySLPredicateNew>> requiredPredicatesForPath(){
//		if(generationPath.isEmpty()) {
//			generationPath = getPath();
//		}
//		
//		Map<CrySLVariable, List<CrySLPredicateNew>> variableRequiredPredicateMap = Maps.newHashMap();
//		
//		for(CrySLMethodCall call : generationPath) {
//			for(CrySLVariable parameter : call.getCallParameters()) {
//				if(parameter.getRequiredPredicate()) {
//					variableRequiredPredicateMap.put(parameter, producer.getRequiredPredicateForVariable(parameter));
//				}
//			}
//		}
//		
//		
//		return variableRequiredPredicateMap;	
//	}
//
//	public List<CrySLMethodCall> getPath() {
//		if (generationPath.isEmpty()) {
//			List<LinkedList<CrySLMethodCall>> possiblePaths = calcPaths();
//			generationPath = calcPaths().get(0);
//		}
//		return generationPath;
//	}
//
//	private List<LinkedList<CrySLMethodCall>> calcPaths() {
//		if (predicate_ instanceof CrySLCondPredicate) {
//			CrySLCondPredicate conPred = (CrySLCondPredicate) predicate_;
//			Set<StateNode> afterStates = conPred.getConditionalMethods();
//			List<LinkedList<CrySLMethodCall>> paths = producer.getFSM()
//					.calcBestPathsForPredicateGenerationFromState(afterStates.iterator().next(), predicateParameters);
//			return paths;
//		} else {
//			List<LinkedList<CrySLMethodCall>> paths = producer.getFSM()
//					.calcBestPathsForPredicateGenerationFromFinalStates(predicateParameters);
//			return paths;
//		}
//	}
//	
//	//TODO: replace by variable pool ??
//	private List<CrySLVariable> extractPredicateParameters() {
//		List<CrySLVariable> predicateParameters = Lists.newArrayList();
//		List<ICrySLPredicateParameter> parameters = predicate_.getParameters();
//		for (ICrySLPredicateParameter parameter : parameters) {
//			CrySLObject predicateParameter = (CrySLObject) parameter;
//			String varName = predicateParameter.getVarName();
//			Type varType = varName.equals("this") ? Scene.v().getType(producer.getRule().getClassName()) : Utils.getType(producer.getRule(), varName);
//			predicateParameters.add(new CrySLVariable(varName, varType));
//		}
//		return predicateParameters;
//	}
//
//	public CrySLEntity getProducer() {
//		return producer;
//	}
//
//	public crypto.rules.CrySLPredicate getPredicate_() {
//		return predicate_;
//	}
//
//	public String getPredicateName() {
//		return predicateName;
//	}
//
//	public List<CrySLVariable> getPredicateParameters() {
//		return predicateParameters;
//	}
//
//	@Override
//	public String toString() {
//		StringBuilder builder = new StringBuilder();
//		builder.append("CrySLPredicate [");
//		builder.append("\n producer=");
//		builder.append(producer.getRule().getClassName());
//		builder.append(",\n predicateName=");
//		builder.append(predicateName);
//		builder.append(",\n predicateParameters=");
//		builder.append(predicateParameters);
//		builder.append(" \n]");
//		return builder.toString();
//	}

	
}
