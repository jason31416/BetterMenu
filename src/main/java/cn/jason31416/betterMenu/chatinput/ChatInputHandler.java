package cn.jason31416.betterMenu.chatinput;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.PluginLogger;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.event.player.ChatEvent;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ChatInputHandler implements Listener {
    private static final Map<SimplePlayer, Consumer<String>> inputTasks = new HashMap<>();

    public static void addInputTask(SimplePlayer pl, Consumer<String> callback){
        if(inputTasks.containsKey(pl)) inputTasks.get(pl).accept(null);
        inputTasks.put(pl, callback);
    }

    public static void registerInputCommand(){
        PlanetLib.instance.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (commands) -> ((Commands)commands.registrar()).register("binput", new BasicCommand() {
            public void execute(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
                if(!(commandSourceStack.getSender() instanceof Player p)) return;
                SimplePlayer pl = SimplePlayer.of(p);
                if(args.length>0){
                    if(inputTasks.containsKey(pl)){
                        if(args[0].equalsIgnoreCase("cancel")){
                            pl.sendMessage(Lang.getMessage("command.input.cancelled"));
                            inputTasks.remove(pl).accept(null);
                        }else{
                            inputTasks.remove(pl).accept(String.join(" ", args));
                        }
                    }else{
                        pl.sendMessage(Lang.getMessage("command.input.not-in-input"));
                    }
                }
            }

            public @NotNull Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
                return List.of("cancel", "<value>");
            }
        }));
    }

    @EventHandler @SuppressWarnings("deprecation")
    public void onPlayerChat(ChatEvent event){
//        PluginLogger.send(Message.of(event.message()));
        SimplePlayer pl = SimplePlayer.of(event.getPlayer());
        if(inputTasks.containsKey(pl)){
            String message = Message.of(event.message()).toFormatted();
            if(message.equalsIgnoreCase("cancel")){
                pl.sendMessage(Lang.getMessage("command.input.cancelled"));
                inputTasks.remove(pl).accept(null);
            }else{
                inputTasks.remove(pl).accept(message);
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        SimplePlayer p = SimplePlayer.of(event.getPlayer());
        inputTasks.remove(p);
    }
}
