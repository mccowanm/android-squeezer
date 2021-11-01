package uk.org.ngo.squeezer.service;

import android.util.Log;

import java.util.Random;
import java.util.Set;

import uk.org.ngo.squeezer.model.Player;

// Has to be instantiated once to have the mDelegate
public class RandomPlayDelegate {

    private static final String TAG = "RandomPlayDelegate";

    public RandomPlayDelegate(SlimDelegate mDelegate) {
        if (RandomPlayDelegate.delegate == null) {
            RandomPlayDelegate.delegate = mDelegate;
        }
    }

    static public SlimDelegate delegate; // Make it final if possible

    static public String pickTrack(Set<String> unplayed, String ignore) {
        Object[] stringArray = unplayed.toArray();
        String track = "";
        Random random = new Random();
        do {
            int r = random.nextInt(unplayed.size());
            try {
                track = (String) stringArray[r];
            } catch (Exception e) {
                Log.e(TAG, "Unable to get track from Array: " + e.getMessage());
            }
        } while (ignore.equals(track)); // ignore the last track in case of new cycle
        return track;
    }

    static public void fillPlaylist(Set<String> unplayed, Player player, String ignore) throws Exception {
        if (unplayed.size() > 0 ) {
            String next = RandomPlayDelegate.pickTrack(unplayed, ignore);
            delegate.command(player).cmd("playlistcontrol")
                    .param("cmd", "add").param("track_id", next).exec();

            // Get the next track and set it for this player's instance.
            // It will be loaded to be added to the played tracks when the next track begins
            // to play (the track info does not contain the ID, so we have to do this).
            delegate.setRandomPlayIsActive(player, next);

        } else {
            throw new Exception("Could not find next track to load it");
        }
    }
}