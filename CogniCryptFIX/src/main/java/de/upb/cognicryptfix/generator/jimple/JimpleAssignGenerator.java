package de.upb.cognicryptfix.generator.jimple;

import soot.ArrayType;
import soot.Local;
import soot.RefType;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IntConstant;
import soot.jimple.Jimple;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleAssignGenerator {

	public JimpleAssignGenerator() {}
	
	
	/**
	 * <p>
	 * Generates a {@link AssignStmt} as {@link Unit} object between two
	 * {@link Value} objects.
	 * </p>
	 * <p>
	 * i.e. // myVar = $r0;
	 * </p>
	 * 
	 * @param left  The left value
	 * @param right The right value
	 * @return Returns the new generated assignment statement.
	 */
	public Unit generateAssignStmt(Value left, Value right) {
		Unit assignStmt = Jimple.v().newAssignStmt(left, right);
		return assignStmt;
	}
	
	
	//FOR constructor calls
	public Unit generateVariableTypeAssignStmt(Local var) {
		Unit typeAssignStmt = Jimple.v().newAssignStmt(var, Jimple.v().newNewExpr((RefType)var.getType()));
		return typeAssignStmt;
	}
	
	//FOR array 
	public Unit generateArrayAssignStmt(Local var, int arrayDimension) {
		Type varType = var.getType();
		
		if(varType instanceof ArrayType) {
			ArrayType arrayType = (ArrayType) var.getType();
			varType = arrayType.baseType;
		}
		Unit arrayAssignStmt = Jimple.v().newAssignStmt(var, Jimple.v().newNewArrayExpr(varType, IntConstant.v(arrayDimension)));
		return arrayAssignStmt;
	}
	
	
}
