package com.jumanjicraft.BungeeChatClient;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.Herochat;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.UnknownHostException;

public class BungeeChatClient extends JavaPlugin {

    private final String CHANNEL_NAME_SEND = "BungeeChatSend", CHANNEL_NAME_RECEIVE = "BungeeChatReceive";
    private MongoClient client;
    private Announcer announcer;
    private MongoMessage queue;

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        try {
            client = new MongoClient(getConfig().getString("mongoAddress"));
            queue = new MongoMessage(client.getDB("messages").getCollection("herochatmessages"), getConfig().getInt("serverID"));
            announcer = new Announcer();
            Bukkit.getScheduler().runTaskAsynchronously(this, announcer);
            Bukkit.getPluginManager().registerEvents(new BungeeListener(this), this);
        } catch (UnknownHostException e) {
            e.printStackTrace();
            getLogger().severe("Unable to connect to MongoDB! Plugin will be crippled in features!");
        }
    }


    public void onDisable() {
       announcer.poison();
    }

    public void sendMessage(String channel, String prefix, String username, String message) {
        queue.send(CHANNEL_NAME_SEND, getConfig().getInt("serverID") + ":" + channel + ":" + prefix + ":" + username + ":" + message);
    }

    private class Announcer implements Runnable {

        private boolean end = false;

        @Override
        public void run() {
            while (!end) {
                BasicDBObject message = queue.get();
                if (message != null) {
                    queue.ack(message);
                    if (message.containsField(CHANNEL_NAME_RECEIVE)) {
                        String[] messages = ((String)message.get(CHANNEL_NAME_RECEIVE)).split(":", 5);
                        String server = messages[0];
                        if (!server.equals(getConfig().getString("serverID"))) {
                            String channelName = messages[1];
                            String rank = messages[2];
                            String nickname = messages[3];
                            String playerMessage = messages[4];
                            playerMessage = ChatColor.translateAlternateColorCodes('&', playerMessage);
                            String rankMessage = ChatColor.translateAlternateColorCodes('&', rank);
                            String playerNickname = ChatColor.translateAlternateColorCodes('&', nickname);
                            Channel herochatChannel = Herochat.getChannelManager().getChannel(channelName);
                            if (herochatChannel == null)
                            {
                                Bukkit.getLogger().warning("Channel "+channelName+" doesn't exist, but a message was receieved on it. Your Herochat configs aren't probably the same on each server.");
                                return;
                            }
                            herochatChannel.sendRawMessage(herochatChannel.getColor() + "[" + herochatChannel.getNick() + "] " + ChatColor.RESET + rankMessage + playerNickname + ChatColor.RESET + ": " + playerMessage);

                        }
                    }
                }
            }
        }

        public void poison() {
            end = true;
        }
    }


}
