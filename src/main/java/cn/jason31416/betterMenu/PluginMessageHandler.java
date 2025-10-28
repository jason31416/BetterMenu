package cn.jason31416.betterMenu;

import cn.jason31416.planetlib.PlanetLib;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class PluginMessageHandler {
    public static void sendBungeeCordMessage(String channel, Player player, Object... message) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (Object s : message) {
            if(s instanceof String s1)
                out.writeUTF(s1);
            else if(s instanceof byte[] s2){
                out.write(s2);
            }else if(s instanceof Short s3){
                out.writeShort(s3);
            }
        }

        player.sendPluginMessage(PlanetLib.instance, channel, out.toByteArray());
    }

    public static void registerBungeeCordListener() {
        Bukkit.getServer().getMessenger().registerOutgoingPluginChannel(PlanetLib.instance, "BungeeCord");
//        Bukkit.getServer().getMessenger().registerIncomingPluginChannel(PlanetLib.instance, "BungeeCord", this);
    }
}
