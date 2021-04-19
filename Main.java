import java.util.Scanner;
import HUIM_GA.HUIM_GA;
import HUIM_BPSO.HUIM_BPSO;
import HUIM_ABC.HUIM_ABC;


public class Main {

    public static void main (String[] args) throws Exception {
        // System.out.println(args[0]);
        if (args.length != 4){
            System.out.println("Usage: java Main <algo_name> <input_file> <output_file> <min_utility>");
        } else {
            processCommandLineArguments(args); 
        }
    }

    public static void processCommandLineArguments (String[] args) {
        try {
            if ("ga".equals(args[0])){
                HUIM_GA algo = new HUIM_GA();
                algo.runAlgorithm(args[1], args[2], Integer.parseInt(args[3]));
                algo.printStats();
            }
            
            else if ("pso".equals(args[0])){
                HUIM_BPSO algo = new HUIM_BPSO();
                algo.runAlgorithm(args[1], args[2], Integer.parseInt(args[3]));
                algo.printStats();
            }
            
            else if ("abc".equals(args[0])){
                HUIM_ABC algo = new HUIM_ABC();
                Scanner in = new Scanner(System.in);
                System.out.println("BucketNum (press 0 to escape): ");
                int bucketNum = in.nextInt();
                if (bucketNum != 0){
                    algo.setBucketNum(bucketNum);
                }
                algo.runAlgorithm(args[1], args[2], Integer.parseInt(args[3]));
                algo.printStats();
                in.close();
            }
        } catch (Exception e) {
            System.out.println("An error while trying to run the algorithm. \n ERROR MESSAGE = " + e.toString());
            e.printStackTrace();
        }
    }
}