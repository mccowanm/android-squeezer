package uk.org.ngo.squeezer.service;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.model.Action;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.model.MenuStatusMessage;
import uk.org.ngo.squeezer.model.Window;
import uk.org.ngo.squeezer.service.event.HomeMenuEvent;

public class HomeMenuHandling {


    private static final String TAG = "HomeMenuHandling";

    /** One less than actual max in JiveItem.java */
    private static final int MAX_CUSTOM_SHORTCUT_NODES = 4;

    /** Home menu tree as received from slimserver */
    public final List<JiveItem> homeMenu = new Vector<>();
    public CopyOnWriteArrayList<JiveItem> customShortcuts = new CopyOnWriteArrayList<>();

    public HomeMenuHandling(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
        initializeShortcutTemplates();
    }

    private final EventBus mEventBus;

    boolean isInArchive(JiveItem toggledItem) {
        return getParents(toggledItem.getNode()).contains(JiveItem.ARCHIVE) ? Boolean.TRUE : Boolean.FALSE;
    }

    void cleanupArchive(JiveItem toggledItem) {
        for (JiveItem archiveItem : homeMenu) {
            if (archiveItem.getNode().equals(JiveItem.ARCHIVE.getId())) {
                Set<JiveItem> parents = getOriginalParents(archiveItem.getOriginalNode());
                if ( parents.contains(toggledItem)) {
                    archiveItem.setNode(archiveItem.getOriginalNode());
                }
            }
        }
    }

    public void handleMenuStatusEvent(MenuStatusMessage event) {
        for (JiveItem serverItem : event.menuItems) {
            JiveItem item = null;
            for (JiveItem clientItem : homeMenu) {
                if (serverItem.getId().equals(clientItem.getId())) {
                    item = clientItem;
                    break;
                }
            }
            if (item != null) {
                homeMenu.remove(item);
                serverItem.setNode(item.getNode());  // for Archive
            }
            if (MenuStatusMessage.ADD.equals(event.menuDirective)) {
                homeMenu.add(serverItem);
            }
        }
        mEventBus.postSticky(new HomeMenuEvent(homeMenu));
    }

    public void triggerHomeMenuEvent() {
        mEventBus.postSticky(new HomeMenuEvent(homeMenu));
    }

    List<String> toggleArchiveItem(JiveItem toggledItem) {
        if (toggledItem.getNode().equals(JiveItem.ARCHIVE.getId())) {
            toggledItem.setNode(toggledItem.getOriginalNode());
            List<String> archivedItems = getArchivedItems();
            if (archivedItems.isEmpty()) {
                homeMenu.remove(JiveItem.ARCHIVE);
            }
            return archivedItems;
        }

        cleanupArchive(toggledItem);
        toggledItem.setNode(JiveItem.ARCHIVE.getId());
        if (!homeMenu.contains(JiveItem.ARCHIVE)) {
            homeMenu.add(JiveItem.ARCHIVE);
            mEventBus.postSticky(new HomeMenuEvent(homeMenu));
        }
        return getArchivedItems();
    }

    public Set<JiveItem> getOriginalParents(String node) {
        Set<JiveItem> parents = new HashSet<>();
        getParents(node, parents, JiveItem::getOriginalNode);
        return parents;
    }

    private Set<JiveItem> getParents(String node) {
        Set<JiveItem> parents = new HashSet<>();
        getParents(node, parents, JiveItem::getNode);
        return parents;
    }

    private void getParents(String node, Set<JiveItem> parents, Function<JiveItem, String> getParent) {
        if (node == null || node.equals(JiveItem.HOME.getId())) {          // if we are done
            return;
        }
        for (JiveItem menuItem : homeMenu) {
            if (menuItem.getId().equals(node)) {
                String parent = getParent.apply(menuItem);
                parents.add(menuItem);
                getParents(parent, parents, getParent);
            }
        }
    }

    public List<String> getArchivedItems() {
        List<String> archivedItems = new ArrayList<>();
        for (JiveItem item : homeMenu) {
            if (item.getNode().equals(JiveItem.ARCHIVE.getId())) {
                archivedItems.add(item.getId());
            }
        }
        return archivedItems;
    }

    private void addArchivedItems(List<String> archivedItems) {
        if (!(archivedItems.isEmpty()) && (!homeMenu.contains(JiveItem.ARCHIVE))) {
            homeMenu.add(JiveItem.ARCHIVE);
        }
        for (String s : archivedItems) {
            for (JiveItem item : homeMenu) {
                if  (item.getId().equals(s)) {
                    item.setNode(JiveItem.ARCHIVE.getId());
                }
            }
        }
    }

