package de.upb.cognicryptfix.generator.jimple;

import soot.ArrayType;
import soot.Body;
import soot.Local;
import soot.Type;
import soot.javaToJimple.LocalGenerator;

/**
 * @author Andre Sonntag
 * @date 19.02.2020
 */
public class JimpleLocalGenerator {

	private Body body;

	public JimpleLocalGenerator(Body body) {
		this.body = body;
	}

	/**
	 * <p>
	 * Generates a fresh {@link Local} variable.
	 * </p>
	 * 
	 * @param body The body in which the variable is generated.
	 * @param type Type of the variable.
	 * @return Returns a fresh generated {@link Local} variable.
	 */
	public Local generateFreshLocal(Type type) {
		LocalGenerator lg = new LocalGenerator(body);

		return lg.generateLocal(type);
	}

	/**
	 * <p>
	 * Generates a fresh {@link Local} variable with a name.
	 * </p>
	 * 
	 * @param body The body in which the variable is generated.
	 * @param type Type of the variable.
	 * @param name Name of the variable.
	 * @return Returns a fresh generated {@link Local} variable.
	 */
	public Local generateFreshLocal(Type type, String name) {

		StringBuilder sb = new StringBuilder(name);
		Local existingLocaL = JimpleUtils.getLocalByName(body, name);

		if (existingLocaL != null) {
			name = findName(name);
		}

		Local l = generateFreshLocal(type);
		if (!name.isEmpty())
			l.setName(name);
		return l;

	}

	private String findName(String name) {
		StringBuilder sb = new StringBuilder(name);
		Local existingLocaL = JimpleUtils.getLocalByName(body, name);
		
		if (existingLocaL != null) {
			if (Character.isDigit(name.charAt(name.length() - 1))) {
				int firstNumber = Integer.parseInt(name.charAt(name.length() - 1) + "");
				if (firstNumber == 9) {
					if (Character.isDigit(name.charAt(name.length() - 2))) {
						int secondNumber = Integer.parseInt(name.charAt(name.length() - 2) + "");
						sb.replace(name.length() - 2, name.length() - 2, secondNumber + 1 + "");
						sb.replace(name.length() - 1, name.length() - 1, 0 + "");
						name = sb.toString();
					} else {
						sb.replace(name.length() - 1, name.length() - 1, "10");
						name = sb.toString();
					}
				} else {
					sb.replace(name.length()-1, name.length(), firstNumber + 1 + "");
					name = sb.toString();
				}
			} else {
				name = findName(name+1);
			}
		}
		
		if(JimpleUtils.getLocalByName(body, name) != null) {
			name = findName(name);
		} 
		return name;
	}

	/**
	 * <p>
	 * Generates a fresh {@link Local} Array variable.
	 * </p>
	 * 
	 * @param body      The body in which the variable is generated.
	 * @param type      Type of the variable.
	 * @param dimension Dimension of the array.
	 * @return Returns a fresh generated {@link Local} array variable.
	 */
	public Local generateFreshArrayLocal(Type type, int dimension) {
		if (type instanceof ArrayType) {
			return generateFreshLocal(type);
		} else {
			return generateFreshLocal(JimpleTypeGenerator.generateArrayType(type, dimension));
		}
	}

	/**
	 * <p>
	 * Generates a fresh {@link Local} Array variable with a name.
	 * </p>
	 * 
	 * @param body      The body in which the variable is generated.
	 * @param type      Type of the variable.
	 * @param dimension Dimension of the array.
	 * @param name      Name of the variable.
	 * @return Returns a fresh generated {@link Local} Array variable.
	 */
	public Local genereateFreshArrayLocal(Type type, String name, int dimension) {

		if (type instanceof ArrayType) {
			return generateFreshLocal(type, name);
		} else {
			return generateFreshLocal(JimpleTypeGenerator.generateArrayType(type, dimension), name);
		}
	}

}
