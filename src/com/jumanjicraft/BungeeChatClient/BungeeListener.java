package com.jumanjicraft.BungeeChatClient;

import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BungeeListener implements Listener {
	
	private BungeeChatClient plugin;

    private List<String> antiSpam = new ArrayList<String>();
	public BungeeListener(BungeeChatClient plugin)
	{
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onChannelChat(final ChannelChatEvent event)
	{
		if (event.getResult() == Chatter.Result.ALLOWED)
		{
            if (!antiSpam.contains(event.getSender().getName())) {
                if (!event.getFormat().equalsIgnoreCase(Herochat.getChannelManager().getConversationFormat()))
                {
                    antiSpam.add(event.getSender().getName());
                    Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                        @Override
                        public void run() {
                           antiSpam.remove(event.getSender().getName());
                        }
                    }, 40);
                    plugin.sendMessage(event.getChannel().getName(), Herochat.getChatService().getPlayerPrefix(event.getSender().getPlayer()), event.getSender().getPlayer().getDisplayName(), event.getMessage());
                }
            } else {
                event.setResult(Chatter.Result.MUTED);
            }

		}
	}


}
