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
public class CrySLValueConstraintExtractor{
	
	private final CrySLValueConstraint constraint;
	private AnalysisSeedWithSpecification seed;

	
	public CrySLValueConstraintExtractor(AnalysisSeedWithSpecification seed, CrySLValueConstraint constraint) {
		this.constraint = constraint;
		this.seed = seed;
	}
	
	public ValueConstraint extract() {
		CrySLValueConstraint valCon = constraint;
		CrySLSplitter splitter = valCon.getVar().getSplitter();
		
		String usedValue = Utils.extractValueAsString(seed, valCon.getVar().getVarName()).keySet().iterator().next().toString();
		List<String> expectedValueList = valCon.getValueRange();
		
		if(splitter == null) {
			return new ValueConstraint(usedValue, expectedValueList, false);
		}else {	
			return new ValueConstraint(usedValue+splitter.getSplitter(), expectedValueList, true); 
		}		
	}
	
}
