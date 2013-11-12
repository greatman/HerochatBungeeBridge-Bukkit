package com.jumanjicraft.BungeeChatClient;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.dthielke.herochat.Channel;
import com.dthielke.herochat.Herochat;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

public class BungeeChatListener implements PluginMessageListener {

        static BungeeChatClient plugin;
 
        public BungeeChatListener(BungeeChatClient plugin) {
                BungeeChatListener.plugin = plugin;
                Bukkit.getMessenger().registerIncomingPluginChannel(plugin, "BungeeChat", this);
                Bukkit.getMessenger().registerOutgoingPluginChannel(plugin, "BungeeChat");
        }
 
        public static void TransmitChatMessage(String message, String chatchannel) {
               ByteArrayDataOutput out = ByteStreams.newDataOutput();
               out.writeUTF(chatchannel);
               out.writeUTF(message);
               Bukkit.getOnlinePlayers()[0].sendPluginMessage(plugin, "BungeeChat", out.toByteArray());
        }
 
        public void onPluginMessageReceived(String s, Player player, byte[] bytes) {
			if (!s.equalsIgnoreCase("BungeeChat"))
			{
				return;
			}
        	ByteArrayDataInput in = ByteStreams.newDataInput(bytes);
        	String chatchannel = in.readUTF();
        	String message = in.readUTF();
        	message = ChatColor.translateAlternateColorCodes('&', message);
        	Channel channel = Herochat.getChannelManager().getChannel(chatchannel);
        	if (channel == null)
        	{
        		Bukkit.getLogger().warning("Channel "+chatchannel+" doesn't exist, but a message was receieved on it. Your Herochat configs aren't probably the same on each server.");
				return;
        	} 
        	StringBuilder msg = new StringBuilder(channel.applyFormat(channel.getFormatSupplier().getAnnounceFormat(), "").replace("%2$s", message.replaceAll("(?i)&([a-fklmno0-9])", "\247$1")));
        	if (channel.getFormat().startsWith("["))
        	{
        		msg.deleteCharAt(2);
        		channel.sendRawMessage(ChatColor.GREEN+"[N"+ChatColor.RESET+msg);
        	} else {
        		channel.sendRawMessage(ChatColor.GREEN+"[N]"+ChatColor.RESET+msg);
        	}
        }
        
}