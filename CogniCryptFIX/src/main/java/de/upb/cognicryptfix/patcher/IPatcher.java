package de.upb.cognicryptfix.patcher;

import crypto.analysis.errors.AbstractError;
import de.upb.cognicryptfix.exception.patch.RepairException;
import soot.SootClass;

public interface IPatcher {
	SootClass getPatchedClass(AbstractError error) throws RepairException;
}
