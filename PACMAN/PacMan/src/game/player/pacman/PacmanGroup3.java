package game.player.pacman;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import game.core.G;
import game.core.Game;
import game.core.Game.DM;
import gui.AbstractPlayer;

public final class PacmanGroup3 extends AbstractPlayer {

	@Override
	public String getGroupName() {
		return "Pacman Group 3 - XCS";
	}

	int currentScore = 0;
	int currentNumPills = 0;

	double lastScore = 0.0;
	double Score = 0.0;

	int minDist = 20;

	int[] activePills;
	int[] activePowerPills;

	// BEGIN REWARD V2
	int lastPillCount;
	int lastPowerPillCount;
	int lastPacmanNode;
	int lastPacmanDirection;
	int lastDistance;
	// END REWARD V2

	XCS xcs = null;
	int count = 0;
	int nearestPillIndex;
	int nearestPowerIndex;
	
	@Override
	public int getAction(Game game, long timeDue) {
		int currentLoc = game.getCurPacManLoc();

		if (xcs == null) {
			xcs = new XCS();
		} else {
			
			//int ret = xcs.setReward(possibleReward(currentLoc, game));
			int ret = xcs.setReward(getBetterReward(game));
			//xcs.setReward(reward(game, currentLoc));
			if (count++ % 5000 == 0 && xcs.pExplore > 0.001) {
				xcs.pExplore *= 0.9;
			}
		}

		activePills = game.getPillIndicesActive();
		activePowerPills = game.getPillIndicesActive();
		nearestPillIndex = game.getTarget(currentLoc, activePills, true, G.DM.PATH);
		nearestPowerIndex = game.getTarget(currentLoc, activePowerPills, true, G.DM.PATH);
		
		int move = 0;
		if (currentLoc > -1) {
			String state = getStateArray(currentLoc, game);
			move = xcs.getAction(state, timeDue);
		}

		int action = interpretAction(move, game);
		return action;
	}
	

	private int interpretAction(int metaAction, Game game ){
		//0 = take pill, 1 = take powerPill, 2 = goToGhost, 3 = flewFromGhost;
		
		int pacPos = game.getCurPacManLoc();

		int[] distGhost = new int[game.NUM_GHOSTS];
		int[] dirGhost = new int[game.NUM_GHOSTS];
		int dirToPill = -1;
		
		for (int i = 0; i < game.NUM_GHOSTS; ++i) {
			dirGhost[i] = directionToGhost(i, game);
			distGhost[i] = game.getGhostPathDistance(i, pacPos);
		}
		for (int i = 1; i < dirGhost.length; i++) {
			for (int j = dirGhost.length - 1; j >= i; j--) {
				if (distGhost[j - 1] > distGhost[j]) {
					int temp = distGhost[j - 1];
					distGhost[j - 1] = distGhost[j];
					distGhost[j] = temp;
					temp = dirGhost[j - 1];
					dirGhost[j - 1] = dirGhost[j];
					dirGhost[j] = temp;
				}
			}
		}
		
		switch(metaAction) {
		case(0):
			if (nearestPillIndex == -1)
				nearestPillIndex = nearestPowerIndex;
			dirToPill = directionTo(nearestPillIndex, game);
			return dirToPill != -1 ? dirToPill : game.getCurPacManDir();
		case(1):
			if (nearestPowerIndex != -1) 
				dirToPill = directionTo(nearestPowerIndex, game);
			return dirToPill != -1 ? dirToPill : game.getCurPacManDir();
		case(2):
			for (int i = game.NUM_GHOSTS - 1; i >= 0; --i) {
				if (distGhost[i] > -1)
					return dirGhost[i]; 
			}
			return game.getCurPacManDir();
		case(3):
			for (int i = game.NUM_GHOSTS - 1; i >= 0; --i) {
				if (distGhost[i] > -1)
					return dirGhost[i]; 
			}
			return game.getCurPacManDir();
			default: return game.getCurPacManDir();
		}
	}

