package uk.org.ngo.squeezer.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.model.MenuStatusMessage;

public class HomeMenuHandling {

    /** Home menu tree as received from slimserver */
    public final List<JiveItem> homeMenu = new Vector<>();

    public List<String> mArchivedItems = new ArrayList<>();

    public List<JiveItem> setHomeMenu(List<JiveItem> items) {
        homeMenu.clear();
        homeMenu.addAll(items);
        return homeMenu; // return it to ConnectionState
    }

    boolean removeArchiveNodeWhenEmpty() {
        for (JiveItem menuItem : homeMenu) {
//            TODO: handle UnDo better (now it is not displayed because screen changes)
            if (menuItem.getNode().equals(JiveItem.ARCHIVE.getId())) {
                return false;
            }
        }
        homeMenu.remove(JiveItem.ARCHIVE);
        return true;  // is empty
    }

    boolean checkIfItemIsAlreadyInArchive(JiveItem toggledItem) {
        if (getParents(toggledItem.getNode()).contains(JiveItem.ARCHIVE)) {
//            TODO: Message to the user
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

    public void handleMenuStatusEvent(MenuStatusMessage event) {
        for (JiveItem menuItem : event.menuItems) {
            JiveItem item = null;
            for (JiveItem menu : homeMenu) {
                if (menuItem.getId().equals(menu.getId())) {
                    item = menu;
                    break;
                }
            }
            if (item != null) {
                homeMenu.remove(item);
            }
            if (MenuStatusMessage.ADD.equals(event.menuDirective)) {
                homeMenu.add(menuItem);
            }
        }
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

}
