package de.upb.cognicryptfix.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crypto.interfaces.ISLConstraint;
import crypto.rules.CryptSLComparisonConstraint;
import crypto.rules.CryptSLConstraint;
import crypto.rules.CryptSLPredicate;
import crypto.rules.CryptSLValueConstraint;

/**
 * @author Andre Sonntag
 * @created 23.04.2019
 */
public class Utils {

	private static final Logger logger = LogManager.getLogger(Utils.class);

	public static MavenProject createAndCompile(String mavenProjectPath) {
		MavenProject mi = new MavenProject(mavenProjectPath);
		mi.compile();
		return mi;
	}
	
	public static String constraintToString(ISLConstraint constraint) {
		StringBuilder builder = new StringBuilder();
		
		if(constraint instanceof CryptSLConstraint) {
			CryptSLConstraint con = (CryptSLConstraint) constraint;
			builder.append("\n"+con.getClass().getSimpleName()+"[\n");
			builder.append("left = "+con.getLeft()+"\n");
			builder.append("right = "+con.getRight()+"\n");
			builder.append("op = "+con.getOperator()+"\n");

		}else if (constraint instanceof CryptSLValueConstraint) {
			CryptSLValueConstraint con = (CryptSLValueConstraint) constraint;
			builder.append("\n"+con.getClass().getSimpleName()+"[\n");
			builder.append(con.toString()+"]\n");

		}else if (constraint instanceof CryptSLPredicate) {
			CryptSLPredicate con = (CryptSLPredicate) constraint;
			builder.append("\n"+con.getClass().getSimpleName()+"[\n");
			builder.append(con.toString()+"]\n");

		}else if (constraint instanceof CryptSLComparisonConstraint) {
			CryptSLComparisonConstraint con = (CryptSLComparisonConstraint) constraint;
			builder.append("\n"+con.getClass().getSimpleName()+"[\n");
			builder.append(con.toString()+"]\n");
		}
		
		return builder.toString();
		
	}
}
