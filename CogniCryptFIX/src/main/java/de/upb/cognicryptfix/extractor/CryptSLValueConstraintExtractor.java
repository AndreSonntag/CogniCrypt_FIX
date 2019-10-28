package de.upb.cognicryptfix.extractor;

import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import crypto.analysis.AnalysisSeedWithSpecification;
import crypto.rules.CryptSLSplitter;
import crypto.rules.CryptSLValueConstraint;
import de.upb.cognicryptfix.extractor.constraints.ValueConstraint;
import de.upb.cognicryptfix.utils.Utils;

/**
 * @author Andre Sonntag
 * @date 19.07.2019
 */
public class CryptSLValueConstraintExtractor{
	
	private final CryptSLValueConstraint constraint;
	private AnalysisSeedWithSpecification seed;

	
	public CryptSLValueConstraintExtractor(AnalysisSeedWithSpecification seed, CryptSLValueConstraint constraint) {
		this.constraint = constraint;
		this.seed = seed;
	}
	
	public ValueConstraint extract() {
		CryptSLValueConstraint valCon = constraint;
		CryptSLSplitter splitter = valCon.getVar().getSplitter();
		
		String usedValue = Utils.extractValueAsString(seed, valCon.getVar().getVarName()).keySet().iterator().next().toString();
		List<String> expectedValueList = valCon.getValueRange();
		
		if(splitter == null) {
			return new ValueConstraint(usedValue, expectedValueList, false);
		}else {	
			return new ValueConstraint(usedValue+splitter.getSplitter(), expectedValueList, true); 
		}		
	}
	
}
