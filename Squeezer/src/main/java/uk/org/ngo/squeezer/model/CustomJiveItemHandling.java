package uk.org.ngo.squeezer.model;


import uk.org.ngo.squeezer.itemlist.JiveItemListActivity;
import uk.org.ngo.squeezer.service.HomeMenuHandling;
import uk.org.ngo.squeezer.service.ISqueezeService;

public class CustomJiveItemHandling {

    private static final String TAG = "CustomJiveItem";

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





//    public static final JiveItem CUSTOM = new JiveItem("customNode", "home", R.string.CUSTOM_NODE, 1010, Window.WindowStyle.HOME_MENU);
//
//    JiveItem.private static Map<String, Object> record(String id, String node, @StringRes int text, int weight) {
//    ...
//            if (id.equals("customNode")) {
//        record = CustomJiveItemHandling.setupCustomNodeRecord(record,"customNode");
//    }
//
//
//    /**
//     * Setup for fixed commands
//     *
//     * @param record
//     * @param name
//     * @return
//     */
//    static Map<String, Object> setupCustomNodeRecord(Map<String, Object> record, String name) {
//        Object[] thirdCmdInItemAction = {"browselibrary", "items"};
//        Object[] fourthCmdInBaseAction = {"browselibrary", "items"};
//
//        Map<String, Object> thirdParamsInItemAction = new HashMap<>();
//        Map<String, Object> secondItemAction = new HashMap<>();
//        Map<String, Object> firstGoForItemActions = new HashMap<>();
//        Map<String, Object> secondMoreForBaseActions = new HashMap<>();
//
//        Map<String, Object> fourthIsContextWindowInBaseAction = new HashMap<>();
//        Map<String, Object> fourthParamsInBaseAction = new HashMap<>();
//        Map<String, Object> thirdActionInBaseAction = new HashMap<>();
//        Map<String, Object> firstBaseActions = new HashMap<>();
//
////      baseAction
//        fourthIsContextWindowInBaseAction.put("isContextMenu", "1");
//        fourthParamsInBaseAction.put("mode", "bmf");
//        fourthParamsInBaseAction.put("menu", "1");
//        fourthParamsInBaseAction.put("useContextMenu", "1");
//        thirdActionInBaseAction.put("itemsParams", "params");
//        thirdActionInBaseAction.put("window", fourthIsContextWindowInBaseAction);
//        thirdActionInBaseAction.put("cmd", fourthCmdInBaseAction);
//        thirdActionInBaseAction.put("params", fourthParamsInBaseAction);
//        thirdActionInBaseAction.put("player", "0");
//        secondMoreForBaseActions.put("more", thirdActionInBaseAction);
//        firstBaseActions.put("actions", secondMoreForBaseActions);
//
////      itemAction
//        thirdParamsInItemAction.put("mode", "bmf");
//        thirdParamsInItemAction.put("menu", "browselibrary"); // "1" for home menu
//        thirdParamsInItemAction.put("useContextMenu", "1");
//        thirdParamsInItemAction.put("item_id", "0.2"); // Kinder
//        secondItemAction.put("cmd", thirdCmdInItemAction);
//        secondItemAction.put("player", "0");
//        secondItemAction.put("params", thirdParamsInItemAction);
//        firstGoForItemActions.put("go", secondItemAction);
//
//        record.put("urlPrefix", "ADD_PREFIX");
//        record.put("actions", firstGoForItemActions);
//        record.put("base", firstBaseActions);
//
//        Log.d(TAG, "record: Custom node 'record': " + record.toString());
//        return record;
//    }



}

