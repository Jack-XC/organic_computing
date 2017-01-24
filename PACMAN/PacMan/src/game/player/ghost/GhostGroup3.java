package game.player.ghost;

import java.util.ArrayList;

import game.core.G;
import game.core.Game;
import gui.AbstractGhost;

public class GhostGroup3 extends AbstractGhost {

	private int pacParam = 10;
	private int ghostParam = 1000;
	private int fleeParam = 500;

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

}