	private double getBetterReward(Game game) {
		int GHOST_MAX_DIST = 16;
		int[] orderedGhosts = new int[Game.NUM_GHOSTS];
		int[] orderedDirectionToGhost = new int[Game.NUM_GHOSTS];

		double reward = 0;
		int pillCount = activePills.length;
		int powerPillCount = activePowerPills.length;
		int pacLoc = game.getCurPacManLoc();
		int pacDir = game.getCurPacManDir();

		int nextPillDir = -1;
		if (nearestPillIndex != -1) {
			nextPillDir = directionTo(nearestPillIndex, game);
		}

		boolean noNextPillBonus = true;

		int[] ghostDistances = new int[Game.NUM_GHOSTS];
		int[] ghostDirections = new int[Game.NUM_GHOSTS];
		for (int i = 0; i < Game.NUM_GHOSTS; i++) {
			ghostDistances[i] = game.getGhostPathDistance(i, pacLoc);
			ghostDirections[i] = game.getNextPacManDir(game.getCurGhostLoc(i), true, DM.MANHATTEN);
		}

		for (int i = 0; i < Game.NUM_GHOSTS; i++) {
			int minDist = Integer.MAX_VALUE;
			int minIndex = -1;
			for (int j = 0; j < Game.NUM_GHOSTS; j++) {
				if (minDist > ghostDistances[j]) {
					minDist = ghostDistances[j];
					minIndex = j;
				}
			}
			orderedGhosts[i] = minIndex;
			orderedDirectionToGhost[i] = game.getNextPacManDir(game.getCurGhostLoc(minIndex), true, DM.MANHATTEN);
			ghostDistances[minIndex] = Integer.MAX_VALUE;
		}

		for (int i = 0; i < Game.NUM_GHOSTS; i++) {
			int ghostDistance = game.getGhostPath(orderedGhosts[i], lastPacmanNode).length;
			if (ghostDistance < GHOST_MAX_DIST && ghostDistance != 0) {
				if (!game.isEdible(orderedGhosts[i])
						&& (pacDir == orderedDirectionToGhost[i] || game.getCurPacManDir() == 4)) {
					reward = reward - (30 - 5 * i);
					noNextPillBonus = false;
				} else if (!game.isEdible(orderedGhosts[i]) && pacDir != orderedDirectionToGhost[i]
						&& game.getCurPacManDir() != 4) {
					reward = reward + (20 - 5 * i);
				} else if (game.isEdible(orderedGhosts[i]) && pacDir == orderedDirectionToGhost[i]) {
					reward = reward + (20 - 5 * i);
				} else {

				}
			}
		}

		if (lastPacmanDirection == game.getReverse(pacDir)) {
			reward = reward - 20;
		} else {
			reward = reward + 5;
		}

		if (lastPillCount == pillCount && lastPowerPillCount == powerPillCount && noNextPillBonus) {
			if (pacDir == nextPillDir && lastPacmanDirection != game.getReverse(pacDir))
				reward = reward + 15;
			else if (lastPacmanDirection != game.getReverse(pacDir))
				reward = reward - 5;

		}

		if (pillCount < lastPillCount && pacDir == nextPillDir && noNextPillBonus) {
			reward = reward + 15;
		}

		if (powerPillCount < lastPowerPillCount) {
			reward = reward + 20;
		}

		lastPillCount = pillCount;
		lastPowerPillCount = powerPillCount;
		lastPacmanNode = pacLoc;
		lastPacmanDirection = pacDir;

		return reward;
	}
	
	private double possibleReward(int index, Game game) {
		double q = 0.0;
		int score = game.getScore();
		int remainingLives = game.getLivesRemaining();

		int pillDist = game.getPathDistance(index, nearestPillIndex);
		int powerPillDist = game.getPathDistance(index, nearestPowerIndex);

		int nearestJunction = game.getTarget(index, game.getJunctionIndices(), true, G.DM.PATH);
		int nearJuncDist = game.getPathDistance(index, nearestPowerIndex);

		double reward = 10.0 * 1.0 / Math.pow(1 + pillDist, 2);
		 
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < 4; ++i) {
			int dist = game.getPathDistance(index, game.getCurGhostLoc(i)); // game.getGhostPathDistance(i,
																			// index);
			if (dist == 0)
				continue;
			if (min > dist && dist >= 0) {
				min = dist;
			}
		}

