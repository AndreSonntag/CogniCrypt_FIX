package de.upb.cognicryptfix.generator.jimple;

import soot.ArrayType;
import soot.Type;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleTypeGenerator {

	/**
	 * <p>
	 * Converts a {@link Type} to an Array type
	 * </p>
	 * 
	 * @param type      The type of the Array. i.e. char
	 * @param dimension Dimension of the Array.
	 * @return Returns Array type.
	 */
	public static Type generateArrayType(Type type, int dimension) {
		Type parameterArray = ArrayType.v(type, dimension);
		return parameterArray;
	}
}
