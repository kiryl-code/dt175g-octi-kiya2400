package se.miun.dt175g.octi.client;


import java.util.List;
import java.util.Map;

import se.miun.dt175g.octi.core.Agent;
import se.miun.dt175g.octi.core.Direction;
import se.miun.dt175g.octi.core.Node;
import se.miun.dt175g.octi.core.OctiAction;
import se.miun.dt175g.octi.core.OctiBoard;
import se.miun.dt175g.octi.core.OctiState;
import se.miun.dt175g.octi.core.PlayerColor;
import se.miun.dt175g.octi.core.Pod;
import se.miun.dt175g.octi.core.Point;


public class StudentAgent extends Agent {
	// TODO: adjust values. Not balanced right now.
	private final double rewardForMissingPods = 15.0;
	private final double rewardForStepToOpponentBase = 50.0;
	private final Map<Direction, Double> PodsProngReward = Map.of(
			Direction.BACK, 1.0,
			Direction.BACK_LEFT, 1.0,
			Direction.BACK_RIGHT, 1.0,
			Direction.LEFT, 2.0,
			Direction.RIGHT, 2.0,
			Direction.FRONT_LEFT, 3.0,
			Direction.FRONT_RIGHT, 3.0,
			Direction.FRONT, 4.0
	);
	private int stepCounter = 0;
	private int maxDepth = 2;
	private long maxTime = 0;
	private boolean shouldStop = false;
	
