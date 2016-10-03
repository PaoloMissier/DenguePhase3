package uk.ac.ncl.jcarlton.util;

import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Utility class for misc methods to perform certain actions
 *
 *
 * @author Jonathan Carlton
 */
public class Utility {

    public Utility() {
    }

    /**
     * Fetch the access codes for the various API's that
     * are being used throughout the project.
     *
     * @param filename name of the API
     * @param arrSize  number of expected tokens
     * @return an array consisting of the
     * requested API tokens.
     */
    public String[] getTokens(String filename, int arrSize) {
        String[] result = new String[arrSize];

        File file = new File(getClass().getResource("/access-codes/" + filename).getFile());

        int i = 0;
        try (Scanner scanner = new Scanner(file)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                result[i] = line;
                i++;
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return result;
    }

    public String getResourcePath() {
        try {
            URI pathFile = System.class.getResource("/RESOURCE_PATH").toURI();
            String resourcePath = Files.readAllLines(Paths.get(pathFile)).get(0);
            URI rootURI = new File("").toURI();
            URI resourceURI = new File(resourcePath).toURI();
            URI relativeResourceURI = rootURI.relativize(resourceURI);
            return relativeResourceURI.getPath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     *
     * ==========================================================
     * MongoDB specific utility methods
     * ==========================================================
     */

    /**
     * Fetches all of the users (their ids) in the tweet data set
     * stored on the local mongo client. It also computes the
     * number of occurrences that a user has within the data set.
     *
     * @return  map of user ids -> number of occurrences
     */
    public static Map<String, Integer> fetchAllUsers() {
        MongoClient client = new MongoClient();
        MongoDatabase db = client.getDatabase("twitter-rank-pipeline");

        // tweets-process is the collection that has the classified tweets in
        MongoCollection collection = db.getCollection("tweets-processed");

        Map<String, Integer> map = new HashMap<String, Integer>();
        FindIterable<Document> iterable = collection.find();
        iterable.forEach(new Block<Document>() {
            public void apply(Document document) {
                Document user = (Document) document.get("user");
                String id = user.get("id").toString();
                if (map.containsKey(id)) map.computeIfPresent(id, (k, v) -> v + 1);
                else map.put(id, 1);
            }
        });

        return map;
    }

    public static Map<String, Integer> fetchAllUsers(int topic) {
        MongoClient client = new MongoClient();
        MongoDatabase db = client.getDatabase("twitter-rank-pipeline");

        // tweets-process is the collection that has the classified tweets in
        MongoCollection collection = db.getCollection("tweets-processed");

        Map<String, Integer> map = new HashMap<>();
        FindIterable<Document> iterable = collection.find(Filters.eq("classification.id", topic));
        iterable.forEach(new Block<Document>() {
            public void apply(Document document) {
                Document user = (Document) document.get("user");
                String id = user.get("id").toString();
                if (map.containsKey(id)) map.computeIfPresent(id, (k, v) -> v + 1);
                else map.put(id, 1);
            }
        });

        return map;
    }


    public static int fetchClassification(long currentId, MongoClient client) {
        final String[] val = new String[1];
        try {
            MongoDatabase db = client.getDatabase("twitter-rank-pipeline");
            FindIterable<Document> iterable = db.getCollection("tweets-processed").find(Filters.eq("user.id", currentId));
            iterable.forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {
                    Document classification = (Document) document.get("classification");
                    val[0] = classification.get("id").toString();
                }
            });
        } catch (NullPointerException e) {
            System.out.println("Null pointer caught for id: " + currentId);
            val[0] = "0";
        }
        return Integer.parseInt(val[0]);
    }

}