    public void setHomeMenu(List<String> archivedItems) {
        homeMenu.remove(JiveItem.ARCHIVE);
        homeMenu.stream().forEach(item -> item.setNode(item.getOriginalNode()));
        addArchivedItems(archivedItems);
        loadShortcutItems();
        mEventBus.postSticky(new HomeMenuEvent(homeMenu));
    }

    public void setHomeMenu(List<JiveItem> items, List<String> archivedItems) {
        jiveMainNodes(items);
        homeMenu.clear();
        homeMenu.addAll(items);
        addArchivedItems(archivedItems);
        loadShortcutItems();
        mEventBus.postSticky(new HomeMenuEvent(homeMenu));
    }

    private void jiveMainNodes(List<JiveItem> homeMenu) {
        addNode(JiveItem.EXTRAS, homeMenu);
        addNode(JiveItem.SETTINGS, homeMenu);
        addNode(JiveItem.ADVANCED_SETTINGS, homeMenu);
    }

    void addNode(JiveItem jiveItem, List<JiveItem> homeMenu) {
        if (!homeMenu.contains(jiveItem)) {
            jiveItem.setNode(jiveItem.getOriginalNode());
            homeMenu.add(jiveItem);
        }
    }

    /**
     * Add empty JiveItems to memory.
     */
    public void initializeShortcutTemplates() {
        customShortcuts.add(JiveItem.CUSTOM_SHORTCUT_1);
        customShortcuts.add(JiveItem.CUSTOM_SHORTCUT_2);
        customShortcuts.add(JiveItem.CUSTOM_SHORTCUT_3);
        customShortcuts.add(JiveItem.CUSTOM_SHORTCUT_4);
        customShortcuts.add(JiveItem.CUSTOM_SHORTCUT_5);
    }

