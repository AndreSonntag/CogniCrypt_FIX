package de.upb.cognicryptfix.utils;

import java.util.Comparator;

import soot.SootMethod;

/**
 * @author Andre Sonntag
 * @date 05.02.2020
 */
public class InitializationMethodSorter implements Comparator<SootMethod> {

	@Override
	public int compare(SootMethod o1, SootMethod o2) {

		if (o1.isStatic() && o2.isStatic()) {
			if (o1.getParameterCount() < o2.getParameterCount()) {
				return -1;
			}
			if (o1.getParameterCount() > o2.getParameterCount()) {
				return 1;
			} else {
				return 0;
			}
		} else if (o1.isStatic() && !o2.isStatic()) {
			if (o1.getParameterCount() < o2.getParameterCount()) {
				return -1;
			}
			if (o1.getParameterCount() > o2.getParameterCount()) {
				return 1;
			} else {
				return -1;
			}
		} else if (!o1.isStatic() && o2.isStatic()) {
			if (o1.getParameterCount() < o2.getParameterCount()) {
				return -1;
			}
			if (o1.getParameterCount() > o2.getParameterCount()) {
				return 1;
			} else {
				return 1;
			}
		} else if (!o1.isStatic() && !o2.isStatic()) {
			if (o1.getParameterCount() < o2.getParameterCount()) {
				return -1;
			}
			if (o1.getParameterCount() > o2.getParameterCount()) {
				return 1;
			} else {
				return 0;
			}
		}
		return 0;
	}

}
