package HUIM_BPSO;

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


public class HUIM_BPSO {
	
	double maxMemory = 0;
	long startTimestamp = 0;
	long endTimestamp = 0;
	final int pop_size = 20;
	final int iterations = 10000;
	final int c1 = 2, c2 = 2;

	Map<Integer, Integer> mapItemToTWU;
	Map<Integer, Integer> mapItemToTWU0;
	
	List<Integer> twuPattern;

	BufferedWriter writer = null;


	class Pair {
		int item = 0;
		int utility = 0;
	}


	class Particle {
		List<Integer> X;
		int fitness;

		public Particle() {
			X = new ArrayList<Integer>();
		}

		public Particle(int length) {
			X = new ArrayList<Integer>();
			for (int i = 0; i < length; i++) {
				X.add(i, 0);
			}
		}
		
		public void copyParticle(Particle particle1){
			for(int i=0;i<particle1.X.size();++i){
				this.X.set(i, particle1.X.get(i).intValue());
			}
			this.fitness=particle1.fitness;
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


	Particle gBest;
	List<Particle> pBest = new ArrayList<Particle>();
	List<Particle> population = new ArrayList<Particle>();
	List<HUI> huiSets = new ArrayList<HUI>();
	List<List<Double>> V = new ArrayList<List<Double>>();
	List<Double> percentage = new ArrayList<Double>();

	List<List<Pair>> database = new ArrayList<List<Pair>>();


	public HUIM_BPSO() {
	}

	
	public void runAlgorithm(String input, String output, int minUtility)
			throws IOException {
		
		maxMemory = 0;

		startTimestamp = System.currentTimeMillis();

		writer = new BufferedWriter(new FileWriter(output));

		mapItemToTWU = new HashMap<Integer, Integer>();
		mapItemToTWU0 = new HashMap<Integer, Integer>();

		BufferedReader myInput = null;
		String thisLine;
		try {
			
			myInput = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(input))));
					
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
			
			myInput = new BufferedReader(new InputStreamReader(
					new FileInputStream(new File(input))));
					
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
		
		for(int i=0;i<pop_size;++i){
			pBest.add(new Particle(twuPattern.size()));
		}
		
		gBest = new Particle(twuPattern.size());
		
		checkMemory();
		
		if (twuPattern.size() > 0) {
			
			generatePop(minUtility);

			for (int i = 0; i < iterations; i++) {
				update(minUtility);
			}
		}

		writeOut();
		checkMemory();
		writer.close();
		endTimestamp = System.currentTimeMillis();
	}


	private void generatePop(int minUtility)//
	{
		int i, j, k, temp;
		percentage = roulettePercent();

		for (i = 0; i < pop_size; i++) {
			
			Particle tempParticle = new Particle(twuPattern.size());
			j = 0;

			k = (int) (Math.random() * twuPattern.size());

			while (j < k) {
				temp = rouletteSelect(percentage);
				if (tempParticle.X.get(temp) == 0) {
					j++;
					tempParticle.X.set(temp, 1);
				}

			}
			
			tempParticle.fitness = fitCalculate(tempParticle.X, k);
			population.add(i, tempParticle);
			pBest.get(i).copyParticle(tempParticle);
			
			if (population.get(i).fitness >= minUtility) {
				insert(population.get(i));
			}
			
			if (i == 0) {
				gBest.copyParticle(pBest.get(i));
			} else {
				if (pBest.get(i).fitness > gBest.fitness) {
					gBest.copyParticle(pBest.get(i));
				}
			}
			
			List<Double> tempV = new ArrayList<Double>();
			for (j = 0; j < twuPattern.size(); j++) {
				tempV.add(j, Math.random());
			}
			V.add(i, tempV);
		}
	}

	
	private void update(int minUtility) {
		int i, j, k;
		double r1, r2, temp1, temp2;

		for (i = 0; i < pop_size; i++) {
			k = 0;
			r1 = Math.random();
			r2 = Math.random();
			
			for (j = 0; j < twuPattern.size(); j++) {
				double temp =V.get(i).get(j) + r1 * (pBest.get(i).X.get(j) - population.get(i).X.get(j)) + r2 * (gBest.X.get(j) - population.get(i).X.get(j));
				V.get(i).set(j, temp);
				if (V.get(i).get(j) < -2.0)
					V.get(i).set(j, -2.0);
				else if (V.get(i).get(j) > 2.0)
					V.get(i).set(j, 2.0);
			}
			
			for (j = 0; j < twuPattern.size(); j++) {
				temp1 = Math.random();
				temp2 = 1 / (1.0 + Math.exp(-V.get(i).get(j)));
				if (temp1 < temp2) {
					population.get(i).X.set(j, 1);
					k++;
				} else {
					population.get(i).X.set(j, 0);
				}
			}
			
			population.get(i).fitness = fitCalculate(population.get(i).X, k);

			if (population.get(i).fitness > pBest.get(i).fitness) {
				pBest.get(i).copyParticle(population.get(i));
				if (pBest.get(i).fitness > gBest.fitness) {
					gBest.copyParticle(pBest.get(i));
				}
			}
			
			if (population.get(i).fitness >= minUtility) {
				insert(population.get(i));
			}
		}
	}


	private void insert(Particle tempParticle) {
		int i;
		StringBuilder temp = new StringBuilder();
		for (i = 0; i < twuPattern.size(); i++) {
			if (tempParticle.X.get(i) == 1) {
				temp.append(twuPattern.get(i));
				temp.append(' ');
			}
		}
		
		if (huiSets.size() == 0) {
			huiSets.add(new HUI(temp.toString(), tempParticle.fitness));
		} else {
			
			for (i = 0; i < huiSets.size(); i++) {
				if (temp.toString().equals(huiSets.get(i).itemset)) {
					break;
				}
			}
			
			if (i == huiSets.size())
				huiSets.add(new HUI(temp.toString(), tempParticle.fitness));
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


	private int rouletteSelect(List<Double> percentage) {
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


	private int fitCalculate(List<Integer> tempParticle, int k) {
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
			
			while (j < k && q < database.get(p).size()
					&& i < tempParticle.size()) {
				if (tempParticle.get(i) == 1) {
					if (database.get(p).get(q).item < twuPattern.get(i))
						q++;
					else if (database.get(p).get(q).item == twuPattern.get(i)) {
						sum = sum + database.get(p).get(q).utility;
						j++; q++; temp++; i++;
					} else if (database.get(p).get(q).item > twuPattern.get(i)) {
						j++; i++;
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
		System.out.println("============= HUIM-BPSO ALGORITHM STATS =============");
		System.out.println(" Total time: " + (endTimestamp - startTimestamp) + " ms");
		System.out.println(" Memory: " + maxMemory + " MB");
		System.out.println(" High-utility itemsets count: " + huiSets.size());
		System.out.println("===================================================");
	}
}
