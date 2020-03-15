package de.upb.cognicryptfix.crysl;

import soot.ArrayType;
import soot.Type;
import soot.Value;

/**
 * TODO: documentation
 * @author Andre Sonntag
 *
 */
public class CrySLVariable {
	
	private String variable;
	private Type type;
	private Value value;
	
	public CrySLVariable(String variable, Type type) {
		super();
		this.variable = variable;
		this.type = type;
	}
	
	public CrySLVariable(String variable, Type type, Value value) {
		super();
		this.variable = variable;
		this.type = type;
		this.value = value;
	}
	
	public boolean isArrayVariable() {
		if(type instanceof ArrayType) {
			return true;
		} else {
			return false;
		}
	}

	public String getVariable() {
		return variable;
	}

	public void setVariable(String variable) {
		this.variable = variable;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}
	
	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
	}
	
	

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((variable == null) ? 0 : variable.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CrySLVariable other = (CrySLVariable) obj;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (type != other.type)
			return false;
		if (variable == null) {
			if (other.variable != null)
				return false;
		} else if (!variable.equals(other.variable))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CrySLVariable [variable=");
		builder.append(variable);
		builder.append(", type=");
		builder.append(type);
		builder.append("]");
		return builder.toString();
	}

	

	
}
