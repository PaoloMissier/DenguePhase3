package uk.ac.ncl.jcarlton;

import uk.ac.ncl.cc.classifier.Category;
import uk.ac.ncl.cc.classifier.Classifier;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created by Jonathan on 08-Jul-16.
 */
public class TestRun {
    public static void main(String[] args) {
        try {
            Classifier classifier = Classifier.getInstance();
//            Category cat = classifier.classify("#Dengue matou 229 brasileiros desde o in¡cio do ano - http://t.co/jXkN5IWJnz http://t.co/sl5F3sdqes");
//            System.out.println(cat);

            BufferedReader br = new BufferedReader(new FileReader("9k-filtered-txt-proper.txt"));
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();
            int i = 1;
            int t = 0;
            int f = 0;
            while (line != null) {
                String[] split = line.split(" ");

                Category cat = classifier.classify(split[1]);
                if (cat.toString().toLowerCase().equals(split[0])) {
                    System.out.println(i + ", true");
                    i++;
                    t++;
                }
                else {
                    System.out.println(i +", false");
                    System.out.println("\tActual: " + split[0] + ", Classified: " + cat);
                    System.out.println("key : " +cat.getKey());
                    System.out.println("name : " + cat.getName());
                    System.out.println("to string: " + cat.toString());
                    System.out.println("predication: " + cat.getPrediction());
                    i++;
                    f++;
                }

                line = br.readLine();
            }
            System.out.println("True Matches: " + t);
            System.out.println("False Matches: " + f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