    /**
     * Load complete list of stored items from preferences.
     * Use action the values on the initialized customNodes.
     */
    public void loadShortcutItems() {
        Map<String, String> map = new Preferences(Squeezer.getContext()).restoreCustomShortcuts();
        int index = -1;
        for (Map.Entry<String, String> pair : map.entrySet()) {
            index++;
            customShortcuts.get(index).setName(pair.getKey());
            if (!pair.getKey().contains("customShortcut")) {       // assign values to empty item
                JSONObject loadedObj;
                JSONObject loadedAction;
                JSONObject loadedJsonAction;
                JSONObject loadedParams;
                Object loadedCmd;
                Map<String, String> paramsMap = new HashMap<>();
                Action action = new Action();
                Action.JsonAction jsonAction = new Action.JsonAction();
                try {
                    loadedObj = new JSONObject(pair.getValue());
                    loadedAction = new JSONObject(loadedObj.getString("Action"));
                    if (loadedObj.has("nextWindow")) { // normally null
//                      TODO: Maybe store and reload nextWindow objects
                    }
                    loadedJsonAction = new JSONObject(loadedAction.getString("JsonAction"));
                    loadedCmd = loadedJsonAction.get("cmd");
                    if (loadedCmd instanceof JSONArray) {
                        toList((JSONArray) loadedCmd, jsonAction); // add commands to jsonAction
                    }
                    loadedParams = new JSONObject(loadedJsonAction.getString("params"));
                    Iterator<String> keys = loadedParams.keys();
                    while (keys.hasNext()) {
                        String key = keys.next();
                        Object value = loadedParams.get(key);
                        paramsMap.put(key, value.toString());  // isContextMenu is Integer!
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                jsonAction.nextWindow = null;
                jsonAction.params.putAll(paramsMap);
                action.urlCommand = null;       // TODO: Maybe store and reload this
                action.action = jsonAction;

                customShortcuts.get(index).goAction = action;
                customShortcuts.get(index).setNode("home");
            } else {
                customShortcuts.get(index).setNode("none");  // empty item invisible on homeMenu
            }
        }
        for (JiveItem item : customShortcuts) {
            homeMenu.remove((item));
        }
        homeMenu.addAll(customShortcuts);
    }

    /**
     * Extract commands from array and add them to the shortcut item
     * @param obj
     * @param jsonAction
     * @throws JSONException
     */
    private void toList(JSONArray obj, Action.JsonAction jsonAction) throws JSONException {
        for (int i = 0; i < obj.length(); i++) {
            String value = (String) obj.get(i);
            jsonAction.cmd.add(value);
        }
    }

    /**
     * Add prepared jive item to home menu
     * @param jiveItem
     */
    public void addShortcutToHomeMenu(JiveItem jiveItem) {
        homeMenu.remove(jiveItem);
        jiveItem.setNode("home");
        homeMenu.add(jiveItem);
    }

    public void resetShortcutNodeOnHomeMenu(JiveItem oldItem, JiveItem newItem) {
        homeMenu.remove(oldItem);
        homeMenu.add(newItem);
    }

    /**
     * Get the triggered slim item
     * @param itemToShortcut
     */
    public boolean triggerCustomShortcut(JiveItem itemToShortcut) {
        return addShortcutNodeToCustomNodes(itemToShortcut);
    }

    /**
     * Add jive item to the home screen and save the whole list to preferences
     * @param item
     */
    boolean addShortcutNodeToCustomNodes(JiveItem item) {
        if (!shortcutAlreadyAdded(item)) {
            JiveItem template = getShortcutTemplate();
            template.goAction = item.goAction;
            template.setName(item.getName());
//            TODO: add Icon to shortcut
            addShortcutToHomeMenu(template);
            new Preferences(Squeezer.getContext()).saveCustomShortcuts(convertCustomShortcutItems());
        } else {
            return false;
        }
        return true; // item was put into shortcuts
    }

    /**
     * Get the first empty template to fill it as a shortcut or return the first real shortcut
     * to overwrite it, if no space is left (and move items in list up when doing so).
     * @return
     */
    private JiveItem getShortcutTemplate() {
        for (JiveItem item : customShortcuts) {
            if (item.getName().contains("customShortcut")) {
                return item;     // found an empty slot
            }
        }
        reorderCustomShortcutItems();
        return customShortcuts.get(MAX_CUSTOM_SHORTCUT_NODES);
    }

    /**
     * Returns true if the shortcut is already in memory (and therefore should be displayed).
     * @param itemToShortcut
     * @return
     */
    private boolean shortcutAlreadyAdded(JiveItem itemToShortcut) {
        for (JiveItem item : customShortcuts) {
            if (item.getName().equals(itemToShortcut.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reorder memory. Put the added item last in the list of active shortcuts.
     * Used if the first item was empty due to a remove before.
     **/
    public void reorderCustomShortcutItems() {
        List<JiveItem> reorder = new ArrayList<>();
        for (int i = 1; i <= MAX_CUSTOM_SHORTCUT_NODES; i++) {
            reorder.add(customShortcuts.get(i));
        }
        reorder.add(customShortcuts.get(0));
        customShortcuts.clear();
        customShortcuts.addAll(reorder);
    }

    /**
     * Get list of current custom shortcut items from memory and deliver map of name/goAction.
     * If item is a template with no action, make the name + "_index" and action "null".
     * @return
     */
    public LinkedHashMap<String,String> convertCustomShortcutItems() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        int index = 0;
        for (JiveItem item : customShortcuts) {
            if (item.goAction != null) {
                map.put(item.getName(), item.goAction.toJSONString());
            } else {
                index = index + 1;
                map.put("customShortcut_" + index, "null");
            }
        }
        return map;
    }

    /**
     * Remove custom shortcut item from memory and save whole list to preferences, move emptied slot
     * to end of list and remove custom shortcut item from homeMenu.
     * @param item
     */
    public void removeCustomShortcut(JiveItem item) {
        JiveItem newItem = new JiveItem(item.getId(), "none", R.string.CUSTOM_SHORTCUT_NODE, 1010, Window.WindowStyle.HOME_MENU);
        customShortcuts.remove(item);
        customShortcuts.add(newItem);
        new Preferences(Squeezer.getContext()).saveCustomShortcuts(convertCustomShortcutItems());
        resetShortcutNodeOnHomeMenu(item, newItem);
    }

    public void removeAllShortcuts() {
        for (JiveItem item : customShortcuts) {
            JiveItem newItem = new JiveItem(item.getId(), "none", R.string.CUSTOM_SHORTCUT_NODE, 1010, Window.WindowStyle.HOME_MENU);
            customShortcuts.remove(item);
            customShortcuts.add(newItem);
            resetShortcutNodeOnHomeMenu(item, newItem);
        }
        new Preferences(Squeezer.getContext()).saveCustomShortcuts(convertCustomShortcutItems());
    }
}
