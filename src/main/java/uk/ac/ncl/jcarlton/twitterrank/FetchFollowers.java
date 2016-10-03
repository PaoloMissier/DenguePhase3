package uk.ac.ncl.jcarlton.twitterrank;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import twitter4j.IDs;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import uk.ac.ncl.jcarlton.util.Utility;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Follower processing.
 *
 * @author Jonathan Carlton
 */
public class FetchFollowers {

    private static final String DB_NAME = "twitter-rank-pipeline";
    private static final String COLLECTION_FULL = "socialgraph_full";
    private static final String COLLECTION_TOPIC = "socialgraph_topic_";

    public static void fullSocialGraph() {
        Map<String, Integer> map = Utility.fetchAllUsers();
        crawlForFollowers(map, DB_NAME, COLLECTION_FULL, 0);
    }


    public static void topicalSocialGraph(int topic) {
        Map<String, Integer> map = Utility.fetchAllUsers(topic);
        crawlForFollowers(map, DB_NAME, COLLECTION_TOPIC + topic, topic);
    }

    // TODO think about this...
    public static void individualUser(String userId) {}

    /**
     * Query the Twitter API for the followers of
     * each of the users within the data set that
     * is stored on the Mongo instance.
     * <p>
     * Warning: Can take a long time due to the
     * Twitter API rate limitations. Especially
     * when collecting a large amount.
     *
     * @param map            all users within the data set,
     *                       their ID mapped to the number
     *                       of occurrences.
     * @param dbName         name of the database
     * @param collectionName name of the collection
     * @param classifierId   the classification id (0 if all)
     * @param limit          optional limit to impose.
     */
    private static void crawlForFollowers(Map<String, Integer> map, String dbName,
                                          String collectionName, int classifierId, int... limit) {
        MongoClient client = new MongoClient();
        MongoDatabase db = client.getDatabase(dbName);
        MongoCollection<BasicDBObject> collection = db.getCollection(collectionName, BasicDBObject.class);

        TwitterSetup setup = new TwitterSetup();
        Twitter twitter = setup.getInstance();

        int counter = 0;
        for (Map.Entry<String, Integer> m : map.entrySet()) {
            if (counter == limit[0]) break;
            else {
                IDs ids;
                long cursor = -1;
                long currentId = Long.parseLong(m.getKey());
                BasicDBList followers = new BasicDBList();

                try {
                    do {
                        ids = twitter.getFollowersIDs(currentId, cursor);
                        for (Object f_id : ids.getIDs())
                            followers.add(f_id);
                    } while ((cursor = ids.getNextCursor()) != 0);

                    BasicDBObject dbObject = new BasicDBObject();
                    dbObject.put("user_id", currentId);
                    dbObject.put("occurrences_in_dataset", m.getValue());

                    if (classifierId == 0)
                        dbObject.put("classifier_id", Utility.fetchClassification(currentId, client));
                    else dbObject.put("classifier_id", classifierId);

                    dbObject.put("followers", followers);
                    collection.insertOne(dbObject);
                    counter++;
                } catch (TwitterException e) {
                    if (e.getErrorCode() == 88) {
                        System.out.println("----- Thread Sleep -----");
                        try {
                            TimeUnit.MINUTES.sleep(15);
                        } catch (InterruptedException ie) {
                            System.out.println("----- Interrupted Sleep -----");
                            ie.printStackTrace();
                        }
                        continue;
                    } else if (e.getErrorCode() == 34) {
                        System.out.println("------ User doesn't exist / Private account ------");
                        continue;
                    } else {
                        e.printStackTrace();
                    }
                }
            }
        }
        followEachOther(db, collectionName, String.valueOf(limit));
    }

    /**
     * Once the followers for the users within the
     * data-set has been collected, now process if any
     * of them follow each other to start building
     * the social graph.
     *
     * @param database       pre-initialised MongoDatabase object, to
     *                       save reopening another connection.
     * @param collectionName the collection in which to query.
     * @param count          the number to look at, would
     *                       be the limit passed from the
     *                       crawl. Optional
     */
    private static void followEachOther(MongoDatabase database, String collectionName, String... count) {
        // user id mapped to a list of those users who follow (within the data-set)
        Map<Long, List<Long>> map = new HashMap<>();

        FindIterable<Document> iterable = database.getCollection(collectionName).find();
        iterable.forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                List followerList = (ArrayList) document.get("followers");
                long currentId = (long) document.get("user_id");

                FindIterable<Document> allUsers = database.getCollection(collectionName).find();
                allUsers.forEach(new Block<Document>() {
                    @Override
                    public void apply(Document document1) {
                        long uID = (long) document1.get("user_id");
                        for (int i = 0; i < followerList.size(); i++) {
                            // if the current ID is in the list of followers for 'current'
                            if (followerList.get(i).equals(uID)) {
                                if (map.containsKey(currentId))
                                    map.get(currentId).add(uID);
                                else {
                                    map.put(currentId, new LinkedList<>());
                                    map.get(currentId).add(uID);
                                }
                            }
                        }
                    }
                });
            }
        });

        try {
            Utility utility = new Utility();
            String resourcePath = utility.getResourcePath();
            if (resourcePath != null) {
                File file;
                if (count[0].isEmpty())
                    file = new File(resourcePath + "/follow-each-other/" + "follow-each-other-" + collectionName + ".txt");
                else
                    file = new File(resourcePath + "/follow-each-other/" + count[0] + "-follow-each-other-" + collectionName + ".txt");

                FileWriter fileWriter = new FileWriter(file);
                for (Map.Entry<Long, List<Long>> m : map.entrySet()) {
                    fileWriter.write(m.getKey() + ":");
                    for (int i = 0; i < m.getValue().size(); i++)
                        fileWriter.write(m.getValue().get(i) + " ");
                    fileWriter.write("\n");
                    fileWriter.flush();
                }
                fileWriter.close();
            } else {
                throw new IOException("Error in fetching resource path");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
