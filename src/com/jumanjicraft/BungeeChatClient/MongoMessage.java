package com.jumanjicraft.BungeeChatClient;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MongoMessage {
    private DBCollection collection;
    private int serverID;
    private final DBObject searchQuery;
    public MongoMessage(DBCollection collection, int serverID) {
        this.collection = collection;
        this.serverID = serverID;
        BasicDBObject serverList = (BasicDBObject) collection.findOne(new BasicDBObject("serverlist", true));
        if (serverList == null) {
            List<Integer> servers = new ArrayList<Integer>();
            servers.add(serverID);
            serverList = new BasicDBObject("serverlist", true)
                    .append("servers", servers);

        } else {
            List<Integer> servers = (List<Integer>) serverList.get("servers");
            if (!servers.contains(serverID)) {
                servers.add(serverID);
            }
            serverList.put("servers", servers);
        }

        collection.save(serverList);

        searchQuery = new BasicDBObject("payload", true)
                .append("servers", serverID);

        final Calendar calendar = Calendar.getInstance();

        calendar.add(Calendar.SECOND, 60);
    }

    public void send(String key, String message) {
        BasicDBObject servers = (BasicDBObject) collection.findOne(new BasicDBObject("serverlist", true));
        for (Integer server : (List<Integer>) servers.get("servers")) {
            collection.save(new BasicDBObject("payload", true)
                    .append("created", new Date())
                    .append("servers", server)
                    .append(key, message));
        }
    }

    public BasicDBObject get() {
        try {
            Thread.sleep(200);
            return (BasicDBObject) collection.findOne(searchQuery);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void ack(DBObject object) {
        collection.remove(object);
    }

    private DBObject getServers() {
        return collection.findOne(new BasicDBObject("serverlist", true));
    }

    public void stop() {
        BasicDBObject serverList = (BasicDBObject) collection.findOne(new BasicDBObject("serverlist", true));
        List<Integer> servers = (List<Integer>) serverList.get("servers");
        servers.remove(serverID);
        collection.remove(serverList);
    }
}
