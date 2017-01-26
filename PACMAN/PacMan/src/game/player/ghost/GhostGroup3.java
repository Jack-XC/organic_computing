package game.player.ghost;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import game.core.G;
import game.core.Game;
import gui.AbstractGhost;
import game.player.ghost.ghostgroup3.*;

public class GhostGroup3 extends AbstractGhost {

	public int savedScore = -1;
	// Score,PacParam,GhostParam,FleeParam
	// Best Values Random 1000: ++++ 1380,578,0,641 ++++
	// Best Values Random 100:  ++++ 1440,3,0,117 ++++
	private int pacParam = 3;
	private int ghostParam = 0;
	private int fleeParam = 117;
	
	private String bestValues = "NONE";
	
	private int counter = -1;
	private String[] selected = new String[10];
	private String[] population = new String[100];
	private boolean learning = false;
	
	public GhostGroup3(){
		if(learning){
			for(int i=0;i<population.length;++i){
				pacParam = ThreadLocalRandom.current().nextInt(0, 100);
				ghostParam = ThreadLocalRandom.current().nextInt(0, 100);
				fleeParam = ThreadLocalRandom.current().nextInt(0, 100);
				population[i] = "NONE," + pacParam + "," + ghostParam + "," + fleeParam;
			}
			pacParam = Integer.parseInt(population[0].split(",")[1]);
			ghostParam = Integer.parseInt(population[0].split(",")[2]);
			fleeParam = Integer.parseInt(population[0].split(",")[3]);
			counter = 0;
		}
	}

	@Override
	public int[] getActions(Game game, long timeDue) {
		int[] directions = new int[Game.NUM_GHOSTS];

		for (int i = 0; i < directions.length; i++) {
			if (game.ghostRequiresAction(i)) {
				int[] possibleDirs = game.getPossibleGhostDirs(i);
				int pacPos = game.getCurPacManLoc();
				int[] scores = new int[possibleDirs.length];
				
				for (int d = 0; d < possibleDirs.length; d++) {
					int startPos = game.getNeighbour(game.getCurGhostLoc(i), possibleDirs[d]);
					int[] ghostPath = getGhostPath(game, startPos, pacPos, possibleDirs[d]);
					/*
					int projectedPos = startPos;
					GameView.addPoints(game, Color.CYAN, ghostPath);

					for (int tile = 0; tile < ghostPath.length; tile++) {
						if (game.isJunction(tile)) {
							projectedPos = tile;
							break;
						}
					}
					*/
					int dist = game.getPathDistance(startPos, pacPos);
					if (dist > fleeParam || !game.isEdible(i)) {
						scores[d] = pacParam * dist;
					} else {
						scores[d] = -pacParam * dist;
					}
					for (int tile = 0; tile < ghostPath.length; tile++) {
						scores[d] += ghostParam;
					}
				}
				directions[i] = possibleDirs[findSmallestIndex(scores)];
			}
		}
		savedScore = game.getScore();
		return directions;

	}

	@Override
	public String getGhostGroupName() {
		return "GeneticGhost - Group3";
	}

	private int findSmallestIndex(int array[]) {
		int smallest = array[0];
		int smallestIndex = 0;

		for (int i = 0; i < array.length; i++) {
			if (array[i] < smallest) {
				smallest = array[i];
				smallestIndex = i;
			}
		}

		return smallestIndex;
	}

	private int[] getGhostPath(Game game, int from, int to, int dir) {

		ArrayList<Integer> path = new ArrayList<Integer>();
		int lastDir = dir;
		int currentNode = from;

		while (currentNode != to) {
			path.add(currentNode);
			int[] neighbours = getGhostNeighbours(game, currentNode, lastDir);
			lastDir = getNextDir(game, neighbours, to, true, G.DM.PATH);
			currentNode = neighbours[lastDir];
		}

		int[] arrayPath = new int[path.size()];

		for (int i = 0; i < arrayPath.length; i++)
			arrayPath[i] = path.get(i);

		return arrayPath;
	}

