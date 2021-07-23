package uk.org.ngo.squeezer.service;

import android.util.Log;

import java.util.Random;
import java.util.Set;

import uk.org.ngo.squeezer.model.Player;

// Has to be instantiated once to have the mDelegate
public class RandomPlayDelegate {

    private static final String TAG = "RandomPlayStatic";

    public RandomPlayDelegate(SlimDelegate mDelegate) {
        if (RandomPlayDelegate.mDelegate == null) {
            RandomPlayDelegate.mDelegate = mDelegate;
        }
    }

    // Make it final if possible
    static public SlimDelegate mDelegate;

    static public String pickTrack(Set<String> unplayed) {
        Object[] stringArray = unplayed.toArray();
        String track = "";
        Random random = new Random();
        int r = random.nextInt(unplayed.size());
        try {
            track = (String) stringArray[r];       // track = "203200";
        } catch (Exception e) {
            Log.e(TAG, "Could not cast to String: " + e.getMessage());
        }
        return track;
    }

    // If no player was given, find active one (initial setup)
    static public void fillPlaylist(Set<String> unplayed) throws Exception {
        fillPlaylist(unplayed, RandomPlayDelegate.mDelegate.getActivePlayer());
    }

    // Use if player was specified (refill)
    static public void fillPlaylist(Set<String> unplayed, Player player) throws Exception {
        if (unplayed.size() > 0 ) {
            String next = RandomPlayDelegate.pickTrack(unplayed);
            SlimDelegate.Command command =
                    RandomPlayDelegate.mDelegate.command(player)
                            .cmd("playlistcontrol")
                            .param("cmd", "add")
                            .param("track_id", next);

            command.exec();

            // Get the next track and set it to the instance for the active player.
            // It will be loaded to be added to the played tracks when the next track begins
            // to play (the track info does not contain the ID, so we have to do this).
            RandomPlayDelegate.mDelegate.setRandomPlayIsActive(next);

        } else {
            throw new Exception("Could not find next track to load it");
        }
    }
}