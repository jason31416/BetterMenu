package cn.jason31416.betterMenu.chatinput;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.general.Pair;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ListInputCommand {
    private static final Map<SimplePlayer, Pair<Consumer<List<String>>, List<String>>> listSessions = new HashMap<>();

    public static void startListInput(SimplePlayer player, Consumer<List<String>> callback, List<String> initialLines){
        listSessions.put(player, Pair.of(callback, initialLines));
        displayList(player);
    }

    private static void displayList(SimplePlayer player){
        if(listSessions.containsKey(player)){
            List<String> lines = listSessions.get(player).second();
            player.sendMessage(Lang.getMessage("input.lore.header"));
            int i = 0;
            for(String line: lines){
                player.sendMessage(Lang.getMessage("input.lore.line").add("content", Message.of(line).toFormatted()).add("line", i++));
            }
            player.sendMessage(Lang.getMessage("input.lore.footer"));
        }
    }

    public static void registerCommand(){
        PlanetLib.instance.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (commands) -> ((Commands)commands.registrar()).register("bloreinput", new BasicCommand() {
            public void execute(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
                if(!(commandSourceStack.getSender() instanceof Player p)) return;
                SimplePlayer pl = SimplePlayer.of(p);
                if(args.length>0&&listSessions.containsKey(pl)){
                    if(args[0].equalsIgnoreCase("edit")||args[0].equalsIgnoreCase("add")){
                        int index;
                        if(args.length>1) {
                            try {
                                index = Integer.parseInt(args[1]);
                            } catch (NumberFormatException e) {
                                return;
                            }
                        }else index = -1;
                        pl.sendMessage(Lang.getMessage("input.lore.edit-prompt"));
                        ChatInputHandler.addInputTask(pl, s->{
                            if(!listSessions.containsKey(pl)) return;
                            if(s==null) {
                                listSessions.remove(pl).first().accept(null);
                            }
                            if(index>=args.length||index==-1){
                                listSessions.get(pl).second().add(s);
                            }else{
                                listSessions.get(pl).second().set(index, s);
                            }
                            displayList(pl);
                        });
                    }else if(args[0].equalsIgnoreCase("remove")){
                        if(args.length>1){
                            int index;
                            try {
                                index = Integer.parseInt(args[1]);
                            }catch (NumberFormatException e){
                                return;
                            }
                            if(index<listSessions.get(pl).second().size()){
                                listSessions.get(pl).second().remove(index);
                            }
                            displayList(pl);
                        }
                    }else if(args[0].equalsIgnoreCase("cancel")){
                        listSessions.remove(pl).first().accept(null);
                    }else if(args[0].equalsIgnoreCase("submit")){
                        listSessions.get(pl).first().accept(listSessions.get(pl).second());
                        listSessions.remove(pl);
                    }
                }
            }

            public @NotNull Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
                if(args.length<=1) return List.of("edit", "remove", "cancel", "submit", "add");
                return List.of();
            }
        }));
    }
}
