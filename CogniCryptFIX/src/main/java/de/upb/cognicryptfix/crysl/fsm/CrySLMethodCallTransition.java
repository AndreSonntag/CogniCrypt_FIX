package de.upb.cognicryptfix.crysl.fsm;

import de.upb.cognicryptfix.crysl.CrySLMethodCall;

/**
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLMethodCallTransition{

	private CrySLMethodCall call;
	private CrySLMethodCallState from;
	private CrySLMethodCallState to;
	
	public CrySLMethodCallTransition(CrySLMethodCall call, CrySLMethodCallState from, CrySLMethodCallState to) {
		this.call = call;
		this.from = from;
		this.to = to;
	}

	public CrySLMethodCallState from() {
		return this.from;
	}

	public CrySLMethodCallState to() {
		return this.to;
	}
	
	public CrySLMethodCall getCall() {
		return call;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CrySLMethodCallTransition\n [call=");
		builder.append(call);
		builder.append(",\n from=");
		builder.append(from.getState());
		builder.append(",\n to=");
		builder.append(to.getState());
		builder.append("]");
		return builder.toString();
	}
}
