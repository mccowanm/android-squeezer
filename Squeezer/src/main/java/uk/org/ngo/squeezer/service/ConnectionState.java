/*
 * Copyright (c) 2009 Google Inc.  All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.ngo.squeezer.service;

import android.util.Log;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import de.greenrobot.event.EventBus;
import uk.org.ngo.squeezer.Util;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.model.MenuStatusMessage;
import uk.org.ngo.squeezer.model.Player;
import uk.org.ngo.squeezer.service.event.ActivePlayerChanged;
import uk.org.ngo.squeezer.service.event.ConnectionChanged;
import uk.org.ngo.squeezer.service.event.HandshakeComplete;
import uk.org.ngo.squeezer.service.event.HomeMenuEvent;
import uk.org.ngo.squeezer.service.event.PlayersChanged;

public class ConnectionState {

    private static final String TAG = "ConnectionState";

    ConnectionState(@NonNull EventBus eventBus) {
        mEventBus = eventBus;
    }

    private final EventBus mEventBus;

    public final static String MEDIA_DIRS = "mediadirs";

    // Connection state machine
    @IntDef({DISCONNECTED, CONNECTION_STARTED, CONNECTION_FAILED, CONNECTION_COMPLETED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ConnectionStates {}
    /** Ordinarily disconnected from the server. */
    public static final int DISCONNECTED = 0;
    /** A connection has been started. */
    public static final int CONNECTION_STARTED = 1;
    /** The connection to the server did not complete. */
    public static final int CONNECTION_FAILED = 2;
    /** The connection to the server completed, the handshake can start. */
    public static final int CONNECTION_COMPLETED = 3;

    @ConnectionStates
    private volatile int mConnectionState = DISCONNECTED;

    /** Map Player IDs to the {@link uk.org.ngo.squeezer.model.Player} with that ID. */
    private final Map<String, Player> mPlayers = new ConcurrentHashMap<>();

    /** The active player (the player to which commands are sent by default). */
    private final AtomicReference<Player> mActivePlayer = new AtomicReference<>();

    /** Home menu tree as received from slimserver */
    private final List<JiveItem> homeMenu = new Vector<>();

    private final AtomicReference<String> serverVersion = new AtomicReference<>();

    private final AtomicReference<String[]> mediaDirs = new AtomicReference<>();

    /**
     * Sets a new connection state, and posts a sticky
     * {@link uk.org.ngo.squeezer.service.event.ConnectionChanged} event with the new state.
     *
     * @param connectionState The new connection state.
     */
    void setConnectionState(@ConnectionStates int connectionState) {
        Log.i(TAG, "setConnectionState(" + mConnectionState + " => " + connectionState + ")");
        updateConnectionState(connectionState);
        mEventBus.postSticky(new ConnectionChanged(connectionState));
    }

    void setConnectionError(ConnectionError connectionError) {
        Log.i(TAG, "setConnectionError(" + mConnectionState + " => " + connectionError.name() + ")");
        updateConnectionState(CONNECTION_FAILED);
        mEventBus.postSticky(new ConnectionChanged(connectionError));
    }

    private void updateConnectionState(@ConnectionStates int connectionState) {
        // Clear data if we were previously connected
        if (isConnected() && !isConnected(connectionState)) {
            mEventBus.removeAllStickyEvents();
            setServerVersion(null);
            mPlayers.clear();
            setActivePlayer(null);
        }
        mConnectionState = connectionState;
    }

    public void setPlayers(Map<String, Player> players) {
        mPlayers.clear();
        mPlayers.putAll(players);
        mEventBus.postSticky(new PlayersChanged(players));

    }

    Player getPlayer(String playerId) {
        return mPlayers.get(playerId);
    }

    public Map<String, Player> getPlayers() {
        return mPlayers;
    }

    public Player getActivePlayer() {
        return mActivePlayer.get();
    }

    void setActivePlayer(Player player) {
        mActivePlayer.set(player);
        mEventBus.post(new ActivePlayerChanged(player));
    }

    void setServerVersion(String version) {
        if (Util.atomicReferenceUpdated(serverVersion, version)) {
            if (version != null && mConnectionState == CONNECTION_COMPLETED) {
                HandshakeComplete event = new HandshakeComplete(getServerVersion());
                Log.i(TAG, "Handshake complete: " + event);
                mEventBus.postSticky(event);
            }
        }
    }

    void setMediaDirs(String[] mediaDirs) {
        this.mediaDirs.set(mediaDirs);
    }

    void setHomeMenu(List<JiveItem> items) {
        homeMenu.clear();
        homeMenu.addAll(items);
        mEventBus.postSticky(new HomeMenuEvent(homeMenu));
    }

