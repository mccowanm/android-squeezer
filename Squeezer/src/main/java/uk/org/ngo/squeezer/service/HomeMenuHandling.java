package uk.org.ngo.squeezer.service;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Preferences;
import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.model.DisplayMessage;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.model.MenuStatusMessage;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.event.DisplayEvent;
import uk.org.ngo.squeezer.service.event.HomeMenuEvent;

public class HomeMenuHandling {

    private static final String TAG = "HomeMenuHandling";

    /** Home menu tree as received from slimserver */
    public final List<JiveItem> homeMenu = new Vector<>();

    public HomeMenuHandling(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
    }

    private final EventBus mEventBus;

    private List<JiveItem> resetHomeMenu(List<JiveItem> items) {
        homeMenu.clear();
        homeMenu.addAll(items);
        return homeMenu;
    }

    void showDisplayMessage(String text) {
        Map<String, Object> display = new HashMap<>();
        Object[] objects = new Object[1];
        objects[0] = text;
        display.put("text", objects);
        display.put("type", (Object) "text");
        display.put("style", (Object) "style");  // TODO: What is the proper object for style?
        DisplayMessage displayMessage = new DisplayMessage(display);
        mEventBus.post(new DisplayEvent(displayMessage));
    }

    boolean checkIfItemIsAlreadyInArchive(JiveItem toggledItem) {
        if (getParents(toggledItem.getNode()).contains(JiveItem.ARCHIVE)) {
            showDisplayMessage(Squeezer.getContext().getResources().getString(R.string.MENU_IS_SUBMENU_IN_ARCHIVE));
            return Boolean.TRUE;
        }
        else {
            return Boolean.FALSE;
        }
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

    public void handleMenuStatusEvent(MenuStatusMessage event, Player activePlayer) {
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
        Log.d(TAG, "handleMenuStatusEvent: save archived items to preferences after server event");
        new Preferences(Squeezer.getContext()).setArchivedMenuItems(getArchivedItems(), activePlayer);
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

    private void getParents(String node, Set<JiveItem> parents, HomeMenuHandling.GetParent getParent) {
        if (node.equals(JiveItem.HOME.getId())) {          // if we are done
            return;
        }
        for (JiveItem menuItem : homeMenu) {
            if (menuItem.getId().equals(node)) {
                String parent = getParent.getNode(menuItem);
                parents.add(menuItem);
                getParents(parent, parents, getParent);
            }
        }
    }

    private interface GetParent {
        String getNode(JiveItem item);
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

    public void setHomeMenuWithEvent(List<JiveItem> homeMenu, List<String> archivedItems) {
        mEventBus.postSticky(new HomeMenuEvent(setHomeMenu(homeMenu, archivedItems)));
    }

    public List<JiveItem> setHomeMenu(List<JiveItem> homeMenu, List<String> archivedItems) {
        jiveMainNodes(homeMenu);
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
        return resetHomeMenu(homeMenu);
    }

    private void jiveMainNodes(List<JiveItem> homeMenu) {
        addNode(JiveItem.EXTRAS, homeMenu);
        addNode(JiveItem.SETTINGS, homeMenu);
        addNode(JiveItem.ADVANCED_SETTINGS, homeMenu);
        addNode(JiveItem.CUSTOM,homeMenu);
    }

    private void addNode(JiveItem jiveItem, List<JiveItem> homeMenu) {
        if (!homeMenu.contains(jiveItem)) {
            jiveItem.setNode(jiveItem.getOriginalNode());
            homeMenu.add(jiveItem);
        }
    }
}
