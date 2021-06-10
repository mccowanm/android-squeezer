package uk.org.ngo.squeezer.model;


import uk.org.ngo.squeezer.itemlist.JiveItemListActivity;
import uk.org.ngo.squeezer.service.HomeMenuHandling;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class CustomJiveItemHandling {

    final ISqueezeService service;
    final HomeMenuHandling mHomeMenuHandling;
    final JiveItemListActivity mActivity;

    public CustomJiveItemHandling(JiveItemListActivity activity) {
        mActivity = activity;
        service = mActivity.getService();
        mHomeMenuHandling = service.getDelegate().getHomeMenuHandling();
    }

    /**
     * Send long pressed JiveItem
     *
     * @param itemToShortcut
     */
    public boolean triggerCustomShortcut(JiveItem itemToShortcut) {
        return mHomeMenuHandling.triggerCustomShortcut(itemToShortcut);
    }

    public boolean isCustomShortcut(JiveItem item) {
        return mHomeMenuHandling.customShortcuts.contains(item);
    }

    public boolean isShortcutable(JiveItem item) {
        //  TODO add better check for fitting items
        //  TODO "All titles" is a name that comes up in several occations and will then not be updated
        if ((item.nextWindow != null) || (item.goAction == null)) {
            return false;
        }
        for (String s : item.goAction.action.cmd) {
            if (s.equals("browselibrary")) {
                return true;
            }
        }
    return false;
    }
}

