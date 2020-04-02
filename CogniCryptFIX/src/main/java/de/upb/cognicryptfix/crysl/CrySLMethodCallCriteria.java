package de.upb.cognicryptfix.crysl;

/**
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLMethodCallCriteria {

	private int requiredUserInteractions;
	private int requiredRefTypeGenerations;
	private int requiredPredicateGenerations;
	
	public CrySLMethodCallCriteria(int userInteractions, int refTypeGenerations) {
		super();
		this.requiredUserInteractions = userInteractions;
		this.requiredRefTypeGenerations = refTypeGenerations;
		this.requiredPredicateGenerations = 0;
	}

	public int getRequiredUserInteractions() {
		return requiredUserInteractions;
	}

	public void setRequiredUserInteractions(int requiredUserInteractions) {
		this.requiredUserInteractions = requiredUserInteractions;
	}

	public int getRequiredRefTypeGenerations() {
		return requiredRefTypeGenerations;
	}

	public void setRequiredRefTypeGenerations(int requiredRefTypeGenerations) {
		this.requiredRefTypeGenerations = requiredRefTypeGenerations;
	}

	public int getRequiredPredicateGenerations() {
		return requiredPredicateGenerations;
	}

	public void setRequiredPredicateGenerations(int requiredPredicateGenerations) {
		this.requiredPredicateGenerations = requiredPredicateGenerations;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + requiredPredicateGenerations;
		result = prime * result + requiredRefTypeGenerations;
		result = prime * result + requiredUserInteractions;
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
		CrySLMethodCallCriteria other = (CrySLMethodCallCriteria) obj;
		if (requiredPredicateGenerations != other.requiredPredicateGenerations)
			return false;
		if (requiredRefTypeGenerations != other.requiredRefTypeGenerations)
			return false;
		if (requiredUserInteractions != other.requiredUserInteractions)
			return false;
		return true;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CrySLMethodCallCriteria [requiredUserInteractions=");
		builder.append(requiredUserInteractions);
		builder.append(", requiredRefTypeGenerations=");
		builder.append(requiredRefTypeGenerations);
		builder.append(", requiredPredicateGenerations=");
		builder.append(requiredPredicateGenerations);
		builder.append("]");
		return builder.toString();
	}

}