//    For menu updates sent from LMS
//    TODO: Handle node that is in Archive and gets an update from LMS here
    void menuStatusEvent(MenuStatusMessage event) {
        if (event.playerId.equals(getActivePlayer().getId())) {
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
            mEventBus.postSticky(new HomeMenuEvent(homeMenu));
        }
    }

//  Finds the last grandparent of a (grandchild) - but does not work
    public String getRootOfArchived(String node) {
        String rootID = "";
        String parent = "";
        Log.d(TAG, "getRootOfArchived: BEN node: " + node);
        for (JiveItem menuItem : homeMenu) {
            if (menuItem.getId().equals(node)) {
                if ((menuItem.getNode()).equals(JiveItem.HOME.getId())) {
                    Log.d(TAG, "getRootOfArchived: BEN parent of this parent is home");
                    rootID = menuItem.getId();
                    Log.d(TAG, "getRootOfArchived: BEN - will now return rootID: " + rootID);
                    return rootID;
                }

                else {
                    Log.d(TAG, "getRootOfArchived: BEN in else");
                    parent = menuItem.getNode();
                    getRootOfArchived(parent);
                }
                Log.d(TAG, "getRootOfArchived: BEN Why is this happening?");
            }
            Log.d(TAG, "getRootOfArchived: BEN and this?");
        }
        Log.d(TAG, "getRootOfArchived: BEN rootID is " + rootID);
        return rootID;
    }

    void toggleArchiveItem(JiveItem item) {

//      this should find the last grandparent, but the return statement from the method does not work
//      as expected
        String bennode = getRootOfArchived(item.getOldNodeWhenArchived());
        Log.d(TAG, "toggleArchiveItem: BEN bennode final grandparent is : " + bennode.toString());

//      get current items in archive
        for (JiveItem menuItem : homeMenu) {
            if (menuItem.getNode().equals(JiveItem.ARCHIVE.getId())) {
                Log.d(TAG, "toggleArchiveItem: BEN old archive was: " + menuItem.toString());
//                if new item ID == old item parent ID
                if (item.getId().equals(menuItem.getOldNodeWhenArchived())) {
//                  If the newly archived item is the parent of an already archived item put the
//                  older item back into its old parent
                    menuItem.setNode(menuItem.getOldNodeWhenArchived());
                    Log.d(TAG, "toggleArchiveItem: BEN old item moved from root into parent (in archive)");
                }
            }
        }

//        put item back
        if (item.getNode().equals(JiveItem.ARCHIVE.getId())) {
            Log.d(TAG, "toggleArchiveItem: BEN put it back wrong");
            item.setNode(item.getOldNodeWhenArchived());
        }
        else {
//            put item into archive (if it is not the archive itself)
            if (item.getId() != JiveItem.ARCHIVE.getId()) {
                item.setOldNodeWhenArchived(item.getNode());
                item.setNode(JiveItem.ARCHIVE.getId());
            }
        }
        if (!homeMenu.contains(JiveItem.ARCHIVE)) {
            homeMenu.add(JiveItem.ARCHIVE);
            mEventBus.postSticky(new HomeMenuEvent(homeMenu));
        }
    }

    String getServerVersion() {
        return serverVersion.get();
    }

    String[] getMediaDirs() {
        return mediaDirs.get();
    }

    /**
     * @return True if the socket connection to the server has completed.
     */
    boolean isConnected() {
        return isConnected(mConnectionState);
    }

    /**
     * @return True if the socket connection to the server has completed.
     */
    static boolean isConnected(@ConnectionStates int connectionState) {
        return connectionState == CONNECTION_COMPLETED;
    }

    /**
     * @return True if the socket connection to the server has started, but not yet
     *     completed (successfully or unsuccessfully).
     */
    boolean isConnectInProgress() {
        return isConnectInProgress(mConnectionState);
    }

    /**
     * @return True if the socket connection to the server has started, but not yet
     *     completed (successfully or unsuccessfully).
     */
    static boolean isConnectInProgress(@ConnectionStates int connectionState) {
        return connectionState == CONNECTION_STARTED;
    }

    @Override
    public String toString() {
        return "ConnectionState{" +
                "mConnectionState=" + mConnectionState +
                ", serverVersion=" + serverVersion +
                '}';
    }
}
