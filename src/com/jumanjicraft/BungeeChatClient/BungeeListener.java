package com.jumanjicraft.BungeeChatClient;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;

public class BungeeListener implements Listener {
	
	private BungeeChatClient plugin;
	
	public BungeeListener(BungeeChatClient plugin)
	{
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onChannelChat(ChannelChatEvent event)
	{
		if (event.getResult() == Chatter.Result.ALLOWED)
		{
			if (!event.getFormat().equalsIgnoreCase(Herochat.getChannelManager().getConversationFormat()))
			{
                plugin.sendMessage(event.getChannel().getName(), Herochat.getChatService().getPlayerPrefix(event.getSender().getPlayer()), event.getSender().getPlayer().getDisplayName(), event.getMessage());
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		String nick = plugin.getNickName(event.getPlayer().getName());
		if (nick != null)
		{
			event.getPlayer().setDisplayName("*"+nick);
		}
		/*int mins = plugin.getMuteMinutes(event.getPlayer());
		if (mins > 0)
		{
			plugin.newMuteTask(event.getPlayer());
			plugin.hcMute(event.getPlayer(), true);
		} else if (mins == 0) {
			plugin.hcMute(event.getPlayer(), true);
		} else {
			plugin.hcMute(event.getPlayer(), false);
		}*/
	}
	
	/*@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event)
	{
		if (plugin.tempmutes.containsKey(event.getPlayer().getName()))
		{
			Bukkit.getScheduler().cancelTask(plugin.tempmutes.get(event.getPlayer().getName()));
			plugin.tempmutes.remove(event.getPlayer().getName());
		}
	}*/
}
