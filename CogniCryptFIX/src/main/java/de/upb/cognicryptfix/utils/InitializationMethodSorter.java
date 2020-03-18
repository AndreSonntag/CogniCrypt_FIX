package de.upb.cognicryptfix.utils;

import java.util.Comparator;
import java.util.List;

import soot.SootClass;
import soot.SootMethod;
import soot.Type;

/**
 * @author Andre Sonntag
 * @date 05.02.2020
 */
public class InitializationMethodSorter implements Comparator<SootMethod> {

	/*
	 * -1 return o1 1 return o2 0 both o1
	 */

	@Override
	public int compare(SootMethod method1, SootMethod method2) {

		Type method1DeclaringClassType = method1.getDeclaringClass().getType();
		Type method2DeclaringClassType = method2.getDeclaringClass().getType();
		Type method1SuperClassType = method1.getDeclaringClass().getSuperclass().getType();
		Type method2SuperClassType = method2.getDeclaringClass().getSuperclass().getType();
		List<Type> method1ParameterTypes = method1.getParameterTypes();
		List<Type> method2ParameterTypes = method2.getParameterTypes();

		if ((method1ParameterTypes.contains(method1DeclaringClassType)
				|| method1ParameterTypes.contains(method1SuperClassType))
						&& (method2ParameterTypes.contains(method2DeclaringClassType)
				|| method2ParameterTypes.contains(method2SuperClassType))) {
			
			if (method1.isStatic() && method2.isStatic()) {
				if (method1.getParameterCount() < method2.getParameterCount()) {
					return -1;
				} else if (method1.getParameterCount() > method2.getParameterCount()) {
					return 1;
				} else {
					return -1;
				}
			} else if (method1.isStatic() && !method2.isStatic()) {
				if (method1.getParameterCount() < method2.getParameterCount()) {
					return -1;
				} else if (method1.getParameterCount() > method2.getParameterCount()) {
					return 1;
				} else {
					return -1;
				}
			} else if (!method1.isStatic() && method2.isStatic()) {
				if (method1.getParameterCount() < method2.getParameterCount()) {
					return -1;
				} else if (method1.getParameterCount() > method2.getParameterCount()) {
					return 1;
				} else {
					return 1;
				}
			} else {
				if (method1.getParameterCount() < method2.getParameterCount()) {
					return -1;
				} else if (method1.getParameterCount() > method2.getParameterCount()) {
					return 1;
				} else {
					return -1;
				}
			}
		} else if (!(method1ParameterTypes.contains(method1DeclaringClassType)
				|| method1ParameterTypes.contains(method1SuperClassType))
				&& (method2ParameterTypes.contains(method2DeclaringClassType)
		|| method2ParameterTypes.contains(method2SuperClassType))) {
				
			return -1;		
		} else if ((method1ParameterTypes.contains(method1DeclaringClassType)
				|| method1ParameterTypes.contains(method1SuperClassType))
				&& !(method2ParameterTypes.contains(method2DeclaringClassType)
		|| method2ParameterTypes.contains(method2SuperClassType))) {
				
			return 1;		
		} else {
			if (method1.isStatic() && method2.isStatic()) {
				if (method1.getParameterCount() < method2.getParameterCount()) {
					return -1;
				} else if (method1.getParameterCount() > method2.getParameterCount()) {
					return 1;
				} else {
					return -1;
				}
			} else if (method1.isStatic() && !method2.isStatic()) {
				if (method1.getParameterCount() < method2.getParameterCount()) {
					return -1;
				} else if (method1.getParameterCount() > method2.getParameterCount()) {
					return 1;
				} else {
					return -1;
				}
			} else if (!method1.isStatic() && method2.isStatic()) {
				if (method1.getParameterCount() < method2.getParameterCount()) {
					return -1;
				} else if (method1.getParameterCount() > method2.getParameterCount()) {
					return 1;
				} else {
					return 1;
				}
			} else {
				if (method1.getParameterCount() < method2.getParameterCount()) {
					return -1;
				} else if (method1.getParameterCount() > method2.getParameterCount()) {
					return 1;
				} else {
					return -1;
				}
			}
		}
	}

}
