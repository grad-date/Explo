package HUIM_GA;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HUIM_GA {
	
	double maxMemory = 0;
	long startTimestamp = 0;
	long endTimestamp = 0;
	final int pop_size = 20;
	final int iterations = 10000;
	Map<Integer, Integer> mapItemToTWU;
	Map<Integer, Integer> mapItemToTWU0;
	List<Integer> twuPattern;
	BufferedWriter writer = null;


	class Pair {
		int item = 0;
		int utility = 0;
	}


	class ChroNode {
		List<Integer> chromosome;
		int fitness;
		double rfitness;
		int rank;

		public ChroNode() {
			chromosome = new ArrayList<Integer>();
		}

		public ChroNode(int length) {
			chromosome = new ArrayList<Integer>();
			for (int i = 0; i < length; i++) {
				chromosome.add(i, 0);
			}
		}
	}


	class HUI {
		String itemset;
		int fitness;

		public HUI(String itemset, int fitness) {
			super();
			this.itemset = itemset;
			this.fitness = fitness;
		}
	}


	List<ChroNode> population = new ArrayList<ChroNode>();
	List<ChroNode> subPopulation = new ArrayList<ChroNode>();
	List<HUI> huiSets = new ArrayList<HUI>();
	
	List<List<Pair>> database = new ArrayList<List<Pair>>();
	List<Double> percentage = new ArrayList<Double>();


	public HUIM_GA() {
	}


	public void runAlgorithm(String input, String output, int minUtility) throws IOException {

		maxMemory = 0;

		startTimestamp = System.currentTimeMillis();

		writer = new BufferedWriter(new FileWriter(output));

		mapItemToTWU = new HashMap<Integer, Integer>();
		mapItemToTWU0 = new HashMap<Integer, Integer>();

		BufferedReader myInput = null;
		String thisLine;
		try {
			
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
			while ((thisLine = myInput.readLine()) != null) {
				
				if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
						|| thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}

				String split[] = thisLine.split(":");
				String items[] = split[0].split(" ");
				int transactionUtility = Integer.parseInt(split[1]);
				
				for (int i = 0; i < items.length; i++) {
					Integer item = Integer.parseInt(items[i]);
					Integer twu = mapItemToTWU.get(item);
					Integer twu0 = mapItemToTWU0.get(item);
					twu = (twu == null) ? transactionUtility : twu + transactionUtility;
					twu0 = (twu0 == null) ? transactionUtility : twu0 + transactionUtility;
					mapItemToTWU.put(item, twu);
					mapItemToTWU0.put(item, twu0);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}

		try {
			
			myInput = new BufferedReader(new InputStreamReader(new FileInputStream(new File(input))));
					
			while ((thisLine = myInput.readLine()) != null) {
				
				if (thisLine.isEmpty() == true || thisLine.charAt(0) == '#'
						|| thisLine.charAt(0) == '%'
						|| thisLine.charAt(0) == '@') {
					continue;
				}

				String split[] = thisLine.split(":");
				String items[] = split[0].split(" ");
				String utilityValues[] = split[2].split(" ");

				List<Pair> revisedTransaction = new ArrayList<Pair>();
				List<Integer> pattern = new ArrayList<Integer>();
				
				for (int i = 0; i < items.length; i++) {
					Pair pair = new Pair();
					pair.item = Integer.parseInt(items[i]);
					pair.utility = Integer.parseInt(utilityValues[i]);
					
					if (mapItemToTWU.get(pair.item) >= minUtility) {
						revisedTransaction.add(pair);
						pattern.add(pair.item);
					}else{
						mapItemToTWU0.remove(pair.item);
					}
				}
				
				database.add(revisedTransaction);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (myInput != null) {
				myInput.close();
			}
		}
		
		twuPattern = new ArrayList<Integer>(mapItemToTWU0.keySet());
		Collections.sort(twuPattern);
		
		checkMemory();

		if (twuPattern.size() > 0) {
			double pMax, pMin;
			int m = database.size();
			int n = twuPattern.size();
			int temp1 = 0, temp2 = 0;
			generatePop(minUtility);
			if (m > n) {
				pMin = 1 / (m + 0.0);
				pMax = 1 / (n + 0.0);
			} else {
				pMin = 1 / (n + 0.0);
				pMax = 1 / (m + 0.0);
			}

			for (int i = 0; i < iterations; i++) {
				calculateRfitness();

				while (subPopulation.size() < pop_size) {
					temp1 = selectChromosome();
					temp2 = selectChromosome();
					while (temp1 == temp2) {
						temp2 = selectChromosome();
					}
					crossover(temp1, temp2, minUtility);
				}
				
				subPopulation = rankedMutation(pMax, pMin, i, minUtility);
				
				subPopulation.addAll(population);
				rankData(subPopulation);
				for (int j = 0; j < population.size(); j++) {
					population.set(j, subPopulation.get(j));
				}
				subPopulation.clear();
			}
		}

		writeOut();
		checkMemory();
		writer.close();
		endTimestamp = System.currentTimeMillis();
	}


	private void generatePop(int minUtility)//
	{
		int i = 0, j, k, temp;
		percentage = roulettePercent();

		while (i < pop_size) {
			
			ChroNode tempNode = new ChroNode(twuPattern.size());
			j = 0;
			k = (int) (Math.random() * twuPattern.size());

			while (j < k) {
				temp = select(percentage);
				if (tempNode.chromosome.get(temp) == 0) {
					j++;
					tempNode.chromosome.set(temp, 1);
				}

			}

			tempNode.fitness = fitCalculate(tempNode.chromosome, k);
			tempNode.rank = 0;
			population.add(tempNode);
			if (tempNode.fitness >= minUtility) {
				insert(tempNode);
			}
			i++;
		}
	}


	private List<Double> roulettePercent() {
		int i, sum = 0, tempSum = 0;
		double tempPercent;

		for (i = 0; i < twuPattern.size(); i++) {
			sum = sum + mapItemToTWU.get(twuPattern.get(i));
		}

		for (i = 0; i < twuPattern.size(); i++) {
			tempSum = tempSum + mapItemToTWU.get(twuPattern.get(i));
			tempPercent = tempSum / (sum + 0.0);
			percentage.add(tempPercent);
		}
		return percentage;
	}


	private int select(List<Double> percentage) {
		int i, temp = 0;
		double randNum;
		randNum = Math.random();
		for (i = 0; i < percentage.size(); i++) {
			if (i == 0) {
				if ((randNum >= 0) && (randNum <= percentage.get(0))) {
					temp = 0;
					break;
				}
			} else if ((randNum > percentage.get(i - 1))
					&& (randNum <= percentage.get(i))) {
				temp = i;
				break;
			}
		}
		return temp;
	}


	private void crossover(int temp1, int temp2, int minUtility) {
		int i = 0;
		int tempA = 0, tempB = 0;
		List<Integer> temp1Chro = new ArrayList<Integer>();
		List<Integer> temp2Chro = new ArrayList<Integer>();
		ChroNode tempNode = new ChroNode();
		int position = (int) (Math.random() * twuPattern.size());

		for (i = 0; i < twuPattern.size(); i++) {
			if (i <= position) {
				temp1Chro.add(population.get(temp2).chromosome.get(i));
				if (temp1Chro.get(i) == 1)
					tempA++;
				temp2Chro.add(population.get(temp1).chromosome.get(i));
				if (temp2Chro.get(i) == 1)
					tempB++;
			} else {
				temp1Chro.add(population.get(temp1).chromosome.get(i));
				if (temp1Chro.get(i) == 1)
					tempA++;
				temp2Chro.add(population.get(temp2).chromosome.get(i));
				if (temp2Chro.get(i) == 1)
					tempB++;
			}
		}

		tempNode.chromosome = temp1Chro;
		tempNode.fitness = fitCalculate(temp1Chro, tempA);
		tempNode.rank = 0;
		subPopulation.add(tempNode);
		if (tempNode.fitness >= minUtility) {
			insert(tempNode);
		}

		tempNode.chromosome = temp2Chro;
		tempNode.fitness = fitCalculate(temp2Chro, tempB);
		tempNode.rank = 0;
		subPopulation.add(tempNode);
		if (tempNode.fitness >= minUtility) {
			insert(tempNode);
		}
	}


	private void rankData(List<ChroNode> tempPop) {
		int i, j, p, q, temp;

		for (i = 0; i < tempPop.size() - 1; i++) {
			p = i;
			for (j = i + 1; j < tempPop.size(); j++) {
				if (tempPop.get(p).fitness < tempPop.get(j).fitness)
					p = j;
			}

			if (i != p) {
				temp = tempPop.get(i).fitness;
				tempPop.get(i).fitness = tempPop.get(p).fitness;
				tempPop.get(p).fitness = temp;
				for (q = 0; q < twuPattern.size(); q++) {
					temp = tempPop.get(i).chromosome.get(q);
					tempPop.get(i).chromosome.set(q,
							tempPop.get(p).chromosome.get(q));
					tempPop.get(p).chromosome.set(q, temp);
				}
			}
			tempPop.get(i).rank = i + 1;
		}
		tempPop.get(i).rank = i + 1;
	}


	private List<Integer> getRank() {
		int i, j;
		List<Integer> rank = new ArrayList<Integer>();
		for (i = 0; i < subPopulation.size(); i++) {
			int temp = 0;
			for (j = 0; j < subPopulation.size(); j++) {
				if (i != j) {
					if (subPopulation.get(i).fitness <= subPopulation.get(j).fitness) {
						temp++;
					}
				}
			}
			rank.add(temp + 1);
		}
		return rank;
	}


	private List<ChroNode> rankedMutation(double pMax, double pMin, int currentIteration, int minUtility) {
		double pm, rankNum;
		List<Integer> record = getRank();

		for (int i = 0; i < pop_size; i++) {
			pm = (pMax - (pMax - pMin) * currentIteration / iterations) * record.get(i) / subPopulation.size();
			rankNum = Math.random();
			
			if (rankNum < pm) {
				int temp = (int) (Math.random() * twuPattern.size());
				if (subPopulation.get(i).chromosome.get(temp) == 1) {
					subPopulation.get(i).chromosome.set(temp, 0);
				} else {
					subPopulation.get(i).chromosome.set(temp, 1);
				}

				int k = 0;
				for (int j = 0; j < twuPattern.size(); j++) {
					if (subPopulation.get(i).chromosome.get(j) == 1) {
						k++;
					}
				}
				subPopulation.get(i).fitness = fitCalculate(subPopulation.get(i).chromosome, k);

				if (subPopulation.get(i).fitness >= minUtility) {
					insert(subPopulation.get(i));
				}
			}
		}
		return subPopulation;
	}


	private void insert(ChroNode tempChroNode) {
		int i;
		StringBuilder temp = new StringBuilder();
		for (i = 0; i < twuPattern.size(); i++) {
			if (tempChroNode.chromosome.get(i) == 1) {
				temp.append(twuPattern.get(i));
				temp.append(' ');
			}
		}
		
		if (huiSets.size() == 0) {
			huiSets.add(new HUI(temp.toString(), tempChroNode.fitness));
		} else {
			for (i = 0; i < huiSets.size(); i++) {
				if (temp.toString().equals(huiSets.get(i).itemset)) {
					break;
				}
			}
			if (i == huiSets.size())
				huiSets.add(new HUI(temp.toString(), tempChroNode.fitness));
		}
	}


	private int fitCalculate(List<Integer> tempChroNode, int k) {
		if (k == 0)
			return 0;
		int i, j, p, q, temp;

		int sum, fitness = 0;
		for (p = 0; p < database.size(); p++) {
			i = 0;
			j = 0;
			q = 0;
			temp = 0;
			sum = 0;
			
			while (j < k && q < database.get(p).size() && i < tempChroNode.size()) {
				if (tempChroNode.get(i) == 1) {
					if (database.get(p).get(q).item < twuPattern.get(i))
						q++;
					else if (database.get(p).get(q).item == twuPattern.get(i)) {
						sum = sum + database.get(p).get(q).utility;
						j++;
						q++;
						temp++;
						i++;
					} else if (database.get(p).get(q).item > twuPattern.get(i)) {
						j++;
						i++;
					}
				} else
					i++;
			}
			if (temp == k) {
				fitness = fitness + sum;
			}
		}
		return fitness;
	}
	
	
	public void calculateRfitness(){
		int sum=0;
		int temp=0;
		
		for(int i=0; i< population.size();++i){
			sum =sum+population.get(i).fitness;
		}
		
		for(int i=0; i< population.size();++i){
			temp = temp + population.get(i).fitness;
			population.get(i).rfitness = temp/(sum+0.0);
		}
	}
	
	
	private int selectChromosome() {
		int i, temp = 0;
		double randNum;
		randNum = Math.random();
		for (i = 0; i < population.size(); i++) {
			if (i == 0) {
				if ((randNum >= 0) && (randNum <= population.get(0).rfitness)) {
					temp = 0;
					break;
				}
			} else if ((randNum > population.get(i - 1).rfitness) && (randNum <= population.get(i).rfitness)) {
				temp = i;
				break;
			}
		}
		return temp;
	}
	

	private void writeOut() throws IOException {
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < huiSets.size(); i++) {
			buffer.append(huiSets.get(i).itemset);
			buffer.append("#UTIL: ");
			buffer.append(huiSets.get(i).fitness);
			buffer.append(System.lineSeparator());
		}
		writer.write(buffer.toString());
		writer.newLine();
	}


	private void checkMemory() {
		double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024d / 1024d;
		if (currentMemory > maxMemory) {
			maxMemory = currentMemory;
		}
	}


	public void printStats() {
		System.out.println("=============  HUIM-GA ALGORITHM STATS =============");
		System.out.println(" Total time: " + (endTimestamp - startTimestamp) + " ms");
		System.out.println(" Memory: " + maxMemory + " MB");
		System.out.println(" High-utility itemsets count: " + huiSets.size());
		System.out.println("===================================================");
	}
}