	private int[] getGhostNeighbours(Game game, int node, int lastDir) {
		ArrayList<Integer> n = new ArrayList<Integer>();

		for (int i = 0; i < 4; i++) {
			int neighbour = game.getNeighbour(node, i);
			if (game.getReverse(lastDir) != i) {
				n.add(neighbour);
			} else {
				n.add(-1);
			}
		}

		int[] neighbours = new int[n.size()];

		for (int i = 0; i < neighbours.length; i++)
			neighbours[i] = n.get(i);

		return neighbours;
	}

	private int getNextDir(Game game, int[] from, int to, boolean closer, Game.DM measure) {
		int dir = -1;

		double min = Integer.MAX_VALUE;
		double max = -Integer.MAX_VALUE;

		for (int i = 0; i < from.length; i++) {
			if (from[i] != -1) {
				double dist = 0;

				switch (measure) {
				case PATH:
					dist = game.getPathDistance(from[i], to);
					break;
				case EUCLID:
					dist = game.getEuclideanDistance(from[i], to);
					break;
				case MANHATTEN:
					dist = game.getManhattenDistance(from[i], to);
					break;
				}

				if (closer && dist < min) {
					min = dist;
					dir = i;
				}

				if (!closer && dist > max) {
					max = dist;
					dir = i;
				}
			}
		}

		return dir;
	}
	
	public void learn(){
		if(counter < population.length - 1){
			population[counter] = savedScore + "," + pacParam + "," + ghostParam + "," + fleeParam; 
			//System.out.println("DEBUG: Score: "+savedScore + "," + pacParam + "," + ghostParam + "," + fleeParam);
			++counter;
			pacParam = Integer.parseInt(population[counter].split(",")[1]);
			ghostParam = Integer.parseInt(population[counter].split(",")[2]);
			fleeParam = Integer.parseInt(population[counter].split(",")[3]);
		} else {
			counter = 0;
			SortScores.mergeSort(population);
			for(int i=0;i<10;++i){
				selected[i] = population[i];
			}
			checkBest(selected[0]);
			System.out.println("BestValues: "+bestValues);
			//TODO saveBestSolutionToFile
			makeChildren();
		}
	}
	
	private void makeChildren(){
		String[] nPopulation = new String[100];
		for(int i=0;i<nPopulation.length;++i){
			int ran1 = ThreadLocalRandom.current().nextInt(0, 10);
			int ran2 = ThreadLocalRandom.current().nextInt(0, 10);
			while(ran1 == ran2)
				ran2 = ThreadLocalRandom.current().nextInt(0, 10);
			String[] ran1split = selected[ran1].split(",");
			String[] ran2split = selected[ran2].split(",");
			int[] nPacParam = crossover(Integer.parseInt(ran1split[1]), Integer.parseInt(ran2split[1]));
			int[] nGhostParam = crossover(Integer.parseInt(ran1split[2]), Integer.parseInt(ran2split[2]));
			int[] nFleeParam = crossover(Integer.parseInt(ran1split[3]), Integer.parseInt(ran2split[3]));
			nPopulation[i] = "NONE," + nPacParam[0] + "," + nGhostParam[0] + "," + nFleeParam[0];
			++i;
			nPopulation[i] = "NONE," + nPacParam[1] + "," + nGhostParam[1] + "," + nFleeParam[1];
		}
		population = nPopulation;
		
	}
	
	public void saveToFile(){
		//TODO
	}
	
	private int[] crossover(int x, int y){
	    int s = ThreadLocalRandom.current().nextInt(0, Short.MAX_VALUE);
	    int[] result = new int[2];
	    result[0] = (x & s) + (y & ~s);
	    result[1] = (x & ~s) + (y & s);
	    return result;
	}
	
	private void checkBest(String nBest){
		bestValues = checkBest(bestValues, nBest);
	}
	
	public String checkBest(String oldBest, String nBest){
		if(nBest.equals("NONE"))
			return oldBest;
		if(oldBest.equals("NONE")){
			oldBest = nBest;
		} else{
			String[] bestValuesSplit = oldBest.split(",");
			String[] nBestSplit = nBest.split(",");
			if(Integer.parseInt(bestValuesSplit[0]) > Integer.parseInt(nBestSplit[0])){
				oldBest = nBest;
			}
		}
		return oldBest;
	}
	
	public String getBest(){
		return bestValues;
	}

}