		q += reward;
		if (min < -1)
			q *= (1 + min) / 32;

		return q;
	}

	private String getBinStr(int i, int n) {
		String format = "%" + n + "s";
		String ret = String.format(format, Integer.toBinaryString(i)).replace(" ", "0");
		if (ret.length() > n)
			System.err.println("Error2:" + i + " vs " + ret + " || " + ret.length() + " vs " + n);
		return ret;
	}

	private int directionTo(int to, Game game) {
		int pacman = game.getCurPacManLoc();
		int dist = game.getPathDistance(pacman, to);
		int[] nb = game.getPacManNeighbours();
		if (dist > 0) {
			int[] path = game.getPath(pacman, to);
			int firstNode = path.length > 1 ? path[1] : path[0];
			for (int j = 0; j < 4; j++)
				if (firstNode == nb[j])
					return j;
		}
		return -1;
	}

	private int directionToGhost(int i, Game game) {
		int pacman = game.getCurPacManLoc();
		int dist = game.getPathDistance(pacman, game.getCurGhostLoc(i));
		int[] nb = game.getPacManNeighbours();
		if (dist >= 0) {
			int[] path = game.getGhostPath(i, pacman);
			if (path.length > 0) {
				int lastNode = path[path.length - 1];
				for (int j = 0; j < 4; j++)
					if (lastNode == nb[j])
						return j;
			}
		}
		return -1;
	}

	private String getStateArray(int pacPos, Game game) {
		final int maxDist = (int) (Math.pow(2, 4) - 1);
		String state = "";

		int min = Integer.MAX_VALUE;
		int dir = -1;
		int[] posDir = game.getPossiblePacManDirs(true);
		int[] nb = game.getPacManNeighbours();

		for (int n : nb)
			state += n != -1 ? 1 : 0;

		/*
		 * int[] dirArr = { 0, 0, 0, 0 }; for (int i = 0; i < 4; ++i) for (int j
		 * : posDir) if (i == j) dirArr[i] = 1; for (int i : dirArr) state += i;
		 */

		if (nearestPillIndex == -1)
			nearestPillIndex = nearestPowerIndex;

		int dirToPill = directionTo(nearestPillIndex, game);
		state += dirToPill != -1 ? getBinStr(dirToPill, 2) : "##";

		if (nearestPowerIndex != -1) {
			dirToPill = directionTo(nearestPowerIndex, game);
			state += dirToPill != -1 ? getBinStr(dirToPill, 2) : "##";
		}
		else 
			state += "##";

		int[] distGhost = new int[game.NUM_GHOSTS];
		int[] dirGhost = new int[game.NUM_GHOSTS];
		for (int i = 0; i < game.NUM_GHOSTS; ++i) {
			dirGhost[i] = directionToGhost(i, game);
			distGhost[i] = game.getGhostPathDistance(i, pacPos);
		}
		for (int i = 1; i < dirGhost.length; i++) {
			for (int j = dirGhost.length - 1; j >= i; j--) {
				if (distGhost[j - 1] > distGhost[j]) {
					int temp = distGhost[j - 1];
					distGhost[j - 1] = distGhost[j];
					distGhost[j] = temp;
					temp = dirGhost[j - 1];
					dirGhost[j - 1] = dirGhost[j];
					dirGhost[j] = temp;
				}
			}
		}

		for (int i = game.NUM_GHOSTS - 1; i >= 0; --i) {
			if (dirGhost[i] != -1)
				state += getBinStr(dirGhost[i], 2) + (game.isEdible(i) ? 1 : 0);
			else
				state += "###";

			if (distGhost[i] > -1 && distGhost[i] < 16)
				state += getBinStr(distGhost[i], 4);
			else
				state += "1111";

		}
		// for (int m : distGhost)
		// System.out.println(m);

	    if (state.length() != 36) System.out.println(state + " " + state.length());
		return state;
	}

	protected int directionToNextPill(Game game) {
		int curPos = game.getCurPacManLoc();
		int[] pillNodes = game.getPillIndices();

		double min = Double.POSITIVE_INFINITY;
		int nextNode = -1;
		for (int i = 0; i < pillNodes.length; i++) {
			int index = game.getPillIndex(pillNodes[i]);
			if (!game.checkPill(index)) {
				pillNodes[i] = -1;
			} else {

				int distance = game.getManhattenDistance(curPos, pillNodes[i]);
				if (min > distance) {
					min = distance;
					nextNode = pillNodes[i];

				}
			}
		}
		int[] path = null;
		if (nextNode != -1) {
			path = game.getPath(curPos, nextNode);
			if (path.length >= 2) {
				return directionTo(path[1], game);
			}
			if (path.length >= 1 && path[0] != nextNode) {
				return directionTo(nextNode, game);
			}
		}
		return -1;
	}

	class Classifier {
		char[] rule = new char[0];
		double p; // payoff
		double e; // error
		double F; // fitness

		double as = 1; // actionSet occurence
		double ts = 0.0; // timestep
		double accuracy;
		double exp = 0; // experience
		int n = 1;

		int action;

		double pI = 0.01;
		double eI = 0.01;
		double FI = 0.01;

		public Classifier() {
			p = pI;
			e = eI;
			F = FI;
		}

		public Classifier(String r) {
			this.rule = new char[r.length()];
			p = pI;
			e = eI;
			F = FI;
		}

		public Classifier(Classifier o) {
			this.p = o.p;
			this.e = o.e;
			this.F = o.F;
			this.as = o.as;
			this.ts = o.ts;
			this.accuracy = o.accuracy;
			this.exp = o.exp;
			this.n = o.n;
			this.action = o.action;
			// this.rule = new char[o.rule.length];
			this.rule = o.rule.clone();
			// System.out.println("Con1"+rule.length+ new String(this.rule));
		}

		public boolean equals(String o) {
			if (this.rule.length != o.length())
				System.err.println("Error1: " + this.rule.length + " vs " + o.length() + "-- " + new String(this.rule)
						+ " || " + o);
			for (int i = 0; i < rule.length; ++i) {
				if (rule[i] != '#' && rule[i] != o.charAt(i))
					return false;
			}
			return true;
		}

		public boolean equals(Classifier o) {
			boolean sameRule = true;
			for (int i = 0; i < rule.length; ++i) {
				if (rule[i] != o.rule[i]) {
					sameRule = false;
					break;
				}
			}
			if (this.action == o.action && sameRule) {
				return true;
			} else {
				return false;
			}
		}

	}

	class doubool {
		boolean b = false;
		double d = 0.0;

		public boolean isSet() {
			return b;
		}

		public void set() {
			this.b = true;
		}
	}

	class XCS {

		int[] possibleActions = { 0, 1, 2, 3 };

		// maximum population size
		int N = 100;

		// used for GA
		double GA_threshold = 30;
		double GA_cross = 0.85;
		double GA_mutate = 0.01;

		// used for multistep
		double discount = 0.71;

		//
		double delThresh = 35;
		double fracMeanFitness = 0.3;
		double subThresh = 20;

		double pHash = 0.33;
		double pExplore = 0.10;

		double mnaThresh = 4;

		// used for fitness
		double e0 = 100; // thresholdError
		double alpha = 0.1;
		double v = 5;

		double threshold;
		double lRate = 0.4;

		String rule = "";
		String lastRule = "";

		boolean doASSubsumption = true;
		boolean doGASubsumption = true;

		double currentTime = 0.0;

		ArrayList<Classifier> population = new ArrayList<Classifier>();
		ArrayList<Classifier> matchSet = new ArrayList<Classifier>();
		ArrayList<Classifier> lastActionSet = new ArrayList<Classifier>();
		ArrayList<Classifier> actionSet = new ArrayList<Classifier>();
		HashSet<Integer> possibleActionSet = new HashSet<Integer>();
		doubool PA[] = new doubool[possibleActions.length];

		Random rng = new Random();

		double reward = 0.0;
		double lastReward = 0.0;

		public XCS() {
			for (int posAct : possibleActions) {
				possibleActionSet.add(posAct);
				PA[posAct] = new doubool();
			}
		}

		public int getAction(String rule, long time) {
			this.currentTime = time;
			this.rule = rule;
			generateMatchingSet(this.rule);
			generatePredictionArray();
			int action = selectAction(PA);
			generateActionSet(action);
			return action;
		}

		public int setReward(double rp) {
			reward = rp; // reinforcement
			if (!lastActionSet.isEmpty()) {
				double best = Double.NEGATIVE_INFINITY;
				// #System.out.println("~~~PA:");
				for (doubool p : PA) {
					if (p.isSet() && p.d > best) {
						best = p.d;
						// #System.out.println(p.d);
					}
				}
				// #System.out.println("~~~");
				double P = lastReward + discount * best;
				updateSet(lastActionSet, P);
				runGA(lastActionSet, lastRule);
			}

			/*
			 * System.out.println("---------------");
			 * System.out.println("lastActions"); int i = 0; for (Classifier c :
			 * lastActionSet) { System.out.println((i++) + ":"+c.rule+":"+
			 * c.action + " # " + c.p); } i = 0;
			 * System.out.println("actionSet"); for (Classifier c : actionSet) {
			 * System.out.println((i++) + ":"+c.rule+":"+ c.action + " # " +
			 * c.p); } System.out.println("---------------");
			 */
			lastActionSet = (ArrayList<Classifier>) actionSet.clone();
			lastReward = reward;
			lastRule = rule;
			return 0;
		}

		private void generateMatchingSet(String rule) {
			matchSet.clear();
			while (matchSet.isEmpty()) {
				HashSet<Integer> actions = new HashSet<Integer>();
				for (Classifier c : population) {
					if (c.equals(rule)) {
						matchSet.add(c);
						actions.add(c.action);
					}
				}
				if (actions.size() < mnaThresh) {
					Classifier cl = covering(rule, actions);
					population.add(cl);
					deleteFromPopulation();
					matchSet.clear();
				}
			}
		}

		private Classifier covering(String rule, HashSet<Integer> actions) {
			Classifier cl = new Classifier(rule);
			for (int i = 0; i < rule.length(); ++i) {
				cl.rule[i] = rng.nextDouble() < pHash ? '#' : rule.charAt(i);
			}
			HashSet<Integer> possible = (HashSet<Integer>) possibleActionSet.clone();
			possible.removeAll(actions);
			int action = (Integer) possible.toArray()[rng.nextInt(possible.size())];
			cl.action = action;
			cl.ts = currentTime;
			return cl;
		}

		private void generatePredictionArray() {
			for (doubool p : PA) {
				// //#System.err.println(p);
				p.b = false;
			}
			double FSA[] = new double[possibleActions.length];
			for (Classifier c : matchSet) {
				if (!PA[c.action].isSet()) {
					PA[c.action].set();
					PA[c.action].d = c.p * c.F;
				} else {
					PA[c.action].d = PA[c.action].d + c.p * c.F;
				}
				FSA[c.action] += c.F;
			}
			for (int action : possibleActions) {
				if (FSA[action] != 0) {
					PA[action].d = PA[action].d / FSA[action];
				}
			}
		}

		private int selectAction(doubool[] PA) {
			if (rng.nextDouble() < pExplore) {
				int action = rng.nextInt(PA.length);
				while (!PA[action].isSet()) {
					action = rng.nextInt(PA.length);
				}
				return action;
			} else {
				int bestAction = -1;
				double best = Double.NEGATIVE_INFINITY;
				for (int action : possibleActions) {
					if (PA[action].isSet() && PA[action].d > best) {
						bestAction = action;
						best = PA[action].d;
					}
				}
				return bestAction;
			}
		}

		public void generateActionSet(int action) {
			actionSet.clear();
			for (Classifier c : matchSet) {
				if (c.action == action) {
					actionSet.add(c);
				}
			}
		}

		private void deleteFromPopulation() {
			int sumCn = 0;
			double sumF = 0.0;
			for (Classifier c : population) {
				sumCn += c.n;
				sumF += c.F;
			}
			if (sumCn <= N) {
				return;
			}
			double avgFit = sumF / sumCn;
			double voteSum = 0.0;
			for (Classifier c : population) {
				voteSum += deletionVote(c, avgFit);
			}

			double choiceP = rng.nextDouble() * voteSum;
			for (Classifier c : population) {
				voteSum += deletionVote(c, avgFit);
				if (voteSum > choiceP) {
					if (c.n > 1)
						c.n--;
					else
						population.remove(c);
					return;
				}
			}
		}

		public double deletionVote(Classifier c, double avgFit) {
			double vote = c.as * c.n;
			if (c.exp > delThresh && c.F / c.n < fracMeanFitness * avgFit)
				vote = vote * avgFit / (c.F / c.n);
			return vote;
		}

		private void updateSet(ArrayList<Classifier> actionSet, double P) {
			int sumCn = 0;
			for (Classifier c : actionSet) {
				sumCn += c.n;
			}
			for (Classifier c : actionSet) {
				c.exp++;
				// update prediction
				if (c.exp < 1.0 / lRate) {
					c.p = c.p + 1.0 / c.exp * (P - c.p);
					c.e = c.e + 1.0 / c.exp * (Math.abs(P - c.p) - c.e);
					c.as = c.as + 1.0 / c.exp * (sumCn - c.as);
				} else {
					c.p = c.p + lRate * (P - c.p);
					c.e = c.e + lRate * (Math.abs(P - c.p) - c.e);
					c.as = c.as + lRate * (sumCn - c.as);
				}
			}
			// #System.err.println("=================");
			// #System.err.println("++++Reward: "+ P);
			updateFitness(actionSet);
			// #System.err.println("==================");
			if (doASSubsumption) {
				doActionSetSubsumption(actionSet);
			}
		}

		public void updateFitness(ArrayList<Classifier> actionSet) {
			double sumAcc = 0.0;
			for (Classifier c : actionSet) {
				// #System.out.println("Payoff: "+ c.p);
				// #System.err.println("------------------");
				// #System.out.println("Check: "+ c.e + " < " + e0 + " == " +
				// (c.e < e0)+ " ?? " + alpha * Math.pow(c.e / e0, -v));
				c.accuracy = c.e < e0 ? 1.0 : alpha * Math.pow(c.e / e0, -v);
				sumAcc = sumAcc + c.accuracy * c.n;
			}
			for (Classifier c : actionSet) {
				c.F = c.F + lRate * (c.accuracy * c.n / sumAcc - c.F);
			}
		}

		public void doActionSetSubsumption(ArrayList<Classifier> actionSet) {
			Classifier cl = new Classifier();
			for (Classifier c : actionSet) {
				if (couldSubsume(c)) {
					if (cl.rule.length == 0 || countHash(c) > countHash(cl)
							|| (countHash(c) == countHash(cl) && rng.nextDouble() < 0.5)) {
						cl = new Classifier(c);
						// #System.out.println(c + " " + cl);
					}
				}
			}
			// #System.out.println(cl.rule + ", " + cl.p + ", " + cl.e + ", " +
			// cl.F + ", " + cl.accuracy + ", " + cl.n + ", " + cl.ts + ", " +
			// cl.action);
			if (cl.rule.length > 0) {
				for (Classifier c : actionSet) {
					if (isMoreGeneral(cl, c)) {
						cl.n = cl.n + c.n;
						// actionSet.remove(c);
						population.remove(c);
					}
				}
			}
		}

		public boolean couldSubsume(Classifier cl) {
			if (cl.exp > subThresh && cl.e < e0)
				return true;
			return false;
		}

		public boolean isMoreGeneral(Classifier clg, Classifier cls) {
			if (countHash(clg) <= countHash(cls))
				return false;
			int i = 0;
			do {
				if (clg.rule[i] != '#' && clg.rule[i] != cls.rule[i]) {
					return false;
				}
				i++;
			} while (i < clg.rule.length);
			return true;
		}

		public boolean doesSubsume(Classifier cls, Classifier clt) {
			if (cls.action == clt.action)
				if (couldSubsume(cls))
					if (isMoreGeneral(cls, clt))
						return true;
			return false;
		}

		public int countHash(Classifier cl) {
			int i = 0;
			for (char c : cl.rule)
				if (c == '#')
					i++;
			return i;
		}

		public void runGA(ArrayList<Classifier> actionSet, String rule) {
			double sumTs = 0.0;
			double sumN = 0.0;
			for (Classifier c : actionSet) {
				sumTs += c.ts * c.n;
				sumN += c.n;
			}
			double temp = sumTs / sumN;
			Classifier p1, p2, c1, c2;
			if (currentTime - temp > GA_threshold) {
				for (Classifier c : actionSet) {
					c.ts = currentTime;
					p1 = selectOffspring(actionSet);
					p2 = selectOffspring(actionSet);
					c1 = new Classifier(p1);
					c2 = new Classifier(p2);
					c1.n = 1;
					c2.n = 1;
					c1.exp = 0.0;
					c1.exp = 0.0;

					if (rng.nextDouble() < GA_cross) {
						applyCrossover(c1, c2);
						c1.p = (p1.p + p2.p) * 0.5;
						c1.e = (p1.e + p2.e) * 0.5;
						c1.F = (p1.F + p2.F) * 0.5;
						c2.p = c1.p;
						c2.e = c1.e;
						c2.F = c1.F;
					}

					c1.F = c1.F * 0.1;
					c2.F = c2.F * 0.1;

					applyMutation(c1, rule);
					applyMutation(c2, rule);
					if (doGASubsumption) {
						if (doesSubsume(p1, c1))
							p1.n++;
						else if (doesSubsume(p1, c1))
							p2.n++;
						else
							insertInPopulation(c1);
						if (doesSubsume(p1, c2))
							p1.n++;
						else if (doesSubsume(p1, c2))
							p2.n++;
						else
							insertInPopulation(c2);
					} else {
						insertInPopulation(c1);
						insertInPopulation(c2);
					}
					deleteFromPopulation();
					deleteFromPopulation();
				}
			}
		}

		public void applyCrossover(Classifier c1, Classifier c2) {
			// System.out.println(c1.rule.length);
			int x = rng.nextInt(c1.rule.length);
			int y = rng.nextInt(c2.rule.length);
			if (x > y) {
				int temp = x;
				x = y;
				y = temp;
			}
			int i = 0;
			char temp;
			do {
				if (x <= i && i < y) {
					temp = c1.rule[i];
					c1.rule[i] = c2.rule[i];
					c2.rule[i] = temp;
				}
				++i;
			} while (i < y);
		}

		public void applyMutation(Classifier c, String rule) {
			for (int i = 0; i < rule.length(); ++i) {
				if (rng.nextDouble() < GA_mutate) {
					if (c.rule[i] == '#')
						c.rule[i] = rule.charAt(i);
					else
						c.rule[i] = '#';
				}
			}
			if (rng.nextDouble() < GA_mutate) {
				c.action = possibleActions[rng.nextInt(possibleActions.length)];
			}
		}

		public Classifier selectOffspring(ArrayList<Classifier> actionSet) {
			double fitnessSum = 0.0;
			for (Classifier c : actionSet) {
				fitnessSum += c.F;
			}
			double choiceP = rng.nextDouble() * fitnessSum;
			fitnessSum = 0.0;
			for (Classifier c : actionSet) {
				fitnessSum += c.F;
				if (fitnessSum > choiceP) {
					return c;
				}
			}
			return actionSet.get(0);
		}

		public void insertInPopulation(Classifier cl) {
			for (Classifier c : population) {
				if (cl.equals(c)) {
					c.n++;
					return;
				}
			}
			population.add(cl);
		}
	}
}