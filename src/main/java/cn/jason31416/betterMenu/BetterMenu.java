package cn.jason31416.betterMenu;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.Required;
import cn.jason31416.planetlib.command.ParameterType;
import cn.jason31416.planetlib.command.RootCommand;
import cn.jason31416.planetlib.gui.*;
import cn.jason31416.planetlib.gui.clickaction.ClickHandler;
import cn.jason31416.planetlib.gui.clickaction.DefaultClickActions;
import cn.jason31416.planetlib.gui.clickaction.RegisteredGUIRunnable;
import cn.jason31416.planetlib.gui.itemgroup.InventoryComponent;
import cn.jason31416.planetlib.gui.itemgroup.InventoryList;
import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.util.Config;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.PluginLogger;
import cn.jason31416.planetlib.util.Util;
import cn.jason31416.planetlib.util.general.Provider;
import cn.jason31416.planetlib.wrapper.SimpleItemStack;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import it.unimi.dsi.fastutil.Pair;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

public final class BetterMenu extends JavaPlugin {
    public static class BetterClickActions {
        @ClickHandler(id="server")
        public void serverAction(GUIRunnable.RunnableInvocation invocation, String[] args) {
            if(args.length < 1) return;
            String serverName = String.join(" ", args);

            PluginMessageHandler.sendBungeeCordMessage("BungeeCord", invocation.getPlayer().getPlayer(), "Connect", serverName);
        }
    }

    public static File guiFolder;

    public void reload(){
        Util.saveFolder("gui");
        Util.saveFolder("lang");
        reloadConfig();
        Config.start(this);
        Lang.init("lang/"+Config.get("lang", "en_us")+".yml");
        GUITemplate.clearLoaded();
        GUITemplate.loadFromDirectory(guiFolder);
    }

    @Override
    public void onEnable() {
        guiFolder = new File(getDataFolder(), "gui");
        if(Bukkit.getPluginManager().getPlugin("Vault") == null) {
            PlanetLib.initialize(this, Required.NBT, Required.PLACEHOLDERAPI);
        }else{
            PlanetLib.initialize(this, Required.NBT, Required.PLACEHOLDERAPI, Required.VAULT);
        }
        PluginMessageHandler.registerBungeeCordListener();
        RegisteredGUIRunnable.registerAll(new BetterClickActions());
        PluginLogger.info("Enabling BetterMenu...");
        Config.start(this);

        Util.saveFolder("gui");
        Util.saveFolder("lang");
        Lang.init("lang/"+Config.get("lang", "en_us")+".yml");
        GUITemplate.loadFromDirectory(guiFolder);

        ConfigurationSection section = Config.config.getConfigurationSection("bind-commands");
        if(section != null){
            for(String i: section.getKeys(false)) {
                if(GUITemplate.loadedTemplates.containsKey(section.getString(i))){
                    getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, (commands) -> ((Commands)commands.registrar()).register(i, new BasicCommand() {
                        public void execute(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
                            if(!(commandSourceStack.getSender() instanceof Player pl)) return;
                            new GUISession(SimplePlayer.of(pl)) {
                                @Override
                                public void setup(GUI gui) {}
                            }.display(GUITemplate.getGUI(section.getString(i)));
                        }

                        public @NotNull Collection<String> suggest(@NotNull CommandSourceStack commandSourceStack, String @NotNull [] args) {
                            return List.of();
                        }
                    }));
                }else{
                    PluginLogger.error("Unknown GUI with ID: "+section.getString(i));
                }
            }
        }

