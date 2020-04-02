package de.upb.cognicryptfix.crysl.fsm;

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
import de.upb.cognicryptfix.crysl.CrySLEntity;
import de.upb.cognicryptfix.crysl.CrySLMethodCall;
import de.upb.cognicryptfix.crysl.CrySLMethodCallCriteria;
import de.upb.cognicryptfix.crysl.CrySLVariable;
import de.upb.cognicryptfix.crysl.pool.CrySLVariablePool;
import de.upb.cognicryptfix.utils.Pair;
import soot.SootMethod;

/**
 * @author Andre Sonntag
 * @date 10.03.2020
 */
public class CrySLCallFSM {

	private CrySLEntity entity;
	private CrySLRule rule;
	private CrySLVariablePool varPool;
	private StateMachineGraph smg;
	private Set<CrySLMethodCallState> startStates;
	private Set<CrySLMethodCallState> finalStates;
	private List<CrySLMethodCallTransition> transitions;
	private List<CrySLMethodCallState> states;
	private HashMap<StateNode, CrySLMethodCallState> wrapperStateMap;
	private HashMap<SootMethod, Pair<CrySLMethodCallState, CrySLMethodCallState>> methodFromToStateMap;

	public CrySLCallFSM(CrySLEntity entity, CrySLVariablePool pool) {
		this.entity = entity;
		this.rule = entity.getRule();
		this.varPool = pool;
		this.smg = rule.getUsagePattern();
		this.wrapperStateMap = Maps.newLinkedHashMap();
		this.methodFromToStateMap = Maps.newHashMap();
		this.transitions = Lists.newArrayList();
		this.states = Lists.newArrayList();
		this.startStates = Sets.newHashSet();
		this.finalStates = Sets.newHashSet();
		createFSM();
	}

	private void createFSM() {
		List<TransitionEdge> edges_ = smg.getEdges();
		List<StateNode> states_ = new ArrayList<>(smg.getNodes());

		for (StateNode state : states_) {
			boolean initState = state == smg.getInitialTransition().getLeft() ? true : false;
			boolean finalState = smg.getAcceptingStates().contains(state) ? true : false;
			CrySLMethodCallState callState = new CrySLMethodCallState(Integer.parseInt(state.getName()), initState,
					finalState);
			wrapperStateMap.put(state, callState);
			states.add(callState);

			if (initState) {
				startStates.add(callState);
			}

			if (finalState) {
				finalStates.add(callState);
			}
		}

		for (TransitionEdge edge : edges_) {
			List<CrySLMethodCall> calls = transformTransitionEdgeToCrySLMethodCalls(edge);
			for (CrySLMethodCall call : calls) {

				CrySLMethodCallState from = wrapperStateMap.get(edge.getLeft());
				CrySLMethodCallState to = wrapperStateMap.get(edge.getRight());

				if (from.getState() != to.getState()) {
					CrySLMethodCallTransition transition = new CrySLMethodCallTransition(call, from, to);
					transitions.add(transition);
					from.addOutTransition(transition);
					to.addInTransition(transition);
					methodFromToStateMap.put(call.getSootMethod(), new Pair<CrySLMethodCallState, CrySLMethodCallState>(from, to));
				}
			}
		}
	}

	public List<LinkedList<CrySLMethodCall>> calcBestPathsForPredicateGenerationFromState(StateNode state, List<CrySLVariable> predicatePramameters) {
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
		paths = CrySLPathFilter.applyCriteriaFilters(paths);
		return paths;
	}

	public List<LinkedList<CrySLMethodCall>> calcBestPathsForPredicateGenerationFromFinalStates(List<CrySLVariable> predicatePramameters) {
		List<LinkedList<CrySLMethodCall>> calcBackwardPaths = Lists.newArrayList();

		for (CrySLMethodCallState finalState : finalStates) {
			calcBackwardPaths.addAll(calcPathsBackward(finalState, Lists.newArrayList()));
		}
		for (LinkedList<CrySLMethodCall> p : calcBackwardPaths) {
			Collections.reverse(p);
		}

		calcBackwardPaths = CrySLPathFilter.filterCallPathsByUsedPredicateParameters(calcBackwardPaths, predicatePramameters);
		calcBackwardPaths = CrySLPathFilter.applyCriteriaFilters(calcBackwardPaths);
		return calcBackwardPaths;
	}
	
