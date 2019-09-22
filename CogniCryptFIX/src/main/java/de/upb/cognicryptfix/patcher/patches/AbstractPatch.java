package de.upb.cognicryptfix.patcher.patches;

import soot.Body;

public abstract class AbstractPatch {
	

	public abstract Body getPatch();
	public abstract String toString();

}
