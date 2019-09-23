package de.upb.cognicryptfix.extractor;

import java.util.HashMap;
import java.util.List;
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
		
		String usedValue = Utils.extractValueAsString(seed, valCon.getVar().getVarName(), valCon).keySet().iterator().next().toString();
		List<String> expectedValueList = valCon.getValueRange();
		
		return new ValueConstraint(usedValue+splitter.getSplitter(), expectedValueList);
	}
	
//	private List<Entry<String, CallSiteWithExtractedValue>> getValFromVar(CryptSLObject var, ISLConstraint cons) {
//		final String varName = var.getVarName();
//		final Map<String, CallSiteWithExtractedValue> valueCollection = Utils.extractValueAsString(seed, varName, cons);
//		List<Entry<String, CallSiteWithExtractedValue>> vals = new ArrayList<>();
//		if (valueCollection.isEmpty()) {
//			return vals;
//		}
//		for (Entry<String, CallSiteWithExtractedValue> e : valueCollection.entrySet()) {
//			CryptSLSplitter splitter = var.getSplitter();
//			final CallSiteWithExtractedValue location = e.getValue();
//			String val = e.getKey();
//			if (splitter != null) {
//				int ind = splitter.getIndex();
//				String splitElement = splitter.getSplitter();
//				if (ind > 0) {
//					String[] splits = val.split(splitElement);
//					if (splits.length > ind) {
//						vals.add(new AbstractMap.SimpleEntry<>(splits[ind], location));
//					} else {
//						vals.add(new AbstractMap.SimpleEntry<>("", location));
//					}
//				} else {
//					vals.add(new AbstractMap.SimpleEntry<>(val.split(splitElement)[ind], location));
//				}
//			} else {
//				vals.add(new AbstractMap.SimpleEntry<>(val, location));
//			}
//		}
//		return vals;
//	}	
}
