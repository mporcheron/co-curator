package uk.porcheron.co_curator.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import uk.porcheron.co_curator.TimelineActivity;
import uk.porcheron.co_curator.item.ItemList;
import uk.porcheron.co_curator.item.ItemType;
import uk.porcheron.co_curator.user.User;
import uk.porcheron.co_curator.val.Instance;

/**
 * Class for loading items from the local and cloud databases.
 */
public class DbLoader extends AsyncTask<Void, Void, Boolean> {
    private static final String TAG = "CC:DbLoader";

    private TimelineActivity mActivity = TimelineActivity.getInstance();
    private DbHelper mDbHelper = DbHelper.getInstance();

    public DbLoader() {
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        try {
            loadUsersFromDb();
            loadItemsFromDb(-1);
        } catch (Exception e) {
            Log.e(TAG, "Error loading data: " + e.getMessage());
            return Boolean.FALSE;
        }
        return Boolean.TRUE;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        mActivity.hideLoadingDialog();
        if (Instance.items.isEmpty()) {
            mActivity.promptAdd();
        }
    }

    protected void loadUsersFromDb() throws Exception {
        Log.d(TAG, "Initial load of users from DB");

        try (SQLiteDatabase db = mDbHelper.getWritableDatabase()) {
            String[] projection = {
                    TableUser.COL_GLOBAL_USER_ID,
                    TableUser.COL_USER_ID,
            };

            String sortOrder =
                     "rowid ASC";

            Cursor c = db.query(
                    TableUser.TABLE_NAME,
                    projection,
                    null,
                    null,
                    null,
                    null,
                    sortOrder
            );

            c.moveToFirst();
            for (int i = 0; i < c.getCount(); i++) {
                int cGlobalUserId = c.getInt(0);
                int cUserId = c.getInt(1);

                Instance.users.add(cGlobalUserId, cUserId, false);

                c.moveToNext();
            }
            c.close();
        }

        // Current user doesn't exist?
        if (Instance.user() == null) {
            Log.d(TAG, "Create local user in local DB");
            Instance.users.add(Instance.globalUserId, Instance.userId, true);
        }

        // Get users from the cloud
        Log.d(TAG, "Get users from cloud...");
        WebLoader.loadUsersFromWeb();
    }

    protected ItemList loadItemsFromDb(int globalUserId) throws Exception {
        Log.d(TAG, "Initial load of items from DB");

        boolean allUsers = globalUserId < 0;

        try (SQLiteDatabase db = mDbHelper.getReadableDatabase()) {
            String[] projection = {
                    TableItem.COL_ITEM_ID,
                    TableItem.COL_GLOBAL_USER_ID,
                    TableItem.COL_ITEM_TYPE,
                    TableItem.COL_ITEM_DATA,
                    TableItem.COL_ITEM_DATETIME
            };

            String selection = "";
            if (!allUsers) {
                selection = TableItem.COL_GLOBAL_USER_ID + " = '" + globalUserId + "'";
            }

            String sortOrder =
                    TableItem.COL_ITEM_DATETIME + " ASC";

            Cursor c = db.query(
                    TableItem.TABLE_NAME,
                    projection,
                    selection,
                    null,
                    null,
                    null,
                    sortOrder
            );

            c.moveToFirst();
            for (int i = 0; i < c.getCount(); i++) {
                final int cItemId = c.getInt(0);
                int cGlobalUserId = c.getInt(1);
                int cTypeId = c.getInt(2);

                User aUser = Instance.users.getByGlobalUserId(cGlobalUserId);
                if (aUser == null) {
                    aUser = Instance.users.add(cGlobalUserId, Instance.users.size(), false);
                }
                final User user = aUser;

                final ItemType type = ItemType.get(cTypeId);

                final String cData = c.getString(3);
                final int cDateTime = c.getInt(4);

                if (cData != null) {
                    Log.v(TAG, "Item[" + i + "]: Save (itemId=" + cItemId + ",type=" + type.toString() + ",dateTime="+cDateTime + ",data='" + cData + "')");

                    mActivity.runOnUiThread(new Runnable() {
                        public void run() {
                            Instance.items.add(cItemId, type, user, cData, cDateTime, false, false);
                        }
                    });
                } else {
                    Log.e(TAG, "Item[" + i + "]: Error (itemId=" + cItemId + ",type=" + type.toString() + ") is NULL");
                }

                c.moveToNext();
            }

            c.close();
        }

        if (allUsers) {
            for (User u : Instance.users) {
                WebLoader.loadItemsFromWeb(u.globalUserId);
            }
        } else {
            WebLoader.loadItemsFromWeb(globalUserId);
        }


        mActivity.runOnUiThread(new Runnable() {
            public void run() {
                mActivity.redrawCentrelines();
            }
        });

        return Instance.items;
    }
}
