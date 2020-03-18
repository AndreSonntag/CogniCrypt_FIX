package de.upb.cognicryptfix.crysl.fsm;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.upb.cognicryptfix.crysl.CrySLMethodCallCriteria;

/**
 * TODO: documentation
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLMethodCallState {

	private int state;
	private boolean initState;
	private boolean finalState;
	private Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> inTransitionMap;
	private Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> outTransitionMap;

	public CrySLMethodCallState(int state, boolean initState, boolean finalState) {
		super();
		this.state = state;
		this.initState = initState;
		this.finalState = finalState;
		this.inTransitionMap = Maps.newHashMap();
		this.outTransitionMap = Maps.newHashMap();
	}

	public void addInTransition(CrySLMethodCallTransition transition) {
		CrySLMethodCallCriteria criteria = transition.getCall().getCallCriteria();
		
		if (inTransitionMap.containsKey(criteria)){
			inTransitionMap.get(criteria).add(transition);
		} else {
			List<CrySLMethodCallTransition> initTransitions = Lists.newArrayList();
			initTransitions.add(transition);
			inTransitionMap.put(criteria,initTransitions);
		}
	}

	public void addOutTransition(CrySLMethodCallTransition transition) {
		CrySLMethodCallCriteria criteria = transition.getCall().getCallCriteria();
		
		if (outTransitionMap.containsKey(criteria)){
			outTransitionMap.get(criteria).add(transition);
		} else {
			List<CrySLMethodCallTransition> initTransitions = Lists.newArrayList();
			initTransitions.add(transition);
			outTransitionMap.put(criteria,initTransitions);
		}
	}
	
	public Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> getOutTransitions() {
		return outTransitionMap;
	}

	public Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> getInTransitions() {
		return inTransitionMap;
	}

	public int getState() {
		return state;
	}

	public boolean isInitState() {
		return initState;
	}

	public boolean isFinalState() {
		return finalState;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CrySLMethodCallState\n [state=");
		builder.append(state);
		builder.append(",\n initState=");
		builder.append(initState);
		builder.append(",\n finalState=");
		builder.append(finalState);
//		builder.append(", transitions=");
//		builder.append(transitions);
		builder.append("]");
		return builder.toString();
	}
}
