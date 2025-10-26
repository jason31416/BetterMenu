package cn.jason31416.betterMenu;

import cn.jason31416.planetlib.gui.GUI;
import cn.jason31416.planetlib.gui.GUIRunnable;
import cn.jason31416.planetlib.gui.GUISession;
import cn.jason31416.planetlib.gui.GUITemplate;
import cn.jason31416.planetlib.gui.itemgroup.InventoryComponent;
import cn.jason31416.planetlib.gui.itemgroup.InventoryItem;
import cn.jason31416.planetlib.util.general.ShitMountainException;
import cn.jason31416.planetlib.wrapper.SimpleItemStack;
import cn.jason31416.planetlib.wrapper.SimplePlayer;

import java.util.List;

public class GUIEditor {
    public GUIRunnable createHandler(){
        return invocation->{
            switch (invocation.getAction()){
                case MOVE_TO_OTHER_INVENTORY -> {
                    invocation.getGui().removeItem(invocation.getSlot());
                }
                case DROP_ALL_CURSOR -> {
                    SimpleItemStack itemStack = new SimpleItemStack().setItemStack(invocation.getEvent().getCursor());
                    invocation.getGui().addItem(invocation.getEvent().getCursor().getType().name().toLowerCase()+"-"+invocation.getSlot(),
                            new InventoryItem(itemStack::copy, List.of(invocation.getSlot()), List.of(createHandler())));
                }
            }
        };
    }
    public static void editGUI(SimplePlayer player, String guiTemplate){
        if(!GUITemplate.loadedTemplates.containsKey(guiTemplate)) return;
        GUITemplate template = GUITemplate.getTemplate(guiTemplate);
        GUI gui = template.createGUI();
        for(InventoryComponent component : gui.getContent().values()){
            if(component instanceof InventoryItem item){
                item.clickable.clear();
                item.addClickHandler();
            }else{
                throw new ShitMountainException("Unsupported component type: " + component.getClass().getName())
            }
        }
        new GUISession(player).display(gui);
    }
}
