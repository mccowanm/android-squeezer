package uk.org.ngo.squeezer.itemlist;

import android.view.View;

import uk.org.ngo.squeezer.R;
import uk.org.ngo.squeezer.Squeezer;
import uk.org.ngo.squeezer.framework.ItemAdapter;
import uk.org.ngo.squeezer.model.JiveItem;
import uk.org.ngo.squeezer.widget.UndoBarController;


/*
 Class for the long click listener that puts menu items into the Archive node and provides an UndoBar.
 */

public class HomeMenuJiveItemView extends JiveItemView {

    HomeMenuActivity mHomeMenuActivity;
    ItemAdapter mItemAdapter;

    public HomeMenuJiveItemView(HomeMenuActivity homeMenuActivity, View view, ItemAdapter adapter) {
        super(homeMenuActivity, view);
        mHomeMenuActivity = homeMenuActivity;
        mItemAdapter = adapter;
    }

    @Override
    public void bindView(JiveItem item) {
        super.bindView(item);
        itemView.setOnLongClickListener(view -> {
            if (!item.getId().equals(JiveItem.ARCHIVE.getId())) {
                if (!item.getNode().equals(JiveItem.ARCHIVE.getId())) {
                    if (mHomeMenuActivity.getService().checkIfItemIsAlreadyInArchive(item)) {
                        return true;
                    }
                }
                mItemAdapter.removeItem(getAdapterPosition());
                UndoBarController.show(mHomeMenuActivity, R.string.MENU_ITEM_MOVED, new UndoBarController.UndoListener() {
                    @Override
                    public void onUndo() {
                        mHomeMenuActivity.getService().toggleArchiveItem(item);
                        mHomeMenuActivity.getService().triggerHomeMenuEvent();
                    }
                    @Override
                    public void onDone() {
                    }
                });

                if ((mHomeMenuActivity.getService().toggleArchiveItem(item))) {
//                                    TODO: Do not instantly show the next screen or put UndoBar onto next screen
                    HomeActivity.show(Squeezer.getContext());
                    mHomeMenuActivity.getService().triggerDisplayMessage(Squeezer.getContext().getResources()
                            .getString(R.string.ARCHIVE_NODE_REMOVED));
                };
            }
            else {
                mHomeMenuActivity.getService().triggerDisplayMessage(Squeezer.getContext().getResources()
                        .getString(R.string.ARCHIVE_CANNOT_BE_ARCHIVED));
            }
            return true;
        });
    }

}
