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
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.model.Action;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.model.MenuStatusMessage;
import uk.org.ngo.squeezer.model.Window;
import uk.org.ngo.squeezer.service.event.HomeMenuEvent;

public class HomeMenuHandling {


    private static final String TAG = "HomeMenuHandling";

    /**
     * Home menu tree as received from slimserver
     */
    public final List<JiveItem> homeMenu = new Vector<>();
    public CopyOnWriteArrayList<JiveItem> customShortcuts = new CopyOnWriteArrayList<>();

    public HomeMenuHandling(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
    }

    private final EventBus mEventBus;

    boolean isInArchive(JiveItem toggledItem) {
        return getParents(toggledItem.getNode()).contains(JiveItem.ARCHIVE) ? Boolean.TRUE : Boolean.FALSE;
    }

    void cleanupArchive(JiveItem toggledItem) {
        for (JiveItem archiveItem : homeMenu) {
            if (archiveItem.getNode().equals(JiveItem.ARCHIVE.getId())) {
                Set<JiveItem> parents = getOriginalParents(archiveItem.getOriginalNode());
                if (parents.contains(toggledItem)) {
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
                if (item.getId().equals(s)) {
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
     * Load complete list of stored items from preferences.
     * Use action the values on the initialized customNodes.
     */
    public void loadShortcutItems() {
        Map<String, String> map = new Preferences(Squeezer.getContext()).restoreCustomShortcuts();
        customShortcuts.clear();
        for (Map.Entry<String, String> pair : map.entrySet()) {
            JiveItem template = new JiveItem("customShortcut_" + customShortcuts.size(), "home", "customShortcut", 1010, Window.WindowStyle.HOME_MENU);

            template.setName(pair.getKey());
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

            template.goAction = action;
            customShortcuts.add(template);
        }
        homeMenu.addAll(customShortcuts);
    }

    private void toList(JSONArray obj, Action.JsonAction jsonAction) throws JSONException {
        for (int i = 0; i < obj.length(); i++) {
            String value = (String) obj.get(i);
            jsonAction.cmd.add(value);
        }
    }

    public boolean triggerCustomShortcut(JiveItem itemToShortcut) {
        return addShortcutNodeToCustomNodes(itemToShortcut);
    }

    boolean addShortcutNodeToCustomNodes(JiveItem item) {
        if (!shortcutAlreadyAdded(item)) {
            JiveItem template = new JiveItem("customShortcut_" + customShortcuts.size(), "home", item.getName(), 1010, Window.WindowStyle.HOME_MENU);
            template.goAction = item.goAction;
//            TODO: add Icon to shortcut
            customShortcuts.add(template);
            homeMenu.add(template);
            new Preferences(Squeezer.getContext()).saveCustomShortcuts(convertCustomShortcutItems());
        } else {
            return false;
        }
        return true;
    }

    private boolean shortcutAlreadyAdded(JiveItem itemToShortcut) {
        for (JiveItem item : customShortcuts) {
            if (item.getName().equals(itemToShortcut.getName())) {
                return true;
            }
        }
        return false;
    }

    public LinkedHashMap<String, String> convertCustomShortcutItems() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        for (JiveItem item : customShortcuts) {
            if (item.goAction != null) {
                map.put(item.getName(), item.goAction.toJSONString());
            }
        }
        return map;
    }


    public void removeCustomShortcut(JiveItem item) {
        customShortcuts.remove(item);
        new Preferences(Squeezer.getContext()).saveCustomShortcuts(convertCustomShortcutItems());
        homeMenu.remove(item);
    }

    public void removeAllShortcuts() {
        for (JiveItem item : customShortcuts) {
            customShortcuts.remove(item);
            homeMenu.remove(item);
        }
        new Preferences(Squeezer.getContext()).saveCustomShortcuts(convertCustomShortcutItems());
    }
}
