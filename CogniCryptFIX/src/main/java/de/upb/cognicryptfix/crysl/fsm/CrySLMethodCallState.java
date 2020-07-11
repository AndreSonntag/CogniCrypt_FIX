package de.upb.cognicryptfix.crysl.fsm;

import java.util.List;

import com.google.common.collect.Lists;

/**
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLMethodCallState {

	private int state;
	private boolean initState;
	private boolean finalState;
	private List<CrySLMethodCallTransition> inTransition;
	private List<CrySLMethodCallTransition> outTransition;

	public CrySLMethodCallState(int state, boolean initState, boolean finalState) {
		super();
		this.state = state;
		this.initState = initState;
		this.finalState = finalState;
		this.inTransition = Lists.newArrayList();
		this.outTransition = Lists.newArrayList();
	}

	public void addInTransition(CrySLMethodCallTransition transition) {
		inTransition.add(transition);
	}

	public void addOutTransition(CrySLMethodCallTransition transition) {
		outTransition.add(transition);
	}
	
	public List<CrySLMethodCallTransition> getInTransition() {
		return inTransition;
	}

	public List<CrySLMethodCallTransition> getOutTransition() {
		return outTransition;
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
