package de.upb.cognicryptfix.crysl;

import soot.ArrayType;
import soot.RefType;
import soot.Scene;
import soot.Type;
import soot.Value;
import soot.jimple.AssignStmt;

/**
 * @author Andre Sonntag
 */
public class CrySLVariable {
	
	private String name;
	private Type type;
	private Value value;
	
	public CrySLVariable(CrySLVariable copy) {
		this.name = copy.getName();
		this.type = copy.getType();
		this.value = copy.getValue();
	}

	public CrySLVariable(String name, Type type) {
		super();
		this.name = name;
		this.type = type;
	}
	
	public CrySLVariable(String name, Type type, Value value) {
		super();
		this.name = name;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
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
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		} else if (type.getNumber() != other.type.getNumber())
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CrySLVariable [name=");
		builder.append(name);
		builder.append(", type=");
		builder.append(type);
		builder.append("]");
		return builder.toString();
	}

	

	
}
