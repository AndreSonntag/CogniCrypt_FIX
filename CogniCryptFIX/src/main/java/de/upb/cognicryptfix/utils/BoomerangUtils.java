package de.upb.cognicryptfix.utils;

import java.util.ArrayList;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.callgraph.ObservableICFG;
import boomerang.jimple.AllocVal;
import boomerang.jimple.Statement;
import boomerang.jimple.Val;
import boomerang.results.BackwardBoomerangResults;
import boomerang.seedfactory.SeedFactory;
import crypto.boomerang.CogniCryptIntAndStringBoomerangOptions;
import crypto.extractparameter.ExtractedValue;
import soot.Local;
import soot.SootMethod;
import soot.Unit;
import wpds.impl.Weight.NoWeight;

public class BoomerangUtils {

	
	public static ArrayList<ExtractedValue> bommerangPointsToAnalysis(ObservableICFG<Unit, SootMethod> observableDynamicICFG, Local local, Statement statment, Unit unit) {
		Boomerang solver = new Boomerang(new CogniCryptIntAndStringBoomerangOptions() {
			@Override
			public boolean onTheFlyCallGraph() {
				return false;
			};
		}) {
			@Override
			public ObservableICFG<Unit, SootMethod> icfg() {
				return observableDynamicICFG;
			}

			@Override
			public SeedFactory<NoWeight> getSeedFactory() {
				return null;
			}
		};

		Val queryVal = new Val(local, statment.getMethod());
		BackwardQuery query = new BackwardQuery(statment,queryVal);
		BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);

		ArrayList<ExtractedValue> evList = new ArrayList<>();
		ExtractedValue ev = null;
		for (ForwardQuery v : backwardQueryResults.getAllocationSites().keySet()) {
			if(v.var() instanceof AllocVal) {
				AllocVal allocVal = (AllocVal) v.var();
				ev = new ExtractedValue(allocVal.allocationStatement(),allocVal.allocationValue(), backwardQueryResults.getDataFlowPath(v));
			evList.add(ev);
			} 
		}
		return evList;
	}	
	
}
