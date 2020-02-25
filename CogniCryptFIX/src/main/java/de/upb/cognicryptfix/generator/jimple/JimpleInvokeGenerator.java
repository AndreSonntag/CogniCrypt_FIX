package de.upb.cognicryptfix.generator.jimple;

import java.util.Arrays;

import soot.Local;
import soot.SootMethod;
import soot.SootMethodRef;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleInvokeGenerator {

	
	public JimpleInvokeGenerator() {
	}
	
	/**
	 * <p>
	 * Generates a {@link InvokeStmt} as {@link Unit} object.
	 * </p>
	 * <p>
	 * i.e. specialinvoke $r0.&lt;javax.crypto.spec.PBEKeySpec: void
	 * &lt;init&gt;(char[])&gt;();
	 * </p>
	 * 
	 * @param invokingVar The variable that executes the method call.
	 * @param method The method to be called.
	 * @param args   Arguments for the method call.
	 * @return Returns the new generated invocation statement.
	 */
	public Unit generateInvokeStmt(Local invokingVar, SootMethod method, Value... args) {
		SootMethodRef ref = method.makeRef();
		InvokeExpr invokeExpr = null;

		if (method.isStatic()) {
			invokeExpr = Jimple.v().newStaticInvokeExpr(ref, Arrays.asList(args));
		} else if (method.isConstructor()) {
			invokeExpr = Jimple.v().newSpecialInvokeExpr(invokingVar, ref, Arrays.asList(args));
		} else {
			invokeExpr = Jimple.v().newVirtualInvokeExpr(invokingVar, ref, Arrays.asList(args));
		}
		
		//TODO: dynamic invokes ??

		InvokeStmt invoke = Jimple.v().newInvokeStmt(invokeExpr);
		return invoke;
	}

}
