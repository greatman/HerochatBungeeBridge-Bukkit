package com.jumanjicraft.BungeeChatClient;

import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

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
}
