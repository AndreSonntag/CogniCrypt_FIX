package de.upb.cognicryptfix.generator.jimple;

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
		int i = 1;
		Local existingLocaL = JimpleUtils.getLocalByName(body,name);
		if (existingLocaL != null) {
			while (true) {
				Local tempLocal = JimpleUtils.getLocalByName(body,name + "#" + i);
				if (tempLocal != null) {
					i++;
				} else {
					name = name + "#" + i;
					break;
				}
			}
		}

		Local l = generateFreshLocal(type);
		if (!name.isEmpty())
			l.setName(name);
		return l;
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
		return generateFreshLocal(JimpleTypeGenerator.generateArrayType(type, dimension));
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
		//TODO: if type is already ArrayType
		return generateFreshLocal(JimpleTypeGenerator.generateArrayType(type, dimension), name);
	}

}
