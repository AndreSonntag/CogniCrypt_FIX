package de.upb.cognicryptfix.patcher;

import crypto.analysis.errors.AbstractError;
import soot.SootClass;

public interface IPatcher {
	SootClass getPatchedClass(AbstractError error);
}
