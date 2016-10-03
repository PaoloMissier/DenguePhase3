package uk.ac.ncl.pipeline;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import uk.ac.ncl.cc.classifier.Category;
import uk.ac.ncl.cc.classifier.Classifier;
import uk.ac.ncl.jcarlton.twitterrank.FetchFollowers;
import uk.ac.ncl.jcarlton.util.MongoLoad;

import java.io.FileReader;
import java.io.IOException;

/**
 *
 * @author Jonathan Carlton
 */
public class TwitterRankPipeline {

    boolean started;
    String collectionName;
    int topic;

    public TwitterRankPipeline() {
        this.started = false;
        this.topic = 0;
    }

    public void start(String jsonFileName) {
        JSONArray jsonFile = readInJson(jsonFileName);

        // load into mongo
        MongoLoad ml = new MongoLoad();
        for (Object o : jsonFile) {
            JSONObject tweet = (JSONObject) o;
            JSONObject cTweet = classifyTweet(tweet);
            ml.importJson(cTweet);
        }

        started = true;
    }

    /**
     *
     * Note: This can take a significant amount of time
     * due to the limitations of the Twitter API (rate limits).
     */
    public void beginFollowerHarvesting() {
        if (started) {
            FetchFollowers.fullSocialGraph();
            collectionName = "socialgraph_full";
        }
    }

    /**
     * Note: This can take a significant amount of time
     * due to the limitations of the Twitter API (rate limits).
     * @param topic
     */
    public void beginFollowerHarvesting(int topic) {
        if (started) {
            FetchFollowers.topicalSocialGraph(topic);
            collectionName = "socialgraph_topic_" + String.valueOf(topic);
            this.topic = topic;
        }
    }

    /**
     *
     */
    public void startTwitterRank() {

    }

    private JSONArray readInJson(String jsonFileName) {
        JSONParser parser = new JSONParser();
        JSONArray arr = null;
        try {
            arr = (JSONArray) parser.parse(new FileReader(jsonFileName));
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return arr;
    }

    private JSONObject classifyTweet(JSONObject tweet) {
        try {
            Classifier classifier = Classifier.getInstance();
            String tweetText = (String) tweet.get("text");
            Category category = classifier.classify(tweetText);

            JSONObject classification = new JSONObject();
            classification.put("id", category.getPrediction());
            classification.put("key", category.toString());
            classification.put("label", category.getKey());
            tweet.put("classification", classification);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tweet;
    }

    private void produceRankedList() {

    }
}
