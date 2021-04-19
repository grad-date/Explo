package HUIM_ABC;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class HUIM_ABC {
	
	double maxMemory = 0; 
	long startTimestamp = 0;
	long endTimestamp = 0;
	int transactionCount = 0;
	
	final int pop_size =10;
	final int limit = 5;
	final int iterations = 2000;
	int changeBitNO = 2;
	int times = 5;
	final int prunetimes = 50;
	final int estiTransCount = 10000;
	int m = 0;
	int bucketNum = 120;

	int[] ScoutBeesBucket=new int[bucketNum];
	double[] RScoutBeesiniBit=new double[bucketNum];
	
	int iniBitNO=0;
	
	Map<Integer, Integer> mapItemToTWU;
	Map<Integer, Integer> mapItemToTWU0;
	
	List<Integer> twuPattern;
	BufferedWriter writer = null;
	

	class Pair {
		int item = 0;
		int utility = 0;
		int rutil = 0;
	}
	
	
	class BeeGroup {
		List<Integer> X;
		int fitness;
		int rutil;
		int trail;
		double rfitness;

		public void addtrail(int k){
			trail = trail + k;
		}
		
		public BeeGroup() {
			X = new ArrayList<Integer>();
		}
		
		public BeeGroup(int length) {
			X = new ArrayList<Integer>();
			for (int i = 0; i < length; i++) {
				X.add(0);			
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
	
	
	class Item{
		int item;
		BitSet TIDS;
		public Item(){
			TIDS=new BitSet(estiTransCount);
		}
		public Item(int item){
			TIDS=new BitSet(estiTransCount);
			this.item=item;
		}
	}
	
	List<Item> Items;
	List<BeeGroup> NectarSource = new ArrayList<BeeGroup>();
	List<BeeGroup> EmployedBee = new ArrayList<BeeGroup>();
	List<BeeGroup> OnLooker = new ArrayList<BeeGroup>();
	
	long sumTwu=0;
	List<HUI> huiSets = new ArrayList<HUI>();
	Set<List<Integer>>  huiBeeGroup = new HashSet<List<Integer>>();
	List<List<Pair>> database = new ArrayList<List<Pair>>();
	List<List<Integer>> databaseTran = new ArrayList<List<Integer>>();
	List<Double> percentage = new ArrayList<Double>();
	

	public HUIM_ABC() {
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
				sumTwu =sumTwu+transactionUtility;
				
				for (int i = 0; i < items.length; i++) {
					
					Integer item = Integer.parseInt(items[i]);
					Integer twu = mapItemToTWU.get(item);
					Integer twu0 = mapItemToTWU0.get(item);
					
					twu = (twu == null) ? transactionUtility : twu
							+ transactionUtility;
					twu0 = (twu0 == null) ? transactionUtility : twu0
							+ transactionUtility;
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
				List<Integer> pattern=new ArrayList<Integer>();
				
				int remainingUtility = 0;

				for (int i = 0; i < items.length; i++) {
					Pair pair = new Pair();
					pair.item = Integer.parseInt(items[i]);
					pair.utility = Integer.parseInt(utilityValues[i]);
					if (mapItemToTWU.get(pair.item) >= minUtility) {
						revisedTransaction.add(pair);
						pattern.add(pair.item);						
						remainingUtility +=pair.utility;	
					}else{
						mapItemToTWU0.remove(pair.item);
					}
				}
				
				for(Pair pair : revisedTransaction){
					remainingUtility = remainingUtility - pair.utility;
					pair.rutil = remainingUtility;
				}
				
				database.add(revisedTransaction);
				databaseTran.add(pattern);

				++transactionCount;
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
		
		m=(int)(twuPattern.size()/bucketNum);

		Items=new ArrayList<Item>();
		
		for(Integer tempitem:twuPattern){
			Items.add(new Item(tempitem.intValue()));
		}

		for(int i=0;i<database.size();++i){
			for(int j=0;j<Items.size();++j){
				for(int k=0;k<database.get(i).size();++k){
					if(Items.get(j).item==database.get(i).get(k).item){
						Items.get(j).TIDS.set(i);
					}
				}
			}
		}
		
		for(int i=0;i<ScoutBeesBucket.length;++i){
			ScoutBeesBucket[i]=1;
		}
			
		checkMemory();
		
		if(twuPattern.size()>0){
			Initialization(minUtility);
			for(int gen =0; gen < iterations; ++gen){
				iniBitNO=32+1;
				
				Employed_bees(minUtility);
				
				calculateRfitness();
				
				OnLooker_bees(minUtility);

				calScoutBees();
				
				Scout_bees(iniBitNO,minUtility);
			}
		}
			
		writeOut();
		checkMemory();
		writer.close();
		endTimestamp = System.currentTimeMillis();

	}


	public boolean isRBeeGroup(BeeGroup tempBeeGroup,List<Integer> list){
		List<Integer> templist=new ArrayList<Integer>();

		for(int i=0;i<tempBeeGroup.X.size();++i){
			if(tempBeeGroup.X.get(i)!=0){
				templist.add(i);
			}	
		}
		if(templist.size()==0){
			return false;
		}
		BitSet tempBitSet=new BitSet(estiTransCount);
		tempBitSet=(BitSet)Items.get(templist.get(0).intValue()).TIDS.clone();
		
		for(int i=1;i<templist.size();++i){
			if(tempBitSet.cardinality()==0){
				break;
			}
			tempBitSet.and(Items.get(templist.get(i).intValue()).TIDS);
		}
		
		if(tempBitSet.cardinality()==0){
			return false;
		}else{
			for(int m=0;m<tempBitSet.length();++m){
				if(tempBitSet.get(m)){
					list.add(m);
				}	
			}
			return true;	
		}
	}
	
	
	private void Initialization(int minUtility){
		int i = 0,k=0;
		percentage = roulettePercent();

		while (i < pop_size) {
			//Initialization nectar source
			BeeGroup tempNode;
			BeeGroup besttempNode=new BeeGroup(twuPattern.size());
			List<Integer> templist;

			int j=0;
			do{
				do{
					templist=new ArrayList<Integer>();

					do{
						k = (int) (Math.random() * twuPattern.size());
					}while(k==0);
					
					tempNode = new BeeGroup(twuPattern.size());
					
					iniBeeGroup(tempNode,k);
				}while(!isRBeeGroup(tempNode,templist)||huiBeeGroup.contains(tempNode.X));
				
				fitCalculate(tempNode, k,templist);
								
				if(tempNode.fitness>=besttempNode.fitness){
					copyBeeGroup(besttempNode,tempNode);
				}
				++j;
			}while(besttempNode.fitness<minUtility&&j<times);

			besttempNode.trail = 0;
			
			OnLooker.add(new BeeGroup(twuPattern.size()));
			EmployedBee.add(new BeeGroup(twuPattern.size()));
			
			NectarSource.add(besttempNode);
			
			if (besttempNode.fitness >= minUtility) {
				if(!huiBeeGroup.contains(besttempNode.X)){
					updateScoutBeesBucket(Collections.frequency(besttempNode.X, 1));
				}
				addlist(huiBeeGroup,besttempNode.X);
				insert(besttempNode);
			}
			i++;
		}
		
		copylistBeeGroup(EmployedBee, NectarSource);
	}
	
	
	public void copylistBeeGroup(List<BeeGroup> list1BeeGroup,List<BeeGroup> list2BeeGroup){
		for(int i=0;i<list1BeeGroup.size();++i){
			copyBeeGroup(list1BeeGroup.get(i),list2BeeGroup.get(i));
		}
	}

	public void copyBeeGroup(BeeGroup beeG1,BeeGroup beeG2){
		copyList(beeG1.X,beeG2.X);
		beeG1.fitness =beeG2.fitness;
		beeG1.rfitness = beeG2.rfitness;
		beeG1.rutil =beeG2.rutil;
		beeG1.trail =beeG2.trail;
	}
	
	public void  copyList(List<Integer> list1,List<Integer> list2){
		for(int i=1;i<list1.size();++i){
			list1.set(i, list2.get(i).intValue());
		}
	}
	
	
	public void addlist(Set<List<Integer>> huiBeeGroup,List<Integer> list){
		List<Integer> templist= new ArrayList<Integer>();
		for(int i=0;i<list.size();++i){
			templist.add(list.get(i).intValue());
		}
		huiBeeGroup.add(templist);
	}
	

	public void Employed_bees(int minUtility){
		int i = 0;
		copylistBeeGroup(EmployedBee, NectarSource);
		BeeGroup temp;
		
		for(i = 0; i < pop_size; ++i){
			
			temp=meetReqBeeGroup(EmployedBee.get(i),minUtility,"sendEmployedBees");
			EmployedBee.set(i, temp);
			
			if(EmployedBee.get(i).fitness > NectarSource.get(i).fitness){
				copyBeeGroup(NectarSource.get(i),EmployedBee.get(i));
			}else{
				NectarSource.get(i).addtrail(1);
			}
		}
	}
	
	
	public void OnLooker_bees(int minUtility){
		for(int i=0;i<pop_size;++i){
			BeeGroup tempBeeGroup;
			int temp=selectNectarSource();
			
			copyBeeGroup(OnLooker.get(i),NectarSource.get(temp));

			tempBeeGroup=meetReqBeeGroup(OnLooker.get(i),minUtility,"sendOnLookerBees");
			
			OnLooker.set(i, tempBeeGroup);
			
			if(OnLooker.get(i).fitness > NectarSource.get(temp).fitness){
				
				copyBeeGroup(NectarSource.get(temp),OnLooker.get(i));
				
			}else{
				NectarSource.get(temp).addtrail(1);
			}
		}
	}
	
	
	public void Scout_bees(int iniBitNO,int minUtility){
		
		for(int i=0;i<pop_size;++i){
			if(NectarSource.get(i).trail>limit){
				
				BeeGroup tempNode;
				BeeGroup besttempNode=new BeeGroup(twuPattern.size());
		
				int k =0;
				
				List<Integer> templist;
				int j=0;
				int times=5;
				do{
					do{
						templist=new ArrayList<Integer>();
						do{
							k=selectScoutIniBit()*m+(int)(Math.random()*m);

						}while(k==0);
						
						tempNode = new BeeGroup(twuPattern.size());
						iniBeeGroup(tempNode,k);
					}while(!isRBeeGroup(tempNode,templist)||huiBeeGroup.contains(tempNode.X));
					
					fitCalculate(tempNode, k,templist);
					
					if(tempNode.fitness>=besttempNode.fitness){
						copyBeeGroup(besttempNode,tempNode);
					}
					++j;
					
				}while(besttempNode.fitness<minUtility&&j<times);
				
				
				besttempNode.trail = 0;
				NectarSource.set(i,besttempNode);
							
				if (besttempNode.fitness >= minUtility) {
					if(!huiBeeGroup.contains(besttempNode.X)){
						updateScoutBeesBucket(Collections.frequency(besttempNode.X, 1));
					}
					addlist(huiBeeGroup,besttempNode.X);
					insert(besttempNode);
				}
			}
		}
	}


	private int selectScoutIniBit(){
		int i, temp = 0;
		double randNum;
		randNum = Math.random();
		for (i = 0; i < RScoutBeesiniBit.length; i++) {
			if (i == 0) {
				if ((randNum >= 0) && (randNum <= RScoutBeesiniBit[0])) {
					temp = 0;
					break;
				}
			} else if ((randNum > RScoutBeesiniBit[i-1])
					&& (randNum <= RScoutBeesiniBit[i])) {
				temp = i;
				break;
			}
		}
		return temp;
	}
	

	public void updateScoutBeesBucket(int k){
		int temp=k/bucketNum;
		if(k>=50){
			ScoutBeesBucket[bucketNum-1]+=1;
			return;
		}
		ScoutBeesBucket[temp]+=1;
	}
	

	public void calScoutBees(){
		int sum=0;
		int tempSum=0;
		
		for(int i=0; i< ScoutBeesBucket.length;++i){
			sum =sum+ScoutBeesBucket[i];
		}
		
		for(int i=0; i< ScoutBeesBucket.length;++i){
			tempSum =tempSum+ScoutBeesBucket[i];
			RScoutBeesiniBit[i]= tempSum/(sum+0.0);
		}
	}
	
	
	private int selectNectarSource() {
		int i, temp = 0;
		double randNum;
		randNum = Math.random();
		for (i = 0; i < NectarSource.size(); i++) {
			if (i == 0) {
				if ((randNum >= 0) && (randNum <= NectarSource.get(0).rfitness)) {
					temp = 0;
					break;
				}
			} else if ((randNum > NectarSource.get(i - 1).rfitness)
					&& (randNum <= NectarSource.get(i).rfitness)) {
				temp = i;
				break;
			}
		}
		return temp;
	}
	

	public void calculateRfitness(){
		int sum=0;
		int temp=0;
		
		for(int i=0; i< NectarSource.size();++i){
			sum =sum+NectarSource.get(i).fitness;
		}
		
		for(int i=0; i< NectarSource.size();++i){
			temp =temp+NectarSource.get(i).fitness;
			NectarSource.get(i).rfitness= temp/(sum+0.0);
		}
	}
	

	public void changeKBit(BeeGroup tempGroup){
		List<Integer> templist=new ArrayList<Integer>();
		
		for(int i = 0; i <changeBitNO;++i){
			int k=0;
			do{
				k = (int) (Math.random() * twuPattern.size());
				
			}while(templist.contains(k));
			
			templist.add(k);
			
			if(tempGroup.X.get(k)==1){
				tempGroup.X.set(k,0);
			}else{
				tempGroup.X.set(k,1);
			}
		}
	}
	

	public BeeGroup meetReqBeeGroup(BeeGroup tempGroup,int minUtility,String flag){
		int j=0;
		int k=0;
		changeBitNO=1;
		times=5;
		
		List<Integer> templist;
		
		BeeGroup besttempNode=new BeeGroup(twuPattern.size());
		
		copyBeeGroup(besttempNode,tempGroup);
		do{
			
			do{
				templist=new ArrayList<Integer>();
				changeKBit(tempGroup);
				
			}while(!isRBeeGroup(tempGroup,templist)||huiBeeGroup.contains(tempGroup.X));

			k=Collections.frequency(tempGroup.X, 1);
			
			fitCalculate(tempGroup,k ,templist);
			
			if(tempGroup.fitness>besttempNode.fitness){
				copyBeeGroup(besttempNode,tempGroup);
			}else{
				copyBeeGroup(tempGroup,besttempNode);
			}
			++j;
			
		}while(besttempNode.fitness<minUtility && j<times);
		
		if (besttempNode.fitness >= minUtility) {
			if(!huiBeeGroup.contains(besttempNode.X)){
				updateScoutBeesBucket(Collections.frequency(besttempNode.X, 1));
			}
			addlist(huiBeeGroup,besttempNode.X);
			insert(besttempNode);
		}
		
		return besttempNode;	
	}
	
	
	public List<Integer> delete0(List<Integer> list){
		int i=0;
		int temp=0;
		if(list.size()>0&&Collections.frequency(list, 1)>0){
			i=list.size()-1;
	
			while(i>=0&&list.get(i)==0){
				--i;
			}
		
			List<Integer> templist= new ArrayList<Integer>();
			int j=0;

			while(j<=i){
				temp=list.get(j).intValue();
				templist.add(temp);
				++j;
			}
			return templist;
		}else{
			return null;
		}
	}
	
	
	public void iniBeeGroup(BeeGroup tempNode,int k){
		int j = 0;
		int temp;

		while (j < k) {
			temp = select(percentage);
			if (tempNode.X.get(temp) == 0){
				++j;
				tempNode.X.set(temp, 1);
			}	
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


	private void fitCalculate(BeeGroup tempGroup, int k,List<Integer> templist) {

		if (k == 0)
			return;

		int i, j, p, q, temp,m;
		int sum, fitness = 0;
		int rutil = 0;

		for (m = 0; m < templist.size(); m++) {
			p=templist.get(m).intValue();									
			i = 0;
			j = 0;
			q = 0;
			temp = 0;
			sum = 0;
			
			while (j < k && q < database.get(p).size()
					&& i < tempGroup.X.size()) {
				if (tempGroup.X.get(i) == 1) {
					if (database.get(p).get(q).item < twuPattern.get(i))
						q++;
					else if (database.get(p).get(q).item == twuPattern.get(i)) {
						sum = sum + database.get(p).get(q).utility;
						j++;
						q++;
						temp++;
						i++;
					} else if (database.get(p).get(q).item > twuPattern.get(i)) {
						break;
					}
				} else{
					i++;
				}
			}

			if (temp == k) {
				rutil = rutil + database.get(p).get(q-1).rutil;
				fitness = fitness + sum;
			}
		}

		tempGroup.rutil = rutil + fitness;
		tempGroup.fitness = fitness;
	}
	
	
	private void insert(BeeGroup tempBeeGroup) {
		int i;
		StringBuilder temp = new StringBuilder();
		for (i = 0; i < twuPattern.size(); i++) {
			if (tempBeeGroup.X.get(i) == 1) {
				temp.append(twuPattern.get(i));
				temp.append(' ');
			}
		}
		
		if (huiSets.size() == 0) {
			huiSets.add(new HUI(temp.toString(), tempBeeGroup.fitness));
		} else {

			for (i = 0; i < huiSets.size(); i++) {
				if (temp.toString().equals(huiSets.get(i).itemset)) {
					break;
				}
			}
			
			if (i == huiSets.size())
				huiSets.add(new HUI(temp.toString(), tempBeeGroup.fitness));
		}
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
		
		double currentMemory = (Runtime.getRuntime().totalMemory() - Runtime
				.getRuntime().freeMemory()) / 1024d / 1024d;
				
		if (currentMemory > maxMemory) {
			maxMemory = currentMemory;
		}
	}
	

	public int getBucketNum() {
		return bucketNum;
	}
	
	
	public void setBucketNum(int bucketNum) {
		this.bucketNum = bucketNum;
		ScoutBeesBucket = new int[bucketNum];
		RScoutBeesiniBit = new double[bucketNum];
	}
	
	
	public void printStats() {
		System.out.println("=============  HUIM-ABC ALGORITHM STATS =============");
		System.out.println(" Total time: " + (endTimestamp - startTimestamp) + " ms");
		System.out.println(" Memory: " + maxMemory + " MB");
		System.out.println(" High-utility itemsets count: " + huiSets.size());
		System.out.println("===================================================");
	}
}
