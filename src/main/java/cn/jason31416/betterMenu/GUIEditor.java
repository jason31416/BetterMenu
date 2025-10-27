package cn.jason31416.betterMenu;

import cn.jason31416.planetlib.PlanetLib;
import cn.jason31416.planetlib.gui.GUI;
import cn.jason31416.planetlib.gui.GUIBuilder;
import cn.jason31416.planetlib.gui.GUISession;
import cn.jason31416.planetlib.gui.GUITemplate;
import cn.jason31416.planetlib.gui.itemgroup.InventoryComponent;
import cn.jason31416.planetlib.gui.itemgroup.InventoryItem;
import cn.jason31416.planetlib.message.Message;
import cn.jason31416.planetlib.message.MessageList;
import cn.jason31416.planetlib.util.Lang;
import cn.jason31416.planetlib.util.PluginLogger;
import cn.jason31416.planetlib.util.general.Pair;
import cn.jason31416.planetlib.util.general.ShitMountainException;
import cn.jason31416.planetlib.wrapper.SimpleItemStack;
import cn.jason31416.planetlib.wrapper.SimplePlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GUIEditor {
    private final Map<Integer, Pair<Pair<String, String>, InventoryItem> > baseInventory=new HashMap<>();
    private final Map<Integer, Pair<String, InventoryItem> > selfInventory=new HashMap<>();
    private GUI gui;
    private String guiId;
    private GUISession getSession(){return gui.getSession();}

    public GUIEditor(SimplePlayer player, String guiTemplate){
        if(!GUITemplate.loadedTemplates.containsKey(guiTemplate)) return;
        GUITemplate template = GUITemplate.getTemplate(guiTemplate);
        gui = new GUI(template.id, template.size, template.name);
        guiId = template.id;
        gui.refresh(template.refreshInterval);
        GUITemplate curBase = template;
        Set<String> usedBases = new HashSet<>();
        Stack<GUITemplate> stack = new Stack<>();

        while(true) {
            if (curBase.base != null && !GUITemplate.loadedTemplates.containsKey(curBase.base)) {
                PluginLogger.error("GUI template not found: " + curBase);
                break;
            }

            if (curBase.base == null) {
                break;
            }

            usedBases.add(curBase.base);
            curBase = GUITemplate.loadedTemplates.get(curBase.base);
            if (curBase.size != template.size) {
                String v = template.name.toString();
                throw new IllegalArgumentException("GUI template size for " + v + " mismatch with base: " + curBase);
            }

            stack.push(curBase);

            if (usedBases.contains(curBase.base)) {
                PluginLogger.error("Circular reference found in GUI template: " + curBase);
                break;
            }
        }

        Set<Integer> slots = new HashSet<>();
        for(int i=0;i<template.size;i++){
            slots.add(i);
        }

        while(!stack.empty()) {
            curBase = stack.pop();
            var cb = curBase;

            for(String key : curBase.inventory.keySet()) {
                InventoryItem component = curBase.inventory.get(key).copy();
                applyEmptyClickable(component);
                gui.addItem(key, component.copy().addItemModifier(item -> (item.lore==null?item.setLore(new MessageList(new ArrayList<>())):item).lore.getContent().addAll(Lang.getMessageList("gui.editor.base-item-lore").add("base-name", cb.id).asList())));
                for(int slot: component.getSlots()){
                    slots.remove(slot);
                    baseInventory.put(slot, Pair.of(Pair.of(cb.id, key), component));
                }
            }
        }
        for(String key : template.inventory.keySet()) {
            InventoryItem component = template.inventory.get(key).copy();
            component.addItemModifier(item -> (item.lore==null?item.setLore(new MessageList(new ArrayList<>())):item).lore.getContent().addAll(Lang.getMessageList("gui.editor.self-item-lore").asList()));
            applyClickable(component);
            gui.addItem(key, component);
            for(int slot: component.getSlots()){
                slots.remove(slot);
                selfInventory.put(slot, Pair.of(key, component));
            }
        }

        for(int slot: slots){
            addEmptyItem(slot);
        }

        new GUISession(player).display(gui);
    }

    public void openItemEditor(String itemId, InventoryItem item){
        getSession().display(new GUIBuilder("itemeditor-"+itemId+"-"+UUID.randomUUID())
                        .name(Lang.getMessage("gui.item-editor.name").add("menu", guiId).add("item", itemId))
                        .shape( "x x x x i x x x x",
                                "x n l - - - - - x",
                                "x x x x x x x x x")
                        .setItem("x", new SimpleItemStack().setMaterial(Material.GRAY_STAINED_GLASS_PANE).setName(" "))
                        .setItem("n", new GUIBuilder.StackedItem()
                                .item(()->new SimpleItemStack().setMaterial(Material.NAME_TAG).setName(Lang.getMessage("gui.item-editor.item.name.name")).setLore(Lang.getMessageList("gui.item-editor.item.name.lore").add("current", "")))
                                .id("name-input")
                                //todo: runnable
                        )
                .setItem("l", new GUIBuilder.StackedItem()
                                .item(()->new SimpleItemStack().setMaterial(Material.PAPER).setName(Lang.getMessage("gui.item-editor.item.lore.name")).setLore(Lang.getMessageList("gui.item-editor.item.lore.lore")))
                                .id("lore-input")
                        //todo: runnable
                )
                .build());
    }


    public void applyClickable(InventoryItem item){
        item.clickable.clear();
        item.addClickHandler(invocation -> {
            invocation.getPlayer().sendMessage(Message.of("You've clicked item with "+invocation.getAction()));
            switch (invocation.getAction()) {
                case PICKUP_ALL -> {
                    int slot = invocation.getSlot();
                    if(selfInventory.containsKey(slot)) {
                        ItemStack citem = selfInventory.get(slot).second().stack.get().toBukkitItem();
                        invocation.getEvent().setCursor(citem);
                        removeItem(invocation.getSlot());
                        gui.update();
                    }
                }
                case PICKUP_HALF -> {
                    if(selfInventory.containsKey(invocation.getSlot())){
                        var t = selfInventory.get(invocation.getSlot());
                        openItemEditor(t.first(), t.second());
                    }
                }
                case SWAP_WITH_CURSOR -> {
                    SimpleItemStack cursor = new SimpleItemStack().setItemStack(invocation.getEvent().getCursor());
                    invocation.getEvent().setCursor(null);
                    InventoryItem itm = new InventoryItem(cursor::copy, new ArrayList<>(List.of(invocation.getSlot())), new ArrayList<>());
                    applyClickable(itm);
                    switchItem(invocation.getSlot(), gui.getId()+"-"+cursor.material.toString().toLowerCase(Locale.ROOT)+"-"+invocation.getSlot(), itm);
                    gui.update();
                }
            }
        });
    }

    public void addEmptyItem(int slot){
        InventoryItem item = new InventoryItem(()->null, new ArrayList<>(List.of(slot)), new ArrayList<>());
        gui.addItem("_empty-"+UUID.randomUUID(), item);
        applyEmptyClickable(item);
    }

    public void applyEmptyClickable(InventoryItem item){
        item.clickable.clear();
        item.addClickHandler(invocation -> {
            invocation.getPlayer().sendMessage(Message.of("You've clicked empty slot "+invocation.getAction()));
            switch (invocation.getAction()) {
                case PLACE_ALL, SWAP_WITH_CURSOR -> {
                    SimpleItemStack cursor = new SimpleItemStack().setItemStack(invocation.getEvent().getCursor());
                    invocation.getEvent().setCursor(null);
                    InventoryItem itm = new InventoryItem(cursor::copy, new ArrayList<>(List.of(invocation.getSlot())), new ArrayList<>());
                    applyClickable(itm);
                    addItem(gui.getId()+"-"+cursor.material.toString().toLowerCase(Locale.ROOT)+"-"+invocation.getSlot(), itm);
                    gui.update();
                }
            }
        });
    }

    public void switchItem(int slot, String key, InventoryItem component){
        if(selfInventory.containsKey(slot)) {
            selfInventory.remove(slot);
            selfInventory.put(slot, new Pair<>(key, component));
            gui.addItem(key, component.copy().addItemModifier(x -> (x.lore==null?x.setLore(new MessageList(new ArrayList<>())):x).lore.getContent().addAll(Lang.getMessageList("gui.editor.self-item-lore").asList())));
        }
    }

    public void removeItem(int slot){
        if(selfInventory.containsKey(slot)){
            selfInventory.remove(slot);
            gui.removeItem(slot);
            if(baseInventory.containsKey(slot)){
                var pair = baseInventory.get(slot);
                var itm = gui.getItem(pair.first().second());
                if(itm==null){
                    gui.addItem(pair.first().second(), pair.second().copy().addItemModifier(item -> (item.lore==null?item.setLore(new MessageList(new ArrayList<>())):item).lore.getContent().addAll(Lang.getMessageList("gui.editor.base-item-lore").add("base-name", pair.first().first()).asList())));
                }else{
                    itm.getSlots().add(slot);
                }
            }else{
                addEmptyItem(slot);
            }
        }
    }

    public void addItem(String key, InventoryItem component) {
        gui.addItem(key, component.copy().addItemModifier(x -> (x.lore==null?x.setLore(new MessageList(new ArrayList<>())):x).lore.getContent().addAll(Lang.getMessageList("gui.editor.self-item-lore").asList())));
        for (int slot : component.getSlots()) {
            selfInventory.put(slot, new Pair<>(key, component));
        }
    }
}
