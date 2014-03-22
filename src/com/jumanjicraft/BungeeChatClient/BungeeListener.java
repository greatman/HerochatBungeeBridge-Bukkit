package com.jumanjicraft.BungeeChatClient;

import com.dthielke.herochat.ChannelChatEvent;
import com.dthielke.herochat.Chatter;
import com.dthielke.herochat.Herochat;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BungeeListener implements Listener {
	
	private BungeeChatClient plugin;

    //private List<String> antiSpam = new ArrayList<String>();
    private Map<String, BukkitTask> antiSpam = new HashMap<String, BukkitTask>();
	public BungeeListener(BungeeChatClient plugin)
	{
		this.plugin = plugin;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onChannelChat(final ChannelChatEvent event)
	{
        if (event.getResult() == Chatter.Result.ALLOWED)
		{
            if (!antiSpam.containsKey(event.getSender().getName())) {
                if (!event.getFormat().equalsIgnoreCase(Herochat.getChannelManager().getConversationFormat()))
                {
                    generateTask(event.getSender().getName());
                    plugin.sendMessage(event.getChannel().getName(), Herochat.getChatService().getPlayerPrefix(event.getSender().getPlayer()), event.getSender().getPlayer().getDisplayName(), event.getMessage());
                }
            } else {
                antiSpam.get(event.getSender().getName()).cancel();
                generateTask(event.getSender().getName());
                event.setResult(Chatter.Result.MUTED);
            }

		}
	}

    private void generateTask(final String player) {
        antiSpam.put(player, Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                antiSpam.remove(player);
            }
        }, 10));
    }


}
