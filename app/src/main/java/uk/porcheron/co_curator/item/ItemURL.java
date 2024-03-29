package uk.porcheron.co_curator.item;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;

import uk.porcheron.co_curator.dialog.DialogNote;
import uk.porcheron.co_curator.dialog.DialogUrl;
import uk.porcheron.co_curator.user.User;
import uk.porcheron.co_curator.util.CCLog;
import uk.porcheron.co_curator.util.Event;
import uk.porcheron.co_curator.util.Image;
import uk.porcheron.co_curator.util.Web;
import uk.porcheron.co_curator.val.Instance;
import uk.porcheron.co_curator.val.Style;

/**
 * An item that contains a URL.
 */
public class ItemUrl extends ItemPhoto {
    private static final String TAG = "CC:ItemURL";

    private String mUrl;

    private boolean mIsVideo = false;
    private static String[] YOUTUBE_URLS = {
            "http://m.youtube.com",
            "https://m.youtube.com",
            "http://www.youtube.com",
            "https://www.youtube.com",
            "http://youtube.com",
            "https://youtube.com",
            "http://youtu.be",
            "https://youtu.be"
    };

    public ItemUrl(Context context) { super(context); }

    public ItemUrl(Context context, AttributeSet attrs) { super(context, attrs); }

    public ItemUrl(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    public ItemUrl(int itemId, User user, long dateTime) {
        super(itemId, user, dateTime);

        setBounds(Style.photoWidth, Style.photoHeight, Style.photoPadding);
    }

    public String setData(String url) {
        mUrl = url;
        mIsVideo = isVideo(url);

        if(mIsVideo) {
            setBounds(Style.videoWidth, Style.videoHeight, Style.videoPadding);
        } else {
            setBounds(Style.urlWidth, Style.urlHeight, Style.urlPadding);
        }

        return super.setData(getItemId() + "-" + Web.b64encode(url));
    }

    public boolean dataChanged(String data) {
        return !mUrl.equals(data);
    }

    @Override
    public boolean onTap(Activity activity) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(mUrl));
        getContext().startActivity(browserIntent);
        return true;
    }

    @Override
    public void simulateTap(Activity activity) {
        onTap();
    }

    public static boolean isVideo(String url) {
        for (String youtubeUrl : YOUTUBE_URLS) {
            if (url.startsWith(youtubeUrl)) {
                return true;
            }
        }
        return false;
    }

    public static int getThumbnailWidth(boolean isVideo) {
        if (isVideo) {
            return (int) (Style.videoWidth - (2 * Style.videoPadding));
        } else {
            return (int) (Style.urlWidth - (2 * Style.urlPadding));
        }
    }

    public static int getThumbnailHeight(boolean isVideo) {
        if (isVideo) {
            return (int) (Style.videoHeight - (2 * Style.videoPadding));
        } else {
            return (int) (Style.urlHeight - (2 * Style.urlPadding));
        }
    }

    @Deprecated
    public static int getThumbnailWidth() {
        return getThumbnailWidth(false);
    }

    @Deprecated
    public static int getThumbnailHeight() {
        return getThumbnailHeight(false);
    }


    @Override
    protected boolean onLongPress(Activity activity) {
        onSelect(activity, false, true, true);
        return true;
    }

    @Override
    protected void onSelect(Activity activity, boolean fullScreen,boolean editable, boolean deletable) {
        new DialogUrl(activity)
                .setText(mUrl)
                .setOnSubmitListener(new DialogNote.OnSubmitListener() {
                    @Override
                    public void onSubmit(DialogInterface dialog, String text) {
                        Log.d(TAG, "Update Url to " + text);

                        CCLog.write(Event.ITEM_UPDATE, "{uniqueItemId=" + getUniqueItemId() + ",text=" + text + "}");

                        final String url = text;
                        final String b64Url = Web.b64encode(text);
                        final String filename = getItemId() + "-" + b64Url;
                        final String requestUrl = Web.GET_URL_SCREENSHOT + b64Url;

                        boolean isVideo = ItemUrl.isVideo(text);
                        final int width = ItemUrl.getThumbnailWidth(isVideo);
                        final int height = ItemUrl.getThumbnailHeight(isVideo);

                        new Thread(new Runnable() {

                            @Override
                            public void run() {
                                Image.url2File(requestUrl, filename, width, height, new Runnable() {
                                    @Override
                                    public void run() {
                                        Log.d(TAG, "Screenshot saved as " + filename);
                                        Instance.items.update(ItemUrl.this, url, true, true);
                                    }
                                }, null);
                            }
                        }).start();
                    }
                })
                .setOnDeleteListener(new DialogNote.OnDeleteListener() {
                    @Override
                    public void onDelete(DialogInterface dialog) {
                        CCLog.write(Event.ITEM_DELETE, "{uniqueItemId=" + getUniqueItemId() + "}");
                        Instance.items.remove(ItemUrl.this, true, true, true);
                    }
                })
                .setFullScreen(fullScreen)
                .setUser(getUser())
                .isDeletable(deletable)
                .isEditable(editable)
                .create()
                .show();
        }
    }
