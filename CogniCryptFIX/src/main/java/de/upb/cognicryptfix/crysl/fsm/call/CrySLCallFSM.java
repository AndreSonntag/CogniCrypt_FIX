package de.upb.cognicryptfix.crysl.fsm.call;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import crypto.rules.CrySLMethod;
import crypto.rules.CrySLRule;
import crypto.rules.StateMachineGraph;
import crypto.rules.StateNode;
import crypto.rules.TransitionEdge;
import crypto.typestate.CrySLMethodToSootMethod;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLMethodCallCriteria;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.CrySLVariableConstraintPool;
import de.upb.cognicryptfix.utils.Pair;
import soot.SootMethod;

/**
 * TODO: documentation
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLCallFSM {

	private CrySLRule rule;
	private CrySLVariableConstraintPool pool;
	private StateMachineGraph smg;
	private CrySLMethodCallState startState;
	private Set<CrySLMethodCallState> finalStates;
	private List<CrySLMethodCallTransition> transitions;
	private List<CrySLMethodCallState> states;
	
	private HashMap<StateNode, CrySLMethodCallState> wrapperStateMap;
	private HashMap<SootMethod, Pair<CrySLMethodCallState, CrySLMethodCallState>> methodFromToStateMap;

	public CrySLCallFSM(CrySLRule rule, CrySLVariableConstraintPool pool) {
		this.rule = rule;
		this.pool = pool;
		this.smg = rule.getUsagePattern();
		this.wrapperStateMap = Maps.newLinkedHashMap();
		this.methodFromToStateMap = Maps.newHashMap();
		this.transitions = Lists.newArrayList();
		this.states = Lists.newArrayList();
		this.finalStates = Sets.newHashSet();
		createFSM();
	}

	private void createFSM() {

		List<TransitionEdge> edges_ = smg.getEdges();
		List<StateNode> states_ = new ArrayList<>(smg.getNodes());

		for (StateNode state : states_) {
			boolean initState = state == smg.getInitialTransition().getLeft() ? true : false;
			boolean finalState = smg.getAcceptingStates().contains(state) ? true : false;
			CrySLMethodCallState callState = new CrySLMethodCallState(Integer.parseInt(state.getName()), initState, finalState);
			wrapperStateMap.put(state, callState);
			states.add(callState);

			if (initState) {
				startState = callState;
			}

			if (finalState) {
				finalStates.add(callState);
			}
		}

		for (TransitionEdge edge : edges_) {
			List<CrySLMethodCall> calls = createCrySLMethodCall(edge, smg.getInitialTransition() == edge ? true : false);
			for (CrySLMethodCall call : calls) {
				CrySLMethodCallState from = wrapperStateMap.get(edge.getLeft());
				CrySLMethodCallState to = wrapperStateMap.get(edge.getRight());

				if (from.getState() != to.getState()) {
					CrySLMethodCallTransition transition = new CrySLMethodCallTransition(call, from, to);

					transitions.add(transition);
					from.addOutTransition(transition);
					to.addInTransition(transition);
					methodFromToStateMap.put(call.getSootMethod(),
							new Pair<CrySLMethodCallState, CrySLMethodCallState>(from, to));
				}
			}
		}
	}
	
	private List<CrySLMethodCall> createCrySLMethodCall(TransitionEdge edge, boolean initCall) {
		List<CrySLMethodCall> calls = Lists.newArrayList();
		for (CrySLMethod crySLMethod : edge.getLabel()) {
			List<SootMethod> sootMethods = Lists.newArrayList(CrySLMethodToSootMethod.v().convert(crySLMethod));
			if (!sootMethods.isEmpty()) {
				calls.add(new CrySLMethodCall(rule, sootMethods.get(0), crySLMethod, initCall, pool));
			}
		}
		return calls;
	}

	public List<LinkedList<CrySLMethodCall>> calcShortesPathsForPredicateFromState(StateNode state, List<CrySLVariable> predicatePramameters) {
		List<LinkedList<CrySLMethodCall>> calcBackwardPaths = Lists.newArrayList();
		List<LinkedList<CrySLMethodCall>> calcForwardPaths = Lists.newArrayList();
		List<LinkedList<CrySLMethodCall>> paths = Lists.newArrayList();

		CrySLMethodCallState currentState = wrapperStateMap.get(state);
		calcBackwardPaths = calcPathsBackward(currentState, Lists.newArrayList());

		for (LinkedList<CrySLMethodCall> p : calcBackwardPaths) {
			Collections.reverse(p);
		}

		if (!currentState.isFinalState()) {
			calcForwardPaths = calcPathsForward(currentState);
			for (LinkedList<CrySLMethodCall> bPath : calcBackwardPaths) {
				for (LinkedList<CrySLMethodCall> fPath : calcForwardPaths) {
					LinkedList<CrySLMethodCall> connectedPath = Lists.newLinkedList(bPath);
					connectedPath.addAll(fPath);
					paths.add(new LinkedList<CrySLMethodCall>(connectedPath));
				}
			}
		} else {
			paths = calcBackwardPaths;
		}
		
		paths = CrySLPathFilter.filterCallPathsByUsedPredicateParameters(paths, predicatePramameters);
		paths = CrySLPathFilter.filterCallPathsByFewestUserInteractions(paths);
		paths = CrySLPathFilter.filterCallPathsByFewestRefTypeGeneration(paths);
		paths = CrySLPathFilter.filterCallPathsByFewestCalls(paths);
		
		return paths;
	}

	public List<LinkedList<CrySLMethodCall>> calcShortesPathsForPredicate(List<CrySLVariable> predicatePramameters) {
		List<LinkedList<CrySLMethodCall>> calcBackwardPaths = Lists.newArrayList();

		for (CrySLMethodCallState finalState : finalStates) {
			calcBackwardPaths.addAll(calcPathsBackward(finalState, Lists.newArrayList()));
		}
		for (LinkedList<CrySLMethodCall> p : calcBackwardPaths) {
			Collections.reverse(p);
		}
		
		calcBackwardPaths = CrySLPathFilter.filterCallPathsByUsedPredicateParameters(calcBackwardPaths, predicatePramameters);
		calcBackwardPaths = CrySLPathFilter.filterCallPathsByFewestUserInteractions(calcBackwardPaths);
		calcBackwardPaths = CrySLPathFilter.filterCallPathsByFewestRefTypeGeneration(calcBackwardPaths);
		calcBackwardPaths = CrySLPathFilter.filterCallPathsByFewestCalls(calcBackwardPaths);

		return calcBackwardPaths;
	}

	private ArrayList<LinkedList<CrySLMethodCall>> calcPathsBackward(CrySLMethodCallState state,
			List<CrySLMethodCallState> visitedStates) {
		visitedStates.add(state);

		ArrayList<LinkedList<CrySLMethodCall>> calcPaths = Lists.newArrayList();
		Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitionMap = Maps.newHashMap(state.getInTransitions());

		for (List<CrySLMethodCallTransition> transitionList : transitionMap.values()) {
			LinkedList<CrySLMethodCall> path;

			for (CrySLMethodCallTransition t : transitionList) {

				if (t.from().isInitState()) {
					path = Lists.newLinkedList();
					path.add(t.getCall());
					calcPaths.add(path);
				} else {

					List<CrySLMethodCallState> tempVisitedStates = Lists.newArrayList(visitedStates);
					if (visitedStates.contains(t.from())) {
						break;
					} else {
						tempVisitedStates.addAll(visitedStates);
					}

					ArrayList<LinkedList<CrySLMethodCall>> subPath = calcPathsBackward(t.from(), tempVisitedStates);
					for (LinkedList<CrySLMethodCall> t1 : subPath) {
						path = Lists.newLinkedList();
						path.add(t.getCall());
						path.addAll(t1);
						calcPaths.add(path);
					}
				}
			}

		}
		return calcPaths;
	}

	private ArrayList<LinkedList<CrySLMethodCall>> calcPathsForward(CrySLMethodCallState state) {

		ArrayList<LinkedList<CrySLMethodCall>> calcPaths = Lists.newArrayList();
		Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitionMap = Maps.newHashMap(state.getOutTransitions());

		for (List<CrySLMethodCallTransition> transitionList : transitionMap.values()) {
			LinkedList<CrySLMethodCall> path;

			for (CrySLMethodCallTransition t : transitionList) {
				if (t.to().isFinalState()) {
					path = Lists.newLinkedList();
					path.add(t.getCall());
					calcPaths.add(path);

				} else {
					ArrayList<LinkedList<CrySLMethodCall>> subPath = calcPathsForward(t.to());
					for (LinkedList<CrySLMethodCall> t1 : subPath) {
						path = Lists.newLinkedList();
						path.add(t.getCall());
						path.addAll(t1);
						calcPaths.add(path);
					}
				}
			}
		}
		return calcPaths;
	}


	
	@Deprecated
	public ArrayList<LinkedList<CrySLMethodCall>> calcFewestUserInteractionRefTypeGenPathsFromStart() {
		return calcFewestUserInteractionRefTypeGenPathsForward(startState);
	}
	
	@Deprecated
	public ArrayList<LinkedList<CrySLMethodCall>> calcFewestUserInteractionRefTypeGenPathsFrom(SootMethod method) {		
		return calcFewestUserInteractionRefTypeGenPathsForward(methodFromToStateMap.get(method).getRight());
	}
	
	@Deprecated
	public ArrayList<LinkedList<CrySLMethodCall>> calcFewestUserInteractionRefTypeGenPathsFrom_(SootMethod method) {
		return calcFewestUserInteractionRefTypeGenPathsForward(methodFromToStateMap.get(method).getLeft());
	}

	@Deprecated
	private ArrayList<LinkedList<CrySLMethodCall>> calcFewestUserInteractionRefTypeGenPathsForward(
			CrySLMethodCallState state) {
		ArrayList<LinkedList<CrySLMethodCall>> calcPaths = Lists.newArrayList();
		Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitionMap = state.getOutTransitions();

		CrySLPathFilter.filterTransitionsByFewestMethodCallUserInteractions(transitionMap);
		CrySLPathFilter.filterTransitionsByFewestRefTypeGenerations(transitionMap);

		List<CrySLMethodCallTransition> filteredTransitionList = transitionMap.values().iterator().next();
		LinkedList<CrySLMethodCall> path;

		for (CrySLMethodCallTransition t : filteredTransitionList) {

			if (t.to().isFinalState() || state == t.to()) {
				path = Lists.newLinkedList();
				path.add(t.getCall());
				calcPaths.add(path);
				return calcPaths;
			}

			ArrayList<LinkedList<CrySLMethodCall>> subPath = calcFewestUserInteractionRefTypeGenPathsForward(t.to());
			for (LinkedList<CrySLMethodCall> t1 : subPath) {
				path = Lists.newLinkedList();
				path.add(t.getCall());
				path.addAll(t1);
				calcPaths.add(path);
			}
		}
		return calcPaths;
	}

	
	public CrySLMethodCall getCrySLMethodCallBySootMethod(SootMethod method) {
		List<CrySLMethodCall> calls = Lists.newArrayList();
		for (CrySLMethodCallTransition transition : transitions) {
			if (method == transition.getCall().getSootMethod()) {
				calls.add(transition.getCall());
			}
		}
	
		for(CrySLMethodCall call : calls) {
			boolean variableReplaced = false;
			for(CrySLVariable parameter : call.getCallParameters()) {
				if(parameter.getVariable().equals("_")) {
					variableReplaced = true;
				}
			}
			
			if(!variableReplaced) {
				return call;
			}
		}	
		return calls.get(0);
	}
}
