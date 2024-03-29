package uk.porcheron.co_curator.user;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

import uk.porcheron.co_curator.db.DbHelper;
import uk.porcheron.co_curator.db.TableUser;
import uk.porcheron.co_curator.item.Item;
import uk.porcheron.co_curator.item.ItemList;
import uk.porcheron.co_curator.val.Instance;

/**
 * List of users within the user's group.
 */
public class UserList extends ArrayList<User> {
    private static final String TAG = "CC:UserList";

    private DbHelper mDbHelper;

    private SparseArray<User> mGlobalUserIds = new SparseArray<>();
    private SparseArray<Boolean> mDrawnUsers = new SparseArray<>();

    public UserList() {
        mDbHelper = DbHelper.getInstance();
    }

    public User add(int globalUserId, int userId, boolean addToLocalDb) {
        Log.v(TAG, "User[" + globalUserId + "]: Add to List (globalUserId=" + globalUserId + ",userId=" + userId + ",addToLocalDb=" + addToLocalDb + ")");

        User user = new User(globalUserId, userId);
        add(user);
        mGlobalUserIds.put(globalUserId, user);

        // Local Database
        if(!addToLocalDb) {
            Log.v(TAG, "User[" + globalUserId + "] not created in DB, as requested");
            return user;
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(TableUser.COL_GLOBAL_USER_ID, globalUserId);
        values.put(TableUser.COL_USER_ID, userId);

        long newRowId;
        newRowId = db.insert(
                TableUser.TABLE_NAME,
                null,
                values);

        db.close();

        if(newRowId >= 0) {
            Log.d(TAG, "User[" + globalUserId + "]: Created in Db (rowId=" + newRowId + ")");
        } else {
            Log.d(TAG, "User[" + globalUserId + "]: Could not create in Db");
        }

        return user;
    }

    public User getByGlobalUserId(int globalUserId) {
        return mGlobalUserIds.get(globalUserId);
    }

    public synchronized void drawUser(int globalUserId) {
        User u = getByGlobalUserId(globalUserId);
        if(u != null && !u.draw()) {
            u.willDraw();

            List<Item> list = Instance.items.getByGlobalUserId(globalUserId);
            if (list == null) {
                return;
            }
            for(Item i : list) {
                i.reassessBounds();
            }
        }
    }

    public synchronized void unDrawUser(int globalUserId) {
        User u = getByGlobalUserId(globalUserId);
        if(u != null) {
            u.willUnDraw();
        }
    }
}