	@Override
	public OctiAction getNextMove(OctiState state) {
		long startTime = System.currentTimeMillis();
		maxTime = startTime +((long) (getTimeLimit() * 0.9));
		shouldStop = false;

		Node<OctiState, OctiAction> root = Node.root(state);
		List<Node<OctiState, OctiAction>> childStates = Agent.generateChildNodes(root);
		RewardValue bestReward = new RewardValue(maxDepth, false);
		Node<OctiState, OctiAction> bestNode = null;
		stepCounter++;
		for (Node<OctiState, OctiAction> node : childStates) {
			RewardValue stateValue = doMagic(node.state, maxDepth,  new RewardValue(0, false), new RewardValue(0, true),false);
//			System.out.println("Step: " + stepCounter + "; StateReward: " + stateValue.getValue() + "; Best: " + bestReward.getValue() + "; Applying: " + (stateValue.compareTo(bestReward) > 0));
			if (stateValue.compareTo(new RewardValue(maxDepth, true)) == 0) {
				return (OctiAction) node.action;
			}
			if (stateValue.compareTo(bestReward) > 0 ) {
				bestReward = stateValue;
				bestNode = node;
//				System.out.println("bestReward: " + bestReward);
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("" + stepCounter +" Time: " + (endTime - startTime));
		if (bestNode == null) {
			return childStates.getFirst().action;
		}
		return (OctiAction) bestNode.action;
	}

	private PlayerColor myColor() {
		return this.player.getColor();
	}

	private RewardValue doMagic(OctiState state, int depth, RewardValue alpha, RewardValue beta, boolean isMaximizingPlayer) {
		if (depth == 0 || state.isTerminal()) {
			return calculateReward(state, depth);
		}
		Node<OctiState, OctiAction> currentNode = Node.root(state);
		List<Node<OctiState, OctiAction>> subTree = Agent.generateChildNodes(currentNode);
		subTree.sort((n1, n2) -> {RewardValue a = calculateReward(n1.state, depth); RewardValue b = calculateReward(n2.state, depth); return isMaximizingPlayer ? b.compareTo(a) : a.compareTo(b); });
		if (isMaximizingPlayer) {
			RewardValue maxEval = null;

			for (Node<OctiState, OctiAction> node : subTree) {
				RewardValue nextLevelValue = doMagic(node.state, depth - 1,  alpha, beta,false);
				maxEval = RewardValue.max(maxEval, nextLevelValue);
				alpha = RewardValue.max(alpha, nextLevelValue);
				if (beta.compareTo(alpha) <= 0) {
					break;
				}
			}

			return maxEval == null ? new RewardValue(-Double.MAX_VALUE, depth) : maxEval;
		} else {
			RewardValue minEval = null;

			for (Node<OctiState, OctiAction> node : subTree) {
				RewardValue nextLevelValue = doMagic(node.state, depth-1,  alpha, beta, true);
				minEval = RewardValue.min(nextLevelValue, minEval);
				beta = RewardValue.min(beta, nextLevelValue);
				if (alpha.compareTo(beta) >= 0) {
					break;
				}
			}
			return minEval == null ? new RewardValue(Double.MAX_VALUE, depth) : minEval;
		}
	}

	private RewardValue calculateReward(OctiState state, int depth) {
		if (state.isTerminal()) {
			if (state.getWinner() == getPlayer().getPlayerId()) {
				return new RewardValue(depth, true);
			} else {
				return new RewardValue(depth, false);
			}
		}
		return new RewardValue(getRewardForPodsAmount(state) + getRewardForDistanceToBase(state) + getRewardForProngs(state), depth);
	}

	private double getRewardForProngs(OctiState state) {
		double totalReward = 0.0;
		List<Pod> myPods;
		List<Pod> opponentPods;
		Map<Direction, Double> myProngReward;
		Map<Direction, Double> opponentProngReward;
		if (myColor() == PlayerColor.RED) {
			myPods = state.getRedPods();
			opponentPods = state.getBlackPods();
			myProngReward = PodsProngReward;
			opponentProngReward = PodsProngReward;
		} else {
			myPods = state.getBlackPods();
			opponentPods = state.getRedPods();
			myProngReward = PodsProngReward;
			opponentProngReward = PodsProngReward;
		}

		for (Pod myPod : myPods) {
			for (Direction d : Direction.values()) {
				if (myPod.hasProng(d)) {
					totalReward += myProngReward.get(d);
				}
			}
		}

		for (Pod opponentPod : opponentPods) {
			for (Direction d : Direction.values()) {
				if (opponentPod.hasProng(d)) {
					totalReward -= opponentProngReward.get(d);
				}
			}
		}

		return totalReward;
	}

	private double getRewardForDistanceToBase(OctiState state) {
		double totalReward = 0.0;
		OctiBoard board = state.getBoard();
		Point[] myBasePositions;
		Point[] opponentBasePositions;
		List<Pod> myPods;
		List<Pod> opponentPods;
		if (myColor() == PlayerColor.RED) {
			myBasePositions = state.getRedBase();
			opponentBasePositions = state.getBlackBase();
			myPods = state.getRedPods();
			opponentPods = state.getBlackPods();
		} else {
			myBasePositions = state.getBlackBase();
			opponentBasePositions = state.getRedBase();
			myPods = state.getBlackPods();
			opponentPods = state.getRedPods();
		}
		for (Pod myPod : myPods) {
			Point myPodPosition = board.getPositionFromPod(myPod);
			int minDist = Integer.MAX_VALUE;
			for (Point opponentBasePosition : opponentBasePositions) {
				int dY = Math.abs(myPodPosition.y() - opponentBasePosition.y());
				int dX = Math.abs(myPodPosition.x() - opponentBasePosition.x());
				int distance = dX + dY;
				minDist = Math.min(minDist, distance);
			}
			totalReward += rewardForStepToOpponentBase / minDist;
		}

		for (Pod opponentPod : opponentPods) {
			Point opponentPodPosition = board.getPositionFromPod(opponentPod);
			int minDist = Integer.MAX_VALUE;
			for (Point myBasePosition : myBasePositions) {
				int dY = Math.abs(opponentPodPosition.y() - myBasePosition.y());
				int dX = Math.abs(opponentPodPosition.x() - myBasePosition.x());
				int distance = dX + dY;
				minDist = Math.min(minDist, distance);
			}
			totalReward -= rewardForStepToOpponentBase / minDist;
		}

		return totalReward;
	}

	private double getRewardForPodsAmount(OctiState state) {
		int myPods;
		int opponentPods;
		if (myColor() == PlayerColor.RED) {
			myPods = state.getRedPods().size();
			opponentPods = state.getBlackPods().size();
		} else {
			myPods = state.getBlackPods().size();
			opponentPods = state.getRedPods().size();
		}
		return (myPods - opponentPods) * rewardForMissingPods;
	}

}