	public List<LinkedList<CrySLMethodCall>> calcBestPathsToFinalStatesStartedByMultipleMethod(List<SootMethod> methods) {
		List<LinkedList<CrySLMethodCall>> calcPaths = Lists.newArrayList();
		Map<CrySLMethodCallState, List<SootMethod>> stateSootMethodMap = Maps.newHashMap();
		
		for(SootMethod method : methods) {
			CrySLMethodCallState succState = getSuccStateFromMethod(method);
			
			if(succState.isFinalState()) {
				LinkedList<CrySLMethodCall> tempPath = Lists.newLinkedList();
				tempPath.add(getCrySLMethodCallBySootMethod(method));
				calcPaths.add(tempPath);
			} else {
				if(stateSootMethodMap.containsKey(succState)) {
					stateSootMethodMap.get(succState).add(method);
				} else {
					stateSootMethodMap.put(succState, Lists.newArrayList(method));
				}
			}			
		}
		
		for(CrySLMethodCallState state : stateSootMethodMap.keySet()) {
			List<SootMethod> stateMethods = stateSootMethodMap.get(state);
			List<LinkedList<CrySLMethodCall>> tempPaths = calcBestPathsToFinalStatesStartedByMethod(stateMethods.get(0));
			
			if(tempPaths.size() > 0) {
				if(stateMethods.size() > 1) {
					for(SootMethod method : stateMethods) {
						CrySLMethodCall call = getCrySLMethodCallBySootMethod(method); 
						for(LinkedList<CrySLMethodCall> path : tempPaths) {
							LinkedList<CrySLMethodCall> tempPath = Lists.newLinkedList(path);
							tempPath.set(0, call);
							calcPaths.add(tempPath);
						}
					}
				} else {
					calcPaths.addAll(tempPaths);
				}
			} else {
				if(stateMethods.size() > 1) {
					for(SootMethod method : stateMethods) {
						CrySLMethodCall call = getCrySLMethodCallBySootMethod(method); 
						LinkedList<CrySLMethodCall> tempPath = Lists.newLinkedList();
						tempPath.add(call);
						calcPaths.add(tempPath);
					}
				} else {
					calcPaths.addAll(tempPaths);
				}
			}	
		}
		
		calcPaths = CrySLPathFilter.applyCriteriaFilters(calcPaths);		
		return calcPaths;
	}

	public List<LinkedList<CrySLMethodCall>> calcBestPathsToFinalStatesStartedByMethod(SootMethod method) {
		List<LinkedList<CrySLMethodCall>> calcForwardPaths = Lists.newArrayList();
		CrySLMethodCallState succState = getSuccStateFromMethod(method);
		calcForwardPaths = calcPathsForward(succState);
		calcForwardPaths = CrySLPathFilter.applyCriteriaFilters(calcForwardPaths);		
		CrySLMethodCall call = getCrySLMethodCallBySootMethod(method); 
	
		if(calcForwardPaths.size() > 0) {
			for(LinkedList<CrySLMethodCall> path : calcForwardPaths) {
				path.add(0, call);
			}
		} else {
			LinkedList<CrySLMethodCall> path = Lists.newLinkedList();
			path.add(call);
			calcForwardPaths.add(path);
		}
		calcForwardPaths = CrySLPathFilter.applyCriteriaFilters(calcForwardPaths);		
		return calcForwardPaths;
	}
	
	public List<LinkedList<CrySLMethodCall>> calcBestPathsToFinalStatesFromPredState(SootMethod method) {
		List<LinkedList<CrySLMethodCall>> calcForwardPaths = Lists.newArrayList();
		calcForwardPaths = calcPathsForward(getPredStateFromMethod(method));
		calcForwardPaths = CrySLPathFilter.applyCriteriaFilters(calcForwardPaths);
		return calcForwardPaths;
	}

	public List<LinkedList<CrySLMethodCall>> calcBestPathsToFinalStatesFromSuccState(SootMethod method) {
		List<LinkedList<CrySLMethodCall>> calcForwardPaths = Lists.newArrayList();
		CrySLMethodCallState succState = getSuccStateFromMethod(method);
		calcForwardPaths = calcPathsForward(succState);
		calcForwardPaths = CrySLPathFilter.applyCriteriaFilters(calcForwardPaths);
		return calcForwardPaths;
	}

	private ArrayList<LinkedList<CrySLMethodCall>> calcPathsBackward(CrySLMethodCallState state, List<CrySLMethodCallState> visitedStates) {
		visitedStates.add(state);

		ArrayList<LinkedList<CrySLMethodCall>> calcPaths = Lists.newArrayList();
		Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitionMap = Maps
				.newHashMap(state.getInTransitions());

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
		Map<CrySLMethodCallCriteria, List<CrySLMethodCallTransition>> transitionMap = Maps
				.newHashMap(state.getOutTransitions());

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

	private List<CrySLMethodCall> transformTransitionEdgeToCrySLMethodCalls(TransitionEdge edge) {
		List<CrySLMethodCall> calls = Lists.newArrayList();
		for (CrySLMethod crySLMethod : edge.getLabel()) {
			List<SootMethod> sootMethods = Lists.newArrayList(CrySLMethodToSootMethod.v().convert(crySLMethod));
			if (!sootMethods.isEmpty()) {
				for(SootMethod method : sootMethods) {
					calls.add(new CrySLMethodCall(rule, method, crySLMethod, varPool));
				}		
			} else {
				System.err.println(crySLMethod);
			}
		}
		return calls;
	}
	
	private CrySLMethodCallState getSuccStateFromMethod(SootMethod method) {
		return methodFromToStateMap.get(method).getRight();
	}
	
	private CrySLMethodCallState getPredStateFromMethod(SootMethod method) {
		return methodFromToStateMap.get(method).getLeft();
	}

	public CrySLMethodCall getCrySLMethodCallBySootMethod(SootMethod method) {
		List<CrySLMethodCall> calls = Lists.newArrayList();
		for (CrySLMethodCallTransition transition : transitions) {
			if (method == transition.getCall().getSootMethod()) {
				calls.add(transition.getCall());
			}
		}

		for (CrySLMethodCall call : calls) {
			boolean variableReplaced = false;
			for (CrySLVariable parameter : call.getCallParameters()) {
				if (parameter.getName().equals("_")) {
					variableReplaced = true;
				}
			}

			if (!variableReplaced) {
				return call;
			}
		}
		return calls.get(0);
	}


}
