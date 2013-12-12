package com.jumanjicraft.BungeeChatClient;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.Herochat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

public class BungeeChatClient extends JavaPlugin {

    private JedisPool jedisPool;
    private final String CHANNEL_NAME_SEND = "BungeeChatSend", CHANNEL_NAME_RECEIVE = "BungeeChatReceive";
    private PubSubListener psl;

    public void onEnable() {
        getConfig().options().copyDefaults(true);
        saveConfig();
        jedisPool = new JedisPool(new JedisPoolConfig(), getConfig().getString("jedisAddress"));
        if (jedisPool == null) {
            getLogger().severe("Redis not found! Disabling...");
            getPluginLoader().disablePlugin(this);
        }
        psl = new PubSubListener();
        Bukkit.getScheduler().runTaskAsynchronously(this, psl);
        Bukkit.getPluginManager().registerEvents(new BungeeListener(this), this);
    }


    public void onDisable() {
        psl.poison();
    }

    public JedisPool getPool() {
        return jedisPool;
    }

    public void sendMessage(String channel, String prefix, String username, String message) {
        Jedis rsc = jedisPool.getResource();
        rsc.publish(CHANNEL_NAME_SEND, channel + ":" + prefix + ":" + username + "message" + message);
    }

    private class PubSubListener implements Runnable {

        private Jedis rsc;
        private JedisPubSubHandler jpsh;

        @Override
        public void run() {
            try {
                rsc = jedisPool.getResource();
                jpsh = new JedisPubSubHandler();
                rsc.subscribe(jpsh, CHANNEL_NAME_RECEIVE);
            } catch (JedisException ignored) {
            }
        }

        public void poison() {
            jpsh.unsubscribe();
            jedisPool.returnResource(rsc);
        }
    }

    private class JedisPubSubHandler extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            if (channel.equals(CHANNEL_NAME_RECEIVE)) {
                String[] messages = message.split(":", 4);
                String channelName = messages[0];
                String rank = messages[1];
                String nickname = messages[2];
                String playerMessage = messages[3];
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

        @Override
        public void onPMessage(String s, String s2, String s3) {
        }

        @Override
        public void onSubscribe(String s, int i) {
        }

        @Override
        public void onUnsubscribe(String s, int i) {
        }

        @Override
        public void onPUnsubscribe(String s, int i) {
        }

        @Override
        public void onPSubscribe(String s, int i) {
        }
    }


}
