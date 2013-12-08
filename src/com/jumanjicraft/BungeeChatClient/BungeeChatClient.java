package com.jumanjicraft.BungeeChatClient;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class BungeeChatClient extends JavaPlugin {
	
	private String user;
	private String pass;
	private String url;
	private MySQLPool pool;
	private boolean connected = true;
	protected Map<String, Integer> tempmutes = new HashMap<String, Integer>();
	
	public void onEnable()
	{
		loadConfig();
		connectToDB();
		if (!connected)
		{
			return;
		}
		createTables();
		new BungeeChatListener(this);
		Bukkit.getPluginManager().registerEvents(new BungeeListener(this), this);
	}
	
	public boolean onCommand(CommandSender sc, Command cmd, String label, String args[])
	{
		if (cmd.getName().equalsIgnoreCase("nickname")) {
			if (args.length == 1)
			{
				if (sc instanceof Player)
				{
					if (sc.hasPermission("BungeeChat.nickname") || sc.hasPermission("essentials.nick"))
					{
						setNickName((Player) sc, args[0]);
					} else {
						sc.sendMessage(ChatColor.RED+"You don't have permission.");
					}
				} else {
					sc.sendMessage(ChatColor.RED+"No player specified!");
				}
			} else if (args.length > 1) {
				Player player = Bukkit.getPlayer(args[0]);
				if (player != null)
				{
					if (sc.hasPermission("BungeeChat.nickname.others") || sc.hasPermission("essentials.nick.others"))
					{
						setNickName(player, args[1]);
					} else {
						sc.sendMessage(ChatColor.RED+"You don't have permission.");
					}
				} else {
					sc.sendMessage(ChatColor.RED+"Player not online.");
				}
			} else {
				sc.sendMessage(ChatColor.RED+"Usage: /nickname [nick/off] or /nickname [player] [nick/off]");
			}
		}/* else if (cmd.getName().equalsIgnoreCase("mute")) {
			if (args.length > 0)
			{
				if (sc.hasPermission("BungeeChat.mute"))
				{
					Player player = Bukkit.getPlayer(args[0]);
					if (player != null)
					{
						if (getMuteMinutes(player) > -1)
						{
							sc.sendMessage(ChatColor.RED+"Player "+player.getName()+" is already muted.");
						} else {
							mute(player, true);
							player.sendMessage(ChatColor.YELLOW+"You are now muted.");
							sc.sendMessage(ChatColor.YELLOW+"Player "+player.getName()+" muted.");
						}
					} else {
						sc.sendMessage(ChatColor.RED+"Player not online.");
					}
				} else {
					sc.sendMessage(ChatColor.RED+"You don't have permission.");
				}
			} else {
				sc.sendMessage(ChatColor.RED+"Usage: /mute [player]");
			}
		} else if (cmd.getName().equalsIgnoreCase("unmute")) {
			if (args.length > 0)
			{
				if (sc.hasPermission("BungeeChat.mute"))
				{
					Player player = Bukkit.getPlayer(args[0]);
					if (player != null)
					{
						if (getMuteMinutes(player) > -1)
						{
							mute(player, false);
							player.sendMessage(ChatColor.YELLOW+"You are now unmuted.");
							sc.sendMessage(ChatColor.YELLOW+"Player "+player.getName()+" unmuted.");
						} else {
							sc.sendMessage(ChatColor.RED+"Player "+player.getName()+" is not muted.");
						}
					} else {
						sc.sendMessage(ChatColor.RED+"Player not online.");
					}
				} else {
					sc.sendMessage(ChatColor.RED+"You don't have permission.");
				}
			} else {
				sc.sendMessage(ChatColor.RED+"Usage: /unmute [player]");
			}
		} else if (cmd.getName().equalsIgnoreCase("tempmute")) {
			if (args.length > 1)
			{
				if (sc.hasPermission("BungeeChat.mute"))
				{
					Player player = Bukkit.getPlayer(args[0]);
					if (player != null)
					{
						int minutes;
						try {
							minutes = Integer.parseInt(args[1]);
						} catch (NumberFormatException e) {
							sc.sendMessage(ChatColor.RED+"Could not parse number of minutes.");
							return true;
						}
						if (getMuteMinutes(player) > -1)
						{
							sc.sendMessage(ChatColor.RED+"Player "+player.getName()+" is already muted.");
						} else {
							tempMute(player, minutes);
							player.sendMessage(ChatColor.YELLOW+"You are now muted for "+minutes+" minute(s).");;
							sc.sendMessage(ChatColor.YELLOW+"Player "+player.getName()+" muted for "+minutes+" minute(s).");
						}
					} else {
						sc.sendMessage(ChatColor.RED+"Player not online.");
					}
				} else {
					sc.sendMessage(ChatColor.RED+"You don't have permission.");
				}
			} else {
				sc.sendMessage(ChatColor.RED+"Usage: /tempmute [player] [minutes]");
			}
		} else if (cmd.getName().equalsIgnoreCase("muted")) {
			int minutes;
			if (args.length > 0)
			{
				Player player = Bukkit.getPlayer(args[0]);
				if (player != null)
				{
					minutes = getMuteMinutes(player);
				} else {
					sc.sendMessage(ChatColor.RED+"Player not online.");
					return true;
				}
			} else {
				if (sc instanceof Player)
				{
					minutes = getMuteMinutes((Player) sc);
				} else {
					sc.sendMessage(ChatColor.RED+"No player specified!");
					return true;
				}
			}
			if (minutes == -1)
			{
				sc.sendMessage(ChatColor.YELLOW+"You are not muted.");
			} else if (minutes == 0) {
				sc.sendMessage(ChatColor.YELLOW+"You are muted.");
			} else {
				sc.sendMessage(ChatColor.YELLOW+"You are muted for "+minutes+" minute(s).");
			}
		}*/
		return true;
	}
	
	public void loadConfig()
	{
		this.saveDefaultConfig();
		user = getConfig().getString("user");
		pass = getConfig().getString("pass");
		String host = getConfig().getString("host");
		String database = getConfig().getString("database");
		int port = getConfig().getInt("port");
		url = "jdbc:mysql://"+host+":"+port+"/"+database;
	}
	
	public void setNickName(Player player, String nick)
	{
		if (nick.equals("") || nick.equalsIgnoreCase("off"))
		{
			// Unset
			removeNickName(player.getName());
		} else {
			if (nick.matches("^[a-zA-Z_0-9&]+$"))
			{
				if (ChatColor.stripColor(nick).length() <= 16)
				{
					nick = ChatColor.translateAlternateColorCodes('&', nick);
					if (!checkNickNameUsed(nick))
					{
						setNickName(player.getName(), nick);
						player.setDisplayName("*"+nick);
						player.sendMessage(ChatColor.YELLOW+"Your nickname has been changed to "+ChatColor.RESET+"*"+nick);
					} else {
						player.sendMessage(ChatColor.RED+"That nickname is already used.");
					}
				} else {
					player.sendMessage(ChatColor.RED+"That nickname is too long! Maximum length is 16 letters, does not include colours.");
				}
			} else {
				player.sendMessage(ChatColor.RED+"Nicknames can only contain letters, numbers, colours (&) and underscores (_).");
			}
			
		}
		
	}
	
	public boolean checkNickNameUsed(String nickname)
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
			state = conn.prepareStatement("SELECT COUNT(*) FROM bhc_nicknames WHERE name=? OR nick=?");
			state.setString(1, nickname);
			state.setString(2, nickname);
			results = state.executeQuery();
			if (results.next())
			{
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
	
	public void setNickName(String player, String nickname)
	{
		Connection conn = null;
		PreparedStatement state = null;
		try {
			conn = this.getConnection();
			if (conn == null)
			{
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
	
	public void removeNickName(String player)
	{
		Connection conn = null;
		PreparedStatement state = null;
		try {
			conn = this.getConnection();
			if (conn == null)
			{
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
	
	public String getNickName(String player)
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
			state = conn.prepareStatement("SELECT nick FROM bhc_nicknames WHERE name=?");
			state.setString(1, player);
			results = state.executeQuery();
			if (results.next())
			{
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
	
	public void connectToDB()
	{
		try {
			pool = new MySQLPool(url, user, pass);
			Connection conn = getConnection();
			if (conn == null)
			{
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
	
	private void createTables()
	{
		Connection conn = null;
		Statement state = null;
		try {
			conn = this.getConnection();
			if (conn == null)
			{
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
	
	public static void closeQuietly(AutoCloseable resource)
	{
		try {
			if (resource != null)
			{
				resource.close();
			}
		} catch (Exception e) {
			
		}
	}
	
	public Connection getConnection()
	{
		Connection conn = null;
		try
		{
			conn = this.pool.getConnection();
			if (connected == false)
			{
				this.getLogger().info("Could not reopen an SQL connection.");
				connected = true;
			}
			return conn;
		} catch (Exception e) {
			if (this.connected)
			{
				this.getLogger().log(Level.SEVERE, "Could not fetch an SQL connection.", e);
				this.connected = false;
			} else {
				this.getLogger().severe("SQL connection lost!");
			}
			closeQuietly(conn);
			return null;
		}
	}

}
