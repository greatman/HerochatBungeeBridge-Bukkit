package com.jumanjicraft.BungeeChatClient;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.Herochat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.exceptions.JedisException;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class BungeeChatClient extends JavaPlugin {

    private String user;
    private String pass;
    private String url;
    private MySQLPool pool;
    private boolean connected = true;
    protected Map<String, Integer> tempmutes = new HashMap<String, Integer>();
    private JedisPool jedisPool;
    private final String CHANNEL_NAME_SEND = "BungeeChatSend", CHANNEL_NAME_RECEIVE = "BungeeChatReceive";
    private PubSubListener psl;

    public void onEnable() {
        loadConfig();
        connectToDB();
        if (!connected) {

            return;
        }
        jedisPool = new JedisPool(new JedisPoolConfig(), getConfig().getString("jedisAddress"));
        if (pool == null) {
            getLogger().severe("Redis not found! Disabling...");
            getPluginLoader().disablePlugin(this);
        }
        psl = new PubSubListener();
        Bukkit.getScheduler().runTaskAsynchronously(this, psl);
        createTables();
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
    public boolean onCommand(CommandSender sc, Command cmd, String label, String args[]) {
        if (cmd.getName().equalsIgnoreCase("nickname")) {
            if (args.length == 1) {
                if (sc instanceof Player) {
                    if (sc.hasPermission("BungeeChat.nickname")) {
                        setNickName((Player) sc, args[0]);
                    } else {
                        sc.sendMessage(ChatColor.RED + "You don't have permission.");
                    }
                } else {
                    sc.sendMessage(ChatColor.RED + "No player specified!");
                }
            } else if (args.length > 1) {
                Player player = Bukkit.getPlayer(args[0]);
                if (player != null) {
                    if (sc.hasPermission("BungeeChat.nickname.others")) {
                        setNickName(player, args[1]);
                    } else {
                        sc.sendMessage(ChatColor.RED + "You don't have permission.");
                    }
                } else {
                    sc.sendMessage(ChatColor.RED + "Player not online.");
                }
            } else {
                sc.sendMessage(ChatColor.RED + "Usage: /nickname [nick/off] or /nickname [player] [nick/off]");
            }
        }
        return true;
    }

    public void loadConfig() {
        this.saveDefaultConfig();
        user = getConfig().getString("user");
        pass = getConfig().getString("pass");
        String host = getConfig().getString("host");
        String database = getConfig().getString("database");
        int port = getConfig().getInt("port");
        url = "jdbc:mysql://" + host + ":" + port + "/" + database;
    }

    public void setNickName(Player player, String nick) {
        if (nick.equals("") || nick.equalsIgnoreCase("off")) {
            // Unset
            removeNickName(player.getName());
        } else {
            if (nick.matches("^[a-zA-Z_0-9&]+$")) {
                if (nick.length() <= 16) {
                    nick = ChatColor.translateAlternateColorCodes('&', nick);
                    if (!checkNickNameUsed(nick)) {
                        setNickName(player.getName(), nick);
                        player.setDisplayName("*" + nick);
                        player.sendMessage(ChatColor.YELLOW + "Your nickname has been changed to " + ChatColor.RESET + "*" + nick);
                    } else {
                        player.sendMessage(ChatColor.RED + "That nickname is already used.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "That nickname is too long!");
                }
            } else {
                player.sendMessage(ChatColor.RED + "Nicknames can only contain letters, numbers, colours (&) and underscores (_).");
            }

        }

    }

    public boolean checkNickNameUsed(String nickname) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet results = null;
        try {
            conn = this.getConnection();
            if (conn == null) {
                throw new SQLException("Could not fetch a connection.");
            }
            state = conn.prepareStatement("SELECT COUNT(*) FROM bhc_nicknames WHERE name=? OR nick=?");
            state.setString(1, nickname);
            state.setString(2, nickname);
            results = state.executeQuery();
            if (results.next()) {
                return results.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(results);
            closeQuietly(state);
            closeQuietly(conn);
        }
        return false;
    }

    public void setNickName(String player, String nickname) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = this.getConnection();
            if (conn == null) {
                throw new SQLException("Could not fetch a connection.");
            }
            state = conn.prepareStatement("INSERT INTO bhc_nicknames (name, nick) VALUES (?,?) ON DUPLICATE KEY UPDATE nick=?");
            state.setString(1, player);
            state.setString(2, nickname);
            state.setString(3, nickname);
            state.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(state);
            closeQuietly(conn);
        }
    }

    public void removeNickName(String player) {
        Connection conn = null;
        PreparedStatement state = null;
        try {
            conn = this.getConnection();
            if (conn == null) {
                throw new SQLException("Could not fetch a connection.");
            }
            state = conn.prepareStatement("DELETE FROM bhc_nicknames where name=?");
            state.setString(1, player);
            state.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(state);
            closeQuietly(conn);
        }
    }

    public String getNickName(String player) {
        Connection conn = null;
        PreparedStatement state = null;
        ResultSet results = null;
        try {
            conn = this.getConnection();
            if (conn == null) {
                throw new SQLException("Could not fetch a connection.");
            }
            state = conn.prepareStatement("SELECT nick FROM bhc_nicknames WHERE name=?");
            state.setString(1, player);
            results = state.executeQuery();
            if (results.next()) {
                return results.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(results);
            closeQuietly(state);
            closeQuietly(conn);
        }
        return null;
    }
	
	/*public void tempMute(Player player, int minutes)
	{
		Connection conn = null;
		PreparedStatement state = null;
		try {
			conn = this.getConnection();
			if (conn == null)
			{
				throw new SQLException("Could not fetch a connection.");
			}
			state = conn.prepareStatement("INSERT IGNORE INTO bhc_mutes (name,time) VALUES (?,?)");
			state.setString(1, player.getName());
			state.setInt(2, minutes);
			state.execute();
			newMuteTask(player);
			hcMute(player, true);
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeQuietly(state);
			closeQuietly(conn);
		}
	}
	
	public void mute(Player player, boolean ismuted)
	{
		if (ismuted)
		{
			Connection conn = null;
			PreparedStatement state = null;
			try {
				conn = this.getConnection();
				if (conn == null)
				{
					throw new SQLException("Could not fetch a connection.");
				}
				state = conn.prepareStatement("INSERT IGNORE INTO bhc_mutes (name) VALUES (?)");
				state.setString(1, player.getName());
				state.execute();
				hcMute(player, true);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeQuietly(state);
				closeQuietly(conn);
			}
		} else {
			Connection conn = null;
			PreparedStatement state = null;
			try {
				conn = this.getConnection();
				if (conn == null)
				{
					throw new SQLException("Could not fetch a connection.");
				}
				state = conn.prepareStatement("DELETE FROM bhc_mutes WHERE name=?");
				state.setString(1, player.getName());
				state.execute();
				hcMute(player, false);
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				closeQuietly(state);
				closeQuietly(conn);
			}
		}
		
	}
	
	private void subtractMinute(Player player)
	{
		Connection conn = null;
		PreparedStatement state = null;
		try {
			conn = this.getConnection();
			if (conn == null)
			{
				throw new SQLException("Could not fetch a connection.");
			}
			state = conn.prepareStatement("UPDATE bhc_mutes SET time=time-1 WHERE name=?");
			state.setString(1, player.getName());
			state.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeQuietly(state);
			closeQuietly(conn);
		}
	}
	
	public int getMuteMinutes(Player player)
	{
		Connection conn = null;
		PreparedStatement state = null;
		ResultSet results = null;
		try {
			conn = this.getConnection();
			if (conn == null)
			{
				throw new SQLException("Could not fetch a connection.");
			}
			state = conn.prepareStatement("SELECT time FROM bhc_mutes WHERE name=?");
			state.setString(1, player.getName());
			results = state.executeQuery();
			if (results.next())
			{
				return results.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			closeQuietly(results);
			closeQuietly(state);
			closeQuietly(conn);
		}
		return -1;
	}
	
	public int newMuteTask(Player player)
	{
		final String playername = player.getName();
		int id = this.getServer().getScheduler().scheduleSyncRepeatingTask(this, new BukkitRunnable() {
			public void run()
			{
				Player theplayer = Bukkit.getPlayer(playername);
				if (theplayer != null)
				{
					if (getMuteMinutes(theplayer) == 1)
					{
						mute(theplayer, false);
						hcMute(theplayer, false);
						theplayer.sendMessage(ChatColor.YELLOW+"You are now unmuted.");
					} else {
						subtractMinute(theplayer);
					}
				} else {
					this.cancel();
				}
			}
		}, 1200L, 1200L);
		tempmutes.put(player.getName(), id);
		return id;
	}
	
	public void hcMute(Player player, boolean ismuted)
	{
		Herochat.getChatterManager().getChatter(player).setMuted(ismuted, true);
	}*/

    public void connectToDB() {
        try {
            pool = new MySQLPool(url, user, pass);
            Connection conn = getConnection();
            if (conn == null) {
                this.getLogger().severe("Could not connect to database!");
                this.getPluginLoader().disablePlugin(this);
                return;
            }
            conn.close();
            this.getLogger().info("Connected to MySQL database.");
            this.connected = true;
        } catch (Exception e) {
            e.printStackTrace();
            this.getPluginLoader().disablePlugin(this);
        }
    }

    private void createTables() {
        Connection conn = null;
        Statement state = null;
        try {
            conn = this.getConnection();
            if (conn == null) {
                throw new SQLException("Could not fetch a connection.");
            }
            state = conn.createStatement();
            state.execute("CREATE TABLE IF NOT EXISTS bhc_nicknames (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(16) UNIQUE NOT NULL, nick VARCHAR(16) UNIQUE)");
            //state.execute("CREATE TABLE IF NOT EXISTS bhc_mutes (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(16) UNIQUE NOT NULL, time INT)");
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(state);
            closeQuietly(conn);
        }
    }

    public static void closeQuietly(AutoCloseable resource) {
        try {
            if (resource != null) {
                resource.close();
            }
        } catch (Exception e) {

        }
    }

    public Connection getConnection() {
        Connection conn = null;
        try {
            conn = this.pool.getConnection();
            if (connected == false) {
                this.getLogger().info("Could not reopen an SQL connection.");
                connected = true;
            }
            return conn;
        } catch (Exception e) {
            if (this.connected) {
                this.getLogger().log(Level.SEVERE, "Could not fetch an SQL connection.", e);
                this.connected = false;
            } else {
                this.getLogger().severe("SQL connection lost!");
            }
            closeQuietly(conn);
            return null;
        }
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
