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
	// ========== EVALUATION ==========
	private final double rewardForMissingPods = 15.0;
	private final double rewardForStepToOpponentBase = 50.0;
	private final Map<Direction, Double> podsProngReward = Map.of(
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
	private int maxDepth = 10;
	private long timeWhenShouldStop = 0;
	private long startTime = 0;

	private static final Direction[] DIRECTIONS = Direction.values();
	
	@Override
	public OctiAction getNextMove(OctiState state) {
		startTime = System.currentTimeMillis();
		timeWhenShouldStop = startTime +((long) (getTimeLimit() * 0.8));

		Node<OctiState, OctiAction> root = Node.root(state);
		List<Node<OctiState, OctiAction>> childStates = Agent.generateChildNodes(root);
		OctiAction bestAction = childStates.getFirst().action;

		for (int depth = 1; depth <= maxDepth; depth++) {
			OctiAction bestActionAtDepth = levelAnalysis(childStates, depth);
			if (shouldStop()) {
				break;
			}

			if (bestActionAtDepth != null) {
				bestAction = bestActionAtDepth;
			}
		}
		long endTime = System.currentTimeMillis();
		System.out.println("" + stepCounter +" Time: " + (endTime - startTime));
		return bestAction;
	}

	private OctiAction levelAnalysis(List<Node<OctiState, OctiAction>> levelNodes, int depth) {
		RewardValue bestReward = null;
		OctiAction bestAction = null;

		RewardValue alpha = new RewardValue(0, false);
		RewardValue beta = new RewardValue(0, true);

		for (Node<OctiState, OctiAction> node: levelNodes) {
			if (shouldStop()) {
				return null;
			}

			RewardValue rewardForState = doMagic(node.state, depth - 1, alpha, beta, false);

			if (rewardForState.isEqualTo(new RewardValue(depth, true))) {
				return node.action;
			}

			if (rewardForState.isGreaterThan(bestReward)) {
				bestReward = rewardForState;
				bestAction = node.action;
			}

			alpha = RewardValue.max(alpha, bestReward);
		}
		return bestAction;
	}

	private void sortStatesList(List<Node<OctiState, OctiAction>> list, int depth, boolean isMaximizingPlayer) {
		if (shouldStop()) {
			return;
		}
		list.sort((n1, n2) -> {
			RewardValue a = calculateReward(n1.state, depth);
			RewardValue b = calculateReward(n2.state, depth);
			return isMaximizingPlayer ? b.compareTo(a) : a.compareTo(b);
		});
	}

	private RewardValue doMagic(OctiState state, int depth, RewardValue alpha, RewardValue beta, boolean isMaximizingPlayer) {
		if (shouldStop() || depth == 0 || state.isTerminal()) {
			return calculateReward(state, depth);
		}

		Node<OctiState, OctiAction> localParentNode = Node.root(state);
		List<Node<OctiState, OctiAction>> subTree = Agent.generateChildNodes(localParentNode);
        sortStatesList(subTree, depth, isMaximizingPlayer);

        if (isMaximizingPlayer) {
			RewardValue maxEval = null;
			for (Node<OctiState, OctiAction> node : subTree) {
				RewardValue nextLevelValue = doMagic(node.state, depth - 1,  alpha, beta,false);
				maxEval = RewardValue.max(maxEval, nextLevelValue);
				alpha = RewardValue.max(alpha, nextLevelValue);
				if (beta.isLessThan(alpha) || beta.isEqualTo(alpha)) {
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
				if (alpha.isGreaterThan(beta) || alpha.isEqualTo(beta)) {
					break;
				}
			}
			return minEval == null ? new RewardValue(Double.MAX_VALUE, depth) : minEval;
		}
	}

	private RewardValue calculateReward(OctiState state, int depth) {
		if (state.isTerminal()) {
			boolean isWin = state.getWinner() == getPlayer().getPlayerId();
			return new RewardValue(depth, isWin);
		}
		double score = getRewardForPodsAmount(state) + getRewardForDistanceToBase(state) + getRewardForProngs(state);
		return new RewardValue(score, depth);
	}

	private double getRewardForProngs(OctiState state) {
		List<Pod> myPods = getMyPods(state);
		List<Pod> opponentPods = getOpponentPods(state);
		double myProngReward = getProngScore(myPods);
		double opponentProngReward = getProngScore(opponentPods);

		return myProngReward - opponentProngReward;
	}

	private double getProngScore(List<Pod> pods) {
		double score = 0;
		for (Pod pod : pods) {
			for (Direction d : DIRECTIONS) {
				if (pod.hasProng(d)) {
					score += podsProngReward.get(d);
				}
			}
		}
		return score;
	}

	private double getRewardForDistanceToBase(OctiState state) {
		OctiBoard board = state.getBoard();
		Point[] myBasePositions = amIRed() ? state.getRedBase() : state.getBlackBase();
		Point[] opponentBasePositions = amIRed() ? state.getBlackBase() : state.getRedBase();
		List<Pod> myPods = getMyPods(state);
		List<Pod> opponentPods = getOpponentPods(state);
		double myPodsReward = getDistanceScore(board, myPods, opponentBasePositions);
		double opponentPodsReward = getDistanceScore(board, opponentPods, myBasePositions);

		return myPodsReward - opponentPodsReward;
	}

	private double getDistanceScore(OctiBoard board, List<Pod> pods, Point[] base) {
		double score = 0;
		for (Pod pod : pods) {
			Point podPos = board.getPositionFromPod(pod);
			int minDist = Integer.MAX_VALUE;
			for (Point basePosition : base) {
				int dY = Math.abs(podPos.y() - basePosition.y());
				int dX = Math.abs(podPos.x() - basePosition.x());
				int distance = dX + dY;
				minDist = Math.min(minDist, distance);
			}
			score += rewardForStepToOpponentBase / minDist;
		}
		return score;
	}

	private double getRewardForPodsAmount(OctiState state) {
		int myPods = getMyPods(state).size();
		int opponentPods = getOpponentPods(state).size();
		return (myPods - opponentPods) * rewardForMissingPods;
	}

	private boolean shouldStop() {
		long currentTime = System.currentTimeMillis();
		return currentTime >= timeWhenShouldStop;
	}

	private PlayerColor myColor() {
		return this.player.getColor();
	}

	private List<Pod> getMyPods(OctiState state) {
		return myColor() == PlayerColor.RED ? state.getRedPods() : state.getBlackPods();
	}

	private List<Pod> getOpponentPods(OctiState state) {
		return myColor() == PlayerColor.RED ? state.getBlackPods() : state.getRedPods();
	}

	private boolean amIRed() {
		return myColor() == PlayerColor.RED;
	}
}