        RootCommand.builder("bettermenu")
                .addAlias("menu")
                .setDirectExecutor(ctx -> Message.of("&bRunning BetterMenu &a(v" + getPluginMeta().getVersion() + ") &7by Jason31416"))
                .addCommandNode("reload", ctx -> {
                    if(!ctx.getSender().sender().isOp()){
                        return Lang.getMessage("command.no-permission");
                    }
                    reload();
                    return Lang.getMessage("command.reload");
                })
                .addCommandNode("open", ctx -> {
                    if(ctx.getPlayer()==null) return null;
                    if(!GUITemplate.loadedTemplates.containsKey(ctx.getArg(0))){
                        return Lang.getMessage("command.gui-not-found").add("menu", ctx.getArg(0));
                    }
                    new GUISession(ctx.getPlayer()) {
                        @Override
                        public void setup(GUI gui) {}
                    }.display(GUITemplate.getGUI(ctx.getArg(0)));
                    return Lang.getMessage("command.gui-opened").add("menu", ctx.getArg(0));
                }, ctx -> {
                    String guiName = ctx.getArg(0);
                    return GUITemplate.loadedTemplates.keySet().stream().filter(name -> name.startsWith(guiName)).toList();
                })
                .addCommandNode("edit", ctx -> {
                    if(ctx.getPlayer()==null||!ctx.checkArgs(ParameterType.STRING)) return null;
                    if(!ctx.getSender().sender().isOp()){
                        return Lang.getMessage("command.no-permission");
                    }
                    if(!GUITemplate.loadedTemplates.containsKey(ctx.getArg(0))) return Lang.getMessage("command.gui-not-found").add("menu", ctx.getArg(0));

                    new GUIEditor(ctx.getPlayer(), ctx.getArg(0));

                    return Lang.getMessage("command.opened-edit").add("menu", ctx.getArg(0));
                }, ctx -> {
                    String guiName = ctx.getArg(0);
                    return GUITemplate.loadedTemplates.keySet().stream().filter(name -> name.startsWith(guiName)).toList();
                })
                .addCommandNode("list", ctx -> {
                    if(ctx.getPlayer()==null) return null;
                    if(!ctx.getPlayer().getPlayer().hasPermission("bettermenu.list")) return Lang.getMessage("command.no-permission");
                    new GUISession(ctx.getPlayer())
                            .display(new GUIBuilder("menu-list")
                                    .name(Lang.getMessage("gui.menu-list.name"))
                                    .shape( "q x x x x x x x x",
                                            "x a a a a a a a x",
                                            "x a a a a a a a x",
                                            "x a a a a a a a x",
                                            "x a a a a a a a x",
                                            "p x x x x x x x n")
                                    .setItem("q", GUIBuilder.StackedItem.builder().item(()->new SimpleItemStack()
                                            .setMaterial(Material.BARRIER)
                                            .setName(Lang.getMessage("gui.menu-list.quit"))
                                            ).runnable(inv->inv.getGui().close())
                                    )
                                    .setItem("a", GUIBuilder.ListedItem.builder().id("list").items(
                                            GUITemplate.loadedTemplates.keySet().stream()
                                            .map(name -> new InventoryList.ListItem(
                                                    ()->new SimpleItemStack()
                                                            .setMaterial(Material.PAINTING)
                                                            .setName(Message.of("&f"+name)),
                                                    List.of(inv->{
                                                        inv.getSession().display(GUITemplate.getGUI(name));
                                                    })
                                            )).toList())
                                    )
                                    .setItem("p", GUIBuilder.StackedItem.builder()
                                            .item(()->new SimpleItemStack()
                                                    .setMaterial(Material.GREEN_WOOL)
                                                    .setName(Lang.getMessage("gui.menu-list.previous-page"))
                                            ).runnable(inv -> {
                                                ((InventoryList) Objects.requireNonNull(inv.getGui().getItem("list"))).previousPage();
                                                inv.getGui().update();
                                            })
                                    )
                                    .setItem("n", GUIBuilder.StackedItem.builder()
                                            .item(()->new SimpleItemStack()
                                                    .setMaterial(Material.GREEN_WOOL)
                                                    .setName(Lang.getMessage("gui.menu-list.next-page"))
                                            ).runnable(inv -> {
                                                ((InventoryList) Objects.requireNonNull(inv.getGui().getItem("list"))).nextPage();
                                                inv.getGui().update();
                                            })
                                    )
                                    .setItem("x", GUIBuilder.StackedItem.builder()
                                            .item(()->new SimpleItemStack()
                                                    .setMaterial(Material.GRAY_STAINED_GLASS)
                                                    .setName("")
                                            )
                                    )
                                    .build());
                    return null;
                })
                .build();
    }

    @Override
    public void onDisable() {
        PluginLogger.info("Disabling BetterMenu...");
    }
}