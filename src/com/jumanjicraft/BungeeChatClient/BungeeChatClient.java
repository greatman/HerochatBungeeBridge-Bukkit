package com.jumanjicraft.BungeeChatClient;

import com.dthielke.herochat.*;
import com.rabbitmq.client.*;
import com.rabbitmq.client.Channel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.CacheRequest;

public class BungeeChatClient extends JavaPlugin {

    private final String CHANNEL_NAME_SEND = "BungeeChatSend", CHANNEL_NAME_RECEIVE = "BungeeChatReceive";
    private Announcer announcer;
    private Channel channelSend;
    private Channel channelReceive;
    private QueueingConsumer consumer;
    private Connection connection = null;

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(getConfig().getString("amqpServer"));
        try {
            connection = factory.newConnection();
            channelSend = connection.createChannel();
            channelSend.exchangeDeclare(CHANNEL_NAME_SEND, "fanout");
            channelReceive = factory.newConnection().createChannel();
            channelReceive.exchangeDeclare(CHANNEL_NAME_RECEIVE, "fanout");
            String queueName = channelReceive.queueDeclare().getQueue();
            channelReceive.queueBind(queueName, CHANNEL_NAME_RECEIVE, "");
            consumer = new QueueingConsumer(channelReceive);
            channelReceive.basicConsume(queueName, true, consumer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onDisable() {
        try {
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        announcer.poison();
    }

    public void sendMessage(String channel, String prefix, String username, String message) {
        try {
            channelSend.basicPublish(CHANNEL_NAME_SEND, "", null, (getConfig().getInt("serverID") + ":" + channel + ":" + prefix + ":" + username + ":" + message).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class Announcer implements Runnable {

        private boolean end = false;

        @Override
        public void run() {
            while (!end) {
                try {
                    QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                    String message = new String(delivery.getBody());
                    String[] messages = message.split(":", 5);
                    String server = messages[0];
                    if (!server.equals(getConfig().getString("serverID"))) {
                        String channelName = messages[1];
                        String rank = messages[2];
                        String nickname = messages[3];
                        String playerMessage = messages[4];
                        playerMessage = ChatColor.translateAlternateColorCodes('&', playerMessage);
                        String rankMessage = ChatColor.translateAlternateColorCodes('&', rank);
                        String playerNickname = ChatColor.translateAlternateColorCodes('&', nickname);
                        com.dthielke.herochat.Channel herochatChannel = Herochat.getChannelManager().getChannel(channelName);
                        if (herochatChannel == null)
                        {
                            Bukkit.getLogger().warning("Channel "+channelName+" doesn't exist, but a message was receieved on it. Your Herochat configs aren't probably the same on each server.");
                            return;
                        }
                        herochatChannel.sendRawMessage(herochatChannel.getColor() + "[" + herochatChannel.getNick() + "] " + ChatColor.RESET + rankMessage + playerNickname + ChatColor.RESET + ": " + playerMessage);

                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        public void poison() {
            end = true;
        }
    }


}
