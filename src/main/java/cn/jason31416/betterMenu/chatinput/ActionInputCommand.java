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

import java.util.*;
import java.util.function.Consumer;

public class ActionInputCommand {
    private static final Map<SimplePlayer, Pair<Consumer<List<String>>, List<String>>> actionSessions = new HashMap<>();

    public static void startActionInput(SimplePlayer player, Consumer<List<String>> callback, List<String> initialLines){
        actionSessions.put(player, Pair.of(callback, initialLines));
        displayList(player);
    }

    private static void displayList(SimplePlayer player){
        if(actionSessions.containsKey(player)){
            List<String> lines = actionSessions.get(player).second();
            player.sendMessage(Lang.getMessage("input.action.header"));
            int i = 0;
            for(String line: lines){
                player.sendMessage(Lang.getMessage("input.action.line").add("content", Message.of(line).toFormatted()).add("line", i++));
            }
            player.sendMessage(Lang.getMessage("input.action.footer"));
        }
    }

    public static void registerCommand(){
        PlanetLib.instance.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (commands) -> ((Commands)commands.registrar()).register("bactioninput", new BasicCommand() {
            public void execute(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
                if(!(commandSourceStack.getSender() instanceof Player p)) return;
                SimplePlayer pl = SimplePlayer.of(p);
                if(args.length>0&&actionSessions.containsKey(pl)){
                    if(args[0].equalsIgnoreCase("add")){
                        if(args.length == 1)
                            pl.sendMessage(Lang.getMessageList("input.action.add-prompt.type"));
                        else if(args.length == 2 && List.of("run", "run-server", "message", "open", "server", "custom").contains(args[1].toLowerCase(Locale.ROOT))) {
                            if(args[1].equalsIgnoreCase("custom")){
                                pl.sendMessage(Lang.getMessageList("input.action.add-prompt.custom"));
                                ChatInputHandler.addInputTask(pl, s->{
                                    if(s==null) displayList(pl);
                                    else{
                                        actionSessions.get(pl).second().add(s);
                                        displayList(pl);
                                    }
                                });
                            }else {
                                switch (args[1].toLowerCase(Locale.ROOT)) {
                                    case "run", "run-server" -> {
                                        pl.sendMessage(Lang.getMessage("input.action.add-prompt.parameter.command"));
                                    }
                                    case "message" -> {
                                        pl.sendMessage(Lang.getMessage("input.action.add-prompt.parameter.message"));
                                    }
                                    case "open" -> {
                                        pl.sendMessage(Lang.getMessage("input.action.add-prompt.parameter.gui"));
                                    }
                                    case "server" -> {
                                        pl.sendMessage(Lang.getMessage("input.action.add-prompt.parameter.server"));
                                    }
                                }
                                ChatInputHandler.addInputTask(pl, s -> {
                                    if (s == null) displayList(pl);
                                    else {
                                        pl.sendMessage(Lang.getMessageList("input.action.add-prompt.condition").add("prev", String.join(" ", args)+" "+s));
                                    }
                                });
                            }
                        }else{
                            if(List.of("a#none", "a#left-click", "a#right-click").contains(args[args.length-1])){
                                StringBuilder sb = new StringBuilder();
                                if(!args[args.length-1].equalsIgnoreCase("a#none"))
                                    sb.append(args[args.length-1].substring(2)).append(" ");
                                for(int i=1;i<args.length-1;i++){
                                    sb.append(args[i]).append(" ");
                                }
                                actionSessions.get(pl).second().add(sb.toString());
                                displayList(pl);
                            }else{
                                pl.sendMessage(Lang.getMessageList("input.action.add-prompt.condition").add("prev", String.join(" ", args)));
                            }
                        }

                    }else if(args[0].equalsIgnoreCase("list")){
                        displayList(pl);
                    }else if(args[0].equalsIgnoreCase("cancel")){
                        actionSessions.remove(pl).first().accept(null);
                    }else if(args[0].equalsIgnoreCase("submit")){
                        actionSessions.get(pl).first().accept(actionSessions.get(pl).second());
                        actionSessions.remove(pl);
                    }
                }
            }

            public @NotNull Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
                if(args.length<=1) return List.of("cancel", "list", "submit", "add");
                return List.of();
            }
        }));
    }
}
