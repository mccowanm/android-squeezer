package uk.org.ngo.squeezer.model;

import android.util.Log;

import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;

public class CustomJiveItem extends JiveItem{

    private static final String TAG = "CustomJiveItem";

    static int id = 1;

    static List<JiveItem> list = new ArrayList<>();

    public static void addCustomShortcut(JiveItem item) {
//      TODO:  string resource problem: dynamically generate JiveItem but resource must be known?
        String str_id = "shortcut_" + String.valueOf(id);
        list.add(new JiveItem(str_id, "home", id,2000,Window.WindowStyle.HOME_MENU));
        list.get(id).goAction = item.goAction;
        list.get(id).setName(item.getName());
        id =+ 1;

    }

    static Map<String, Object> setupCustomNodeRecord(Map<String, Object> record, String name) {
        Object[] thirdCmdInItemAction = {"browselibrary", "items"};
        Object[] fourthCmdInBaseAction = {"browselibrary", "items"};

        Map<String, Object> thirdParamsInItemAction = new HashMap<>();
        Map<String, Object> secondItemAction = new HashMap<>();
        Map<String, Object> firstGoForItemActions = new HashMap<>();
        Map<String, Object> secondMoreForBaseActions = new HashMap<>();

        Map<String, Object> fourthIsContextWindowInBaseAction = new HashMap<>();
        Map<String, Object> fourthParamsInBaseAction = new HashMap<>();
        Map<String, Object> thirdActionInBaseAction = new HashMap<>();
        Map<String, Object> firstBaseActions = new HashMap<>();

//      baseAction
        fourthIsContextWindowInBaseAction.put("isContextMenu", "1");
        fourthParamsInBaseAction.put("mode", "bmf");
        fourthParamsInBaseAction.put("menu", "1");
        fourthParamsInBaseAction.put("useContextMenu", "1");
        thirdActionInBaseAction.put("itemsParams", "params");
        thirdActionInBaseAction.put("window", fourthIsContextWindowInBaseAction);
        thirdActionInBaseAction.put("cmd", fourthCmdInBaseAction);
        thirdActionInBaseAction.put("params", fourthParamsInBaseAction);
        thirdActionInBaseAction.put("player", "0");
        secondMoreForBaseActions.put("more", thirdActionInBaseAction);
        firstBaseActions.put("actions", secondMoreForBaseActions);

//      itemAction
        thirdParamsInItemAction.put("mode", "bmf");
        thirdParamsInItemAction.put("menu", "browselibrary"); // "1" for home menu
        thirdParamsInItemAction.put("useContextMenu", "1");
        thirdParamsInItemAction.put("item_id", "0.2"); // Kinder
        secondItemAction.put("cmd", thirdCmdInItemAction);
        secondItemAction.put("player", "0");
        secondItemAction.put("params", thirdParamsInItemAction);
        firstGoForItemActions.put("go", secondItemAction);

        record.put("urlPrefix", "ADD_PREFIX");
        record.put("actions", firstGoForItemActions);
        record.put("base", firstBaseActions);

        Log.d(TAG, "record: Custom node 'record': " + record.toString());
        return record;
    }



}
