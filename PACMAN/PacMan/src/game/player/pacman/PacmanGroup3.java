package game.player.pacman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import game.core.G;
import game.core.Game;
import gui.AbstractPlayer;

public final class PacmanGroup3 extends AbstractPlayer{
	
	@Override
	public String getGroupName() {
		return "Group3Pacman";
	}
	
	private String[] strDir = {"up", "right", "down", "left"};
	ArrayList<State> scoredStates = new ArrayList<State>();
	State availStates[] = {null, null, null, null};
	
	int currentScore = 0;
	int currentNumPills = 0; 
	
	double currentReinforcement, lastReinforcement;
	
	double lastScore = 0.0;
	double Score = 0.0;
	
	int minDist = 20; 
	
	int[] activePills;
	int[] activePowerPills;
	
	State lastState = null;
	XCS xcs = null;
	
	class State implements Comparable<State> {
	    public int[] state; 
	    public double payoff;
	    public double error;
	    public double fitness;
	    public double score;
	    
	    public State (int[] state){
	    	this.state = state.clone();
	    	//score = rng.nextDouble()*1000;
	    	score = 0.0;
	    }
	    
		@Override
		public int compareTo(State o) {
	        if (this.score > o.score)
	            return 1; // highest value first
	        if (this.score== o.score)
	            return 0;
	        return -1;
		}

	};
	
	@Override
	public int getAction(Game game, long timeDue){
		//System.out.println(timeDue);
	
		int currentLoc = game.getCurPacManLoc();
		
		currentScore -= 1;
		if (currentNumPills == 0) {
			currentNumPills = game.getNumberPills();
		}
		if (currentNumPills != game.getNumActivePills() ) {
			currentScore += 10*(currentNumPills - game.getNumActivePills());
		}
				
		if (xcs == null) {
			System.out.println("murp");
			xcs = new XCS();
		}
		else {
			/*if (currentLoc > -1) {
				xcs.setReward(possibleReward(currentLoc, game));
			}*/
			Score = (game.getScore() + game.getLivesRemaining()*500);
			//System.out.println(Score - lastScore);
			//if (Score != lastScore)
				//xcs.setReward(Score - lastScore);
			xcs.setReward(possibleReward(currentLoc, game));
			lastScore = currentScore;
		}
		
		activePills=game.getPillIndicesActive();
		activePowerPills=game.getPillIndicesActive();
		
		int move = 0;
		if (currentLoc > -1) {
			String state = getStateArray(currentLoc, game);
			//System.out.println(state);
			move = xcs.getAction(state, timeDue);
		}
		
		
		/*for( Classifier p : xcs.population) {
			System.out.println(p.rule+":"+p.action+"="+p.p+" e:"+p.e+" p.F"+p.F+" p.as"+p.as);
		}
		System.out.println("ööööööööööööööööööööööööööööööööööö");
		*/
		return move;
	}

	
	private double possibleReward(int index, Game game) {
		double q = 0.0f;
		int score = game.getScore();
		int remainingLives = game.getLivesRemaining();
		
		int nearestPillIndex = game.getTarget(index,activePills,true,G.DM.PATH);
		int pillDist = game.getPathDistance(index, nearestPillIndex);
		
		int nearestPowerIndex = game.getTarget(index,activePowerPills,true,G.DM.PATH);
		int powerPillDist = game.getPathDistance(index, nearestPowerIndex);
		
		int nearestJunction = game.getTarget(index,game.getJunctionIndices(),true,G.DM.PATH);
		int nearJuncDist = game.getPathDistance(index, nearestPowerIndex);
		
		/*double reward = 15.0 * 1.0/(1 + pillDist);// + 200.0 * 1.0/(1 + 2*powerPillDist );

		double loss = 0.0;
		for ( int i = 0; i < 4; ++i) {
			if (game.isEdible(i)) {
				int dist = game.getPathDistance(index, game.getCurGhostLoc(i));
				if (dist != -1) {	
					reward += 200.0 * game.getEdibleTime(i)/Math.pow((1 + dist),2);
				}
			}
			else {
				//int juncDist = game.getGhostPathDistance(i, nearestJunction);	
				int dist = game.getGhostPathDistance(i, index);				
				loss += 50.0 * 1.0/(1 + dist);
			}
		}		
		
		q = reward - loss;*/
				
		//int[] dist = new int[4];
		int min = Integer.MAX_VALUE; 
		for ( int i = 0; i < 4; ++i) {
			int dist = game.getGhostPathDistance(i, index);
			if (dist == 0)
				continue;
			if (min > dist) {
				min = dist;
				q = 200 * (1 + min);
			}
		}
		
		return q; 
	}
	
	
	private String getStateArray(int index, Game game) {
		final int maxDist = (int) (Math.pow(2, 4) - 1);
		String state = "";

		for ( int i = 0; i < 4; ++i) {
			//int dist = game.getGhostPathDistance(index, game.getCurGhostLoc(i));
			int dist = game.getPathDistance(index, game.getCurGhostLoc(i));
			int direction = game.getReverse(game.getCurGhostDir(i));
			int[] path = game.getGhostPath(i, index);
			if(dist >= 0){//&& dist < maxDist) {
				//state += String.format("%4s", Integer.toBinaryString(dist)).replace(" ", "0");
				state += String.format("%2s", Integer.toBinaryString(direction)).replace(" ", "0");
				//System.out.println("Ghost#"+i+" is going "+strDir[direction]);
			}
			else {
				state += "##";
			}
			state += game.isEdible(i) ? 1 : 0;
		}

		int[] dirArr =  {0, 0, 0, 0};
		int[] posDir = game.getPossiblePacManDirs(false);
		for (int i = 0; i < 4; ++i)
			for (int j : posDir)
				if (i == j)
					dirArr[i] = 1;
		for (int i : dirArr)
			state += i;
		
		/*int[] nb = game.getPacManNeighbours();
		for (int j : nb)
			System.out.print(j+",");
		System.out.print("\n");
		
		for (int j : posDir)
			System.out.println(game.getNeighbour(index, j));
		*/
		
		int nearestPillIndex = game.getTarget(index, activePills, true, G.DM.PATH);
		int nearestPowerIndex = game.getTarget(index, activePowerPills, true, G.DM.PATH);
		
		if (nearestPillIndex == -1) 
			nearestPillIndex = nearestPowerIndex;

		String temp = "##";
		int pillDist = game.getPathDistance(index, nearestPillIndex);
		System.err.println("Pill:" + pillDist);
		for (int j : posDir) {
			int next = game.getNeighbour(index, j);
			int nextDist = game.getPathDistance(next, nearestPillIndex);
			System.out.println("Next:" + nextDist);
			if (nextDist <= pillDist) {
				temp = String.format("%2s", Integer.toBinaryString(j)).replace(" ", "0");
				break;
			}
		}
		
		temp = "##";
		if (nearestPowerIndex  == -1) {
			temp = "##";
		}
		else {
			int powerPillDist = game.getPathDistance(index, nearestPowerIndex);
			
			for (int j : posDir) {
				int next = game.getNeighbour(index, j);
				int nextDist = game.getPathDistance(next, nearestPowerIndex);
				if (nextDist <= powerPillDist ) {
					temp = String.format("%2s", Integer.toBinaryString(j)).replace(" ", "0");
					break; 
				}
			}
		}
		state += temp;

		return state;
	}
	
