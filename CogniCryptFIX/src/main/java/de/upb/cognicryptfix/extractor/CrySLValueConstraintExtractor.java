package de.upb.cognicryptfix.extractor;

import java.util.List;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.rules.CrySLSplitter;
import crypto.rules.CrySLValueConstraint;
import de.upb.cognicryptfix.extractor.constraints.ValueConstraint;
import de.upb.cognicryptfix.utils.Utils;

/**
 * @author Andre Sonntag
 * @date 19.07.2019
 */
public class CrySLValueConstraintExtractor {

	private final CrySLValueConstraint violatedConstraint;
	private AnalysisSeedWithSpecification seed;

	public CrySLValueConstraintExtractor(AnalysisSeedWithSpecification seed, CrySLValueConstraint constraint) {
		this.violatedConstraint = constraint;
		this.seed = seed;
	}

	public ValueConstraint extract() {
		
		CrySLSplitter splitter = violatedConstraint.getVar().getSplitter();
		String usedValue = Utils.extractValueAsString(seed, violatedConstraint.getVar().getVarName()).keySet().iterator().next().toString();
		List<String> expectedValueList = violatedConstraint.getValueRange();

		if (splitter == null) {
			return new ValueConstraint(usedValue, expectedValueList, false);
		} else {
			return new ValueConstraint(usedValue + splitter.getSplitter(), expectedValueList, true);
		}
	}

	
//	public List<CrySLValueConstraint> extractCrySLValueConstraint(ISLConstraint con) {
//		List<CrySLValueConstraint> constraints = Lists.newArrayList();
//
//		if (con instanceof CrySLValueConstraint) {
//			constraints.add((CrySLValueConstraint) con);
//			return constraints;
//		} else if (con instanceof CrySLConstraint) {
//			CrySLConstraint crySLCon = (CrySLConstraint) con;
//
//			if (crySLCon.getLeft() instanceof CrySLValueConstraint) {
//				constraints.add((CrySLValueConstraint) crySLCon.getLeft());
//			} else if (crySLCon.getLeft() instanceof CrySLConstraint) {
//				constraints.addAll(extractCrySLValueConstraint(crySLCon.getLeft()));
//			}
//			if (crySLCon.getRight() instanceof CrySLValueConstraint) {
//				constraints.add((CrySLValueConstraint) crySLCon.getRight());
//			} else if (crySLCon.getRight() instanceof CrySLConstraint) {
//				constraints.addAll(extractCrySLValueConstraint(crySLCon.getRight()));
//			}
//			return constraints;
//		}
//
//		return constraints;
//	}

//	public List<CrySLConstraint> findAndBuildConstraint(List<ISLConstraint> constraints, ISLConstraint constraint) {
//
//				List<CrySLConstraint> buildedConstraints = Lists.newArrayList();
//				for (ISLConstraint con : new ArrayList<>(constraints)) {
//					if (con instanceof CrySLConstraint) {
//						CrySLConstraint crySLCon = (CrySLConstraint) con;
//
//						if (crySLCon.getLeft() == violatedConstraint) {
//							buildedConstraints.add(crySLCon);
//							System.out.println(crySLCon.toString());
//							constraints.remove(crySLCon);
//							buildedConstraints.addAll(findAndBuildConstraint(constraints, crySLCon.getRight()));
//
//						} else if (crySLCon.getRight() == violatedConstraint) {
//							buildedConstraints.add(crySLCon);
//							System.out.println(crySLCon.toString());
//							constraints.remove(crySLCon);
//							buildedConstraints.addAll(findAndBuildConstraint(constraints, crySLCon.getLeft()));
//						}
//					}
//
//				}
//				
//				for (ISLConstraint con : new ArrayList<>(constraints)) {		
//					if (con instanceof CrySLConstraint) {
//						CrySLConstraint crySLCon = (CrySLConstraint) con;
//						if (crySLCon.getLeft() == violatedConstraint) {
//							buildedConstraints.add(crySLCon);
//							System.out.println(crySLCon.toString());
//							constraints.remove(crySLCon);
//							buildedConstraints.addAll(findAndBuildConstraint(constraints, crySLCon.getRight()));
//
//						} else if (crySLCon.getRight() == violatedConstraint) {
//							buildedConstraints.add(crySLCon);
//							System.out.println(crySLCon.toString());
//							constraints.remove(crySLCon);
//							buildedConstraints.addAll(findAndBuildConstraint(constraints, crySLCon.getLeft()));
//						}
//					}
//				}	
//				return buildedConstraints;
//			}
}