	class Classifier {
		String rule = ""; 
		double p; //payoff
		double e; //error
		double F; //fitness
		
		double as = 1; //actionSet occurence
		double ts = 0.0; //timestep 
		double accuracy;
		double exp = 0; //experience
		int n = 1; 
		
		int action;
		
		double pI = 0.01;
		double eI = 0.01;
		double FI = 0.01;

		public Classifier(){
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
			this.rule = o.rule;
		}
		
		public boolean equals(String o) {
			if (this.rule.length() != o.length())
				System.err.println("Error1: "+this.rule.length()+" vs "+o.length()+"-- "+this.rule+" || "+o);
			for (int i = 0; i < rule.length(); ++i ) {
				if ( rule.charAt(i) != '#' && rule.charAt(i) != o.charAt(i) )
					return false;
			}
	        return true;
		}
		
		public boolean equals(Classifier o) {
			boolean sameRule = true;
			for (int i = 0; i < rule.length(); ++i ) {
				if ( rule.charAt(i) != o.rule.charAt(i) ) {
					sameRule = false;
					break;
				}					
			}
			if (this.action == o.action && sameRule) {
				return true;
			}
			else {
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
		
		int[] possibleActions = {0, 1, 2, 3};
		
		//maximum population size
		int N = 500;
		
		//used for GA
		double GA_threshold = 35;
		double GA_cross = 0.75;
		double GA_mutate = 0.03;
		
		//used for multistep
		double discount = 0.71;
		
		//
		double delThresh = 20;
		double fracMeanFitness = 0.1;
		double subThresh = 20;
		
		double pHash = 0.33;
		double pExplore = 0.05;
		
		double mnaThresh = 4;
		
		//used for fitness
		double e0 = 2; //thresholdError
		double alpha = 0.1;
		double v = 5;
		
		double threshold;
		double lRate = 0.3;
		
		String rule = "";
		String lastRule = "";
		
		boolean doASSubsumption = false;
		boolean doGASubsumption = false;
		
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
		
		public void setReward(double rp) {
			reward = rp; //reinforcement
			if (!lastActionSet.isEmpty()) {
				double best = Double.NEGATIVE_INFINITY;
				for(doubool p : PA) {
					if (p.isSet() && p.d > best){
						best = p.d;
					}
				}
				double P = lastReward + discount * best;
				updateSet(lastActionSet, P);
				runGA(lastActionSet, lastRule);
			}
			if (false) {
				
			}
			else {
				lastActionSet = (ArrayList<Classifier>) actionSet.clone();
				lastReward = reward;
				lastRule = rule;
			}
		}
		
		private void generateMatchingSet(String rule) {
			matchSet.clear();
			while (matchSet.isEmpty()) {
				HashSet<Integer> actions = new HashSet<Integer>();
				//System.err.println("~~~~~~~~~~~");
				//System.out.println(rule);
				//System.out.println("----------");
				for (Classifier c : population) {
					if (c.equals(rule)) {
						matchSet.add(c);
						actions.add(c.action);
					}
				}
				//System.err.println("~~~~~~~~~~~");
				if (actions.size() < mnaThresh) {
					Classifier cl = covering(rule, actions);
					population.add(cl);
					deleteFromPopulation();
					matchSet.clear();
				}
			}
		}
		
		private Classifier covering(String rule, HashSet<Integer> actions){
			Classifier cl = new Classifier();
			for (int i = 0; i < rule.length(); ++i ) {
				cl.rule += rng.nextDouble() < pHash ? "#" : rule.charAt(i);
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
				//System.err.println(p);
				p.b = false;
			}
			double FSA[] = new double[possibleActions.length];
			for (Classifier c : matchSet) {
				if (!PA[c.action].isSet()) {
					PA[c.action].set();
					PA[c.action].d = c.p * c.F;
				}
				else {
					PA[c.action].d = PA[c.action].d + c.p * c.F;
				}
				FSA[c.action] += c.F;
			}
			for(int action : possibleActions) {
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
			}
			else {
				int bestAction = -1;
				double best = Double.NEGATIVE_INFINITY;
				for(int action : possibleActions) {
					if (PA[action].isSet() && PA[action].d > best){
						bestAction = action;
						best = PA[action].d;
					}
				}
				return bestAction;
			}
		}
		
		public void generateActionSet(int action){
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
			//System.out.println("popSize"+population.size());
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
			//System.out.println("<<popSize"+population.size()+"<<");
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
				//update prediction
				if (c.exp < 1.0 / lRate) {
					c.p = c.p + 1.0 / c.exp * ( P - c.p );
					c.e = c.e + 1.0 / c.exp * (Math.abs(P - c.p) - c.e);
					c.as = c.as + 1.0 / c.exp * (sumCn - c.as);
				}
				else {
					c.p = c.p  + lRate * ( P - c.p );
					c.e = c.e + lRate * (Math.abs(P - c.p) - c.e);
					c.as = c.as + lRate * (sumCn - c.as);
				}
			}
			updateFitness(actionSet);
			if (doASSubsumption) {
				doActionSetSubsumption();
			}
		}
		
		public void updateFitness(ArrayList<Classifier> actionSet) {
			double sumAcc = 0.0;
			for (Classifier c : actionSet) {
				c.accuracy = c.e < e0 ? 1.0 : alpha * Math.pow(c.e / e0, -v);
				sumAcc = sumAcc + c.accuracy * c.n;
			}
			for (Classifier c : actionSet) {
				c.F = c.F + lRate * (c.accuracy * c.n / sumAcc - c.F);
			}
		}
		
		public void doActionSetSubsumption() {
			
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
			//System.out.println("GA: "+actionSet.size());
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
						
					}
					else {
						insertInPopulation(c1);
						insertInPopulation(c2);
					}
					deleteFromPopulation();
					deleteFromPopulation();
				}
			}
		}
		
		public void applyCrossover(Classifier c1, Classifier c2) {
			int x = rng.nextInt(c1.rule.length());
			int y = rng.nextInt(c2.rule.length());
			char[] char1 = c1.rule.toCharArray();
			char[] char2 = c2.rule.toCharArray();
			if ( x > y ) {
				int temp = x;
				x = y;
				y = x;
			}
			int i = 0;
			char temp; 
			do {
				if (x <= i && i < y) {
					temp = char1[i];
					char1[i] = char2[i];
					char2[i] = temp;
				}
				++i;
			}
			while(i < y);
			c1.rule = new String(char1);
			c2.rule = new String(char2);
		}
		
		public void applyMutation(Classifier c, String rule) {
		    char[] charArray = c.rule.toCharArray();
			for (int i = 0; i < rule.length(); ++i) {
				if(rng.nextDouble() < GA_mutate) {
					if (charArray[i] == '#')
						charArray[i] = rule.charAt(i);
					else
						charArray[i] = '#';
				}
			}
			if(rng.nextDouble() < GA_mutate) {
				c.action = possibleActions[rng.nextInt(possibleActions.length)];
			}
			c.rule = new String(charArray);
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
		
		public void insertInPopulation(Classifier cl){
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