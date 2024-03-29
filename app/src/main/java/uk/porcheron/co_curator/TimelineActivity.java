package uk.porcheron.co_curator;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Timer;
import java.util.TimerTask;

import uk.porcheron.co_curator.collo.ColloCompass;
import uk.porcheron.co_curator.collo.ColloDict;
import uk.porcheron.co_curator.collo.ColloManager;
import uk.porcheron.co_curator.db.DbLoader;
import uk.porcheron.co_curator.dialog.DialogManager;
import uk.porcheron.co_curator.item.ItemList;
import uk.porcheron.co_curator.item.ItemPhoto;
import uk.porcheron.co_curator.item.ItemScrollView;
import uk.porcheron.co_curator.item.ItemType;
import uk.porcheron.co_curator.item.ItemUrl;
import uk.porcheron.co_curator.dialog.DialogNote;
import uk.porcheron.co_curator.dialog.DialogUrl;
import uk.porcheron.co_curator.point.Pointer;
import uk.porcheron.co_curator.point.PointerPointer;
import uk.porcheron.co_curator.user.User;
import uk.porcheron.co_curator.user.UserList;
import uk.porcheron.co_curator.util.AnimationReactor;
import uk.porcheron.co_curator.util.CCLog;
import uk.porcheron.co_curator.util.Event;
import uk.porcheron.co_curator.util.Image;
import uk.porcheron.co_curator.util.Web;
import uk.porcheron.co_curator.val.Instance;
import uk.porcheron.co_curator.val.Phone;
import uk.porcheron.co_curator.val.Style;

public class TimelineActivity extends Activity implements View.OnLongClickListener,
        SurfaceHolder.Callback, ColloManager.ResponseHandler {
    private static final String TAG = "CC:TimelineActivity";

    private static boolean mCreated = false;
    private static TimelineActivity mInstance;

    private Timer mUpdateTimer;
    final Handler mUpdateHandler = new Handler();

    private View.OnTouchListener mGestureDetectorAbove;
    private View.OnTouchListener mGestureDetectorBelow;
    public ScaleGestureDetector mScaleDetector;

    private RelativeLayout mTimeline;
    private ItemScrollView mScrollView;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private ProgressDialog mProgressDialog;
    private FrameLayout mFrameLayout;
    private FrameLayout mOuterFrameLayout;
    private LinearLayout mLayoutAbove;
    private LinearLayout mLayoutBelow;

    private static final long FADE_TIME_ITEMS = 1000L;
    private static final int FADE_POINTERS = 50;

    public static final int PICK_PHOTO = 101;

    private boolean mUnbindAll = true;

    private float mLayoutTouchX = -1;
    private float mLayoutTouchY = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check the user has previously authenticated
        SharedPreferences sharedPrefs = getSharedPreferences(getString(R.string.prefFile), Context.MODE_PRIVATE);
        int globalUserId = sharedPrefs.getInt(getString(R.string.prefGlobalUserId), -1);
        int userId = sharedPrefs.getInt(getString(R.string.prefUserId), -1);
        int groupId = sharedPrefs.getInt(getString(R.string.prefGroupId), -1);
        String serverAddress = sharedPrefs.getString(getString(R.string.prefServerAddress), null);
        if(globalUserId >= 0 && userId >= 0 && groupId >= 0 && serverAddress != null && !serverAddress.isEmpty()) {
            Instance.globalUserId = globalUserId;
            Instance.userId = userId;
            Instance.groupId = groupId;
            Instance.serverAddress = serverAddress;
            Instance.addedUsers = 0;
            Instance.drawnUsers = 0;
            Log.d(TAG, "I am " + globalUserId + ":" + userId + ":" + groupId);
        } else {
            Intent intent = new Intent(this, ParticipantActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        // Setup the activity
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        setContentView(R.layout.activity_timelime);

        mInstance = this;

        CCLog.write(Event.APP_CREATE, Instance.asString());

        // Load various static values
        Phone.collectAttrs();
        Style.collectAttrs();

        // Begin preparation for drawing the UI
        mTimeline = (RelativeLayout) findViewById(R.id.timeline);
        mSurfaceView = (SurfaceView) findViewById(R.id.surface);
        mSurfaceView.setDrawingCacheEnabled(true);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        mScrollView = (ItemScrollView) findViewById(R.id.horizontalScrollView);
        mScrollView.setSmoothScrollingEnabled(true);

        mFrameLayout = (FrameLayout) findViewById(R.id.frameLayout);
        mOuterFrameLayout = (FrameLayout) findViewById(R.id.outerFrameLayout);

        mLayoutAbove = (LinearLayout) findViewById(R.id.layoutAboveCentre);
        mLayoutBelow = (LinearLayout) findViewById(R.id.layoutBelowCentre);

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, Style.layoutHalfPadding, 0, 0);

        final int padRight = (int) (Phone.screenWidth * Style.autoscrollExtra);
        mLayoutAbove.setPadding(0, 0, padRight, Style.layoutHalfPadding);
        mLayoutBelow.setPadding(0, 0, padRight, 0);
        mLayoutBelow.setLayoutParams(params);

        // Gesture recognition
        float bottomOffset = Style.layoutHalfHeight - (2 * Style.layoutCentreHeight);
        mGestureDetectorAbove = new TimelineGestureDetector(0);
        mGestureDetectorBelow = new TimelineGestureDetector(bottomOffset);
        mScaleDetector = new ScaleGestureDetector(this, new OverviewDetector());
//        mFrameLayout.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(final View view, final MotionEvent event) {
//                return mFrameLayout.onTouch(event) || mScaleDetector.onTouchEvent(event);
//            }
//        });

        mLayoutAbove.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                mGestureDetectorAbove.onTouch(view, event);
                return mScaleDetector.onTouchEvent(event);
            }
        });
        mLayoutBelow.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                mGestureDetectorBelow.onTouch(view, event);
                return mScaleDetector.onTouchEvent(event);
            }
        });

        mUnbindAll = true;

//        if(mCreated) {
//            return;
//        }

        showLoadingDialog(R.string.dialogLoading);

        // Load items
        Instance.users = new UserList();
        Instance.items = new ItemList(mScrollView, mLayoutAbove, mLayoutBelow);

        new DbLoader().execute();
    }

    public static TimelineActivity getInstance() {
        return mInstance;
    }

    @Override
    public void onResume() {
        CCLog.write(Event.APP_RESUME);

        Phone.collectAttrs();

        // Begin pitch tracking
        ColloCompass.getInstance().resumeListening();

        // Reschedule IP pinging
        try {
            mUpdateTimer = new Timer();
            mUpdateTimer.schedule(mUpdateUserTask, 1000, ColloManager.BEAT_EVERY);
        } catch(IllegalStateException e) {
        }

        // Fade in if not visible
        fadeIn(null);

        ColloManager.ResponseManager.registerHandler(ColloDict.ACTION_UNBIND, this);
        ColloManager.ResponseManager.registerHandler(ColloDict.ACTION_POINT, this);

        super.onResume();
    }

    @Override
    public void onPause() {
        ColloCompass.getInstance().pauseListening();

        // Pause timers
        mUpdateUserTask.cancel();
        mUpdateTimer.cancel();
        mUpdateTimer.purge();

        super.onPause();
    }

    @Override
    public void onDestroy() {
        saveAsImage(null);
        ColloManager.broadcast(ColloDict.ACTION_UNBIND);
        super.onDestroy();
    }

    @Override
    public boolean onLongClick(View v) {
        CCLog.write(Event.APP_LONG_CLICK);

        promptAdd(mLayoutTouchX);
        return true;
    }

    public void showLoadingDialog(int str) {
        Log.d(TAG, "Show loading dialog");
        mProgressDialog = ProgressDialog.show(this, "", getText(str), true);
    }

    public void fadeOut(final AnimationReactor listener) {
        mLayoutAbove.setVisibility(View.INVISIBLE);
        mLayoutBelow.setVisibility(View.INVISIBLE);
//        final Animation fadeOut = AnimationUtils.loadAnimation(this, R.anim.fade_out);
//        fadeOut.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
                if (listener != null) {
                    listener.onAnimationEnd();
                }
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
//
//        mLayoutAbove.startAnimation(fadeOut);
//        mLayoutBelow.startAnimation(fadeOut);

//        mLayoutAbove.animate()
//                .alpha(0f)
//                .setDuration(FADE_TIME_ITEMS)
//                .setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        mLayoutAbove.setVisibility(View.INVISIBLE);
//                        if (listener != null) {
//                            listener.onAnimationEnd(animation);
//                        }
//                    }
//                });
//        mLayoutBelow.animate()
//                .alpha(0f)
//                .setDuration(FADE_TIME_ITEMS)
//                .setListener(new AnimatorListenerAdapter() {
//                    @Override
//                    public void onAnimationEnd(Animator animation) {
//                        mLayoutBelow.setVisibility(View.INVISIBLE);
//                        if (listener != null) {
//                            listener.onAnimationEnd(animation);
//                        }
//                    }
//                });
    }

    public void fadeIn(final AnimationReactor listener) {
//        final Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
//        fadeIn.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {

//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
//
//        mLayoutAbove.startAnimation(fadeIn);
//        mLayoutBelow.startAnimation(fadeIn);
        mLayoutAbove.setVisibility(View.VISIBLE);
        mLayoutBelow.setVisibility(View.VISIBLE);
    }

    public void hideLoadingDialog() {
        mProgressDialog.hide();
    }

    public void promptAdd(float x) {
        CCLog.write(Event.APP_PROMPT_ADD, "{x=" + mLayoutTouchX + "}");
        Log.v(TAG, "User long press at (" + x + ")");
        promptNewItem(Instance.items.isEmpty());

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "Received ActivityResult (requestCode=" + requestCode + ",resultCode=" + resultCode + ")");

        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == PICK_PHOTO) {
            if (data == null) {
                Log.e(TAG, "No data retrieved...");
                return;
            }


            showLoadingDialog(R.string.dialogAddingImage);

            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String filename = cursor.getString(columnIndex);
            cursor.close();

            CCLog.write(Event.APP_PHOTO_ADD, "{filename=" + filename + "}");

            Log.v(TAG, "File selected by user: " + filename);

            int width = ItemPhoto.getThumbnailWidth();
            int height = ItemPhoto.getThumbnailHeight();

            synchronized (Instance.items) {
                final int itemId = Instance.items.size();
                final String destination = Instance.globalUserId + "-" + System.currentTimeMillis();
                Image.file2file(filename, destination, width, height, new Runnable() {
                    @Override
                    public void run() {
                        long dateTime = (System.currentTimeMillis() / 1000L);
                        if (!Instance.items.add(itemId, ItemType.PHOTO, Instance.user(), destination, dateTime, false, true, true)) {
                            Log.e(TAG, "Failed to save image");
                        }

                        TimelineActivity.getInstance().hideLoadingDialog();
                    }
                });
            }
        }
    }

    @Override
    public boolean respond(String action, int globalUserId, String... data) {
        switch(action) {
            case ColloDict.ACTION_UNBIND:
                ColloManager.unBindFromUser(globalUserId);
                return true;

            case ColloDict.ACTION_POINT:
                if(!ColloManager.isBoundTo(globalUserId)) {
                    Log.v(TAG, "Ignore pointer, not bound to user");
                    return false;
                }

                final User u = Instance.users.getByGlobalUserId(globalUserId);
                if(u == null) {
                    Log.e(TAG, "Badly formed globalUserId");
                    return true;
                }

                try {
                    final float x = Float.parseFloat(data[0]);
                    final float y = Float.parseFloat(data[1]);
                    TimelineActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showPointer(u, x, y);
                        }
                    });
                    return true;
                } catch(NumberFormatException e) {
                    Log.e(TAG, "Badly formed ACTION_POINT");
                }

                return false;
        }

        return false;
    }


    public void promptNewItem(final boolean forceAdd) {
        CCLog.write(Event.TL_NEW_ITEM, "{forceAdd=" + forceAdd + "}");

        AlertDialog.Builder builder = new AlertDialog.Builder(TimelineActivity.this);
        builder.setTitle(R.string.dialog_add_message);

        if(forceAdd) {
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    promptNewItem(true);
                }
            });
            builder.setCancelable(false);
        }

        final ItemType[] types = ItemType.values();
        CharSequence[] typeLabels = new CharSequence[types.length - 1];
        for (int i = 0; i < typeLabels.length; i++) {
            typeLabels[i] = getString(types[i+1].getLabel()); // first item is the UNKNOWN type
        }

        builder.setItems(typeLabels, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                ItemType type = types[which + 1]; // first item is the UNKNOWN type
                switch (type) {
                    case PHOTO:
                        addNewPhoto();
                        break;

                    case NOTE:
                        addNewNote(forceAdd);
                        break;

                    case URL:
                        addNewUrl(forceAdd);
                        break;
                }
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void addNewNote(final boolean promptOnCancel) {
        CCLog.write(Event.TL_NEW_NOTE, "{promptOnCancel=" + promptOnCancel + "}");

        new DialogNote(this)
                .setAutoEdit(true)
                .setOnSubmitListener(new DialogNote.OnSubmitListener() {
                    @Override
                    public void onSubmit(DialogInterface dialog, String text) {
                        Log.e(TAG, "Note Submitted");

                        if (promptOnCancel && text.isEmpty()) {
                            promptNewItem(true);
                        }

                        CCLog.write(Event.TL_NEW_SAVE, "{" + text + "}");

                        synchronized (Instance.items) {
                            final int itemId = Instance.items.size();

                            long dateTime = Instance.items.getDateTimeClosestTo(mLayoutTouchX);

                            boolean create = Instance.items.add(itemId, ItemType.NOTE, Instance.user(), text, dateTime, false, true, true);
                            if (promptOnCancel && !create) {
                                promptNewItem(true);
                            }
                        }
                    }
                })
                .setOnCancelListener(new DialogNote.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        CCLog.write(Event.TL_NEW_CANCEL);
                        if (promptOnCancel) {
                            promptNewItem(true);
                        }
                    }
                })
                .isDeletable(true)
                .isEditable(true)
                .create()
                .show();
    }

    private void addNewUrl(final boolean promptOnCancel) {
        CCLog.write(Event.TL_NEW_URL, "{promptOnCancel=" + promptOnCancel + "}");

        new DialogUrl(this)
                .setAutoEdit(true)
                .setOnSubmitListener(new DialogNote.OnSubmitListener() {
                    @Override
                    public void onSubmit(DialogInterface dialog, String text) {
                        Log.e(TAG, "URL Submitted");

                        if (promptOnCancel && text.isEmpty()) {
                            promptNewItem(true);
                        }

                        CCLog.write(Event.TL_NEW_SAVE, "{" + text + "}");

                        showLoadingDialog(R.string.dialogAddingUrl);

                        if (!text.startsWith("http://") && !text.startsWith("https://")) {
                            text = "http://" + text;
                        }

                        synchronized (Instance.items) {
                            final int itemId = Instance.items.size();
                            final String url = text;
                            final String b64Url = Web.b64encode(text);
                            final String filename = itemId + "-" + b64Url;
                            final String fetchFrom = Web.GET_URL_SCREENSHOT + b64Url;

                            boolean isVideo = ItemUrl.isVideo(url);
                            final int width = ItemUrl.getThumbnailWidth(isVideo);
                            final int height = ItemUrl.getThumbnailHeight(isVideo);

                            new Thread(new Runnable() {

                                @Override
                                public void run() {

                            Image.url2File(fetchFrom, filename, width, height, new Runnable() {
                                @Override
                                public void run() {
                                    long dateTime = (System.currentTimeMillis() / 1000L);
                                    if (!Instance.items.add(itemId, ItemType.URL, Instance.user(), url, dateTime, false, true, true)) {
                                        Log.e(TAG, "Failed to save URL + screenshot");
                                        if (promptOnCancel) {
                                            promptNewItem(true);
                                            return;
                                        }
                                    }

                                    TimelineActivity.getInstance().hideLoadingDialog();
                                }
                            }, new Runnable() {
                                @Override
                                public void run() {
                                    TimelineActivity.getInstance().runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {

                                    Toast.makeText(TimelineActivity.getInstance(), R.string.toastFailedSaveUrl, Toast.LENGTH_LONG).show();
                                    TimelineActivity.getInstance().hideLoadingDialog();

                                        }
                                    });
                                }
                            });
                                }
                            }).start();
                        }
                    }
                })
                .setOnCancelListener(new DialogNote.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        CCLog.write(Event.TL_NEW_CANCEL);
                        if (promptOnCancel) {
                            promptNewItem(true);
                        }
                    }
                })
                .isDeletable(true)
                .isEditable(true)
                .create()
                .show();
    }

    private void addNewPhoto() {
        CCLog.write(Event.TL_NEW_PHOTO);

        Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickIntent.setType("image/*");

        Intent chooserIntent = Intent.createChooser(pickIntent, "Select Image");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});

        startActivityForResult(pickIntent, TimelineActivity.PICK_PHOTO);
    }

    public void redrawCentrelines() {
        Log.v(TAG, "Redraw Centrelines");
        updateCanvas(mSurfaceHolder);
    }

    private void updateCanvas(SurfaceHolder holder) {
        Log.v(TAG, "Redraw canvas");

        Canvas canvas = holder.lockCanvas();
        drawCanvas(canvas);
        try {
            holder.unlockCanvasAndPost(canvas);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error unlocking and posting canvas: " + e.getMessage());
        }
    }

    private void drawCanvas(Canvas canvas) {
        try {
            canvas.drawColor(Style.backgroundColor);

            int w = canvas.getWidth();
            int h = canvas.getHeight();

            for(User user : Instance.users) {
                if (user.draw()) {
                    int y1 = (int) (((h - Style.layoutCentreHeight) / 2) + user.centrelineOffset);
                    int y2 = (int) (y1 + Style.lineWidth);

                    canvas.drawRect(0, y1, w, y2, user.bgPaint);
                }
            }
        } catch(NullPointerException e) {
            Log.e(TAG, "NullPointer in canvas update: " + e.getMessage());
        }
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        updateCanvas(holder);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        updateCanvas(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
    }

    private static final long DISMISS_LOADING_IN = 3000L;
    private Handler mLoadedHandler = new Handler();
    private Runnable mLoadedRunner = new Runnable() {
        @Override
        public void run() {
            TimelineActivity.this.hideLoadingDialog();
        }
    };

    private TimerTask mUpdateUserTask = new TimerTask() {
        @Override
        public void run() {
            mUpdateHandler.post(new Runnable() {
                public void run() {
                    try {
                        ColloManager.beat(mUnbindAll);
                        if (mUnbindAll) {
                            mLoadedHandler.postDelayed(mLoadedRunner, DISMISS_LOADING_IN);
                            mUnbindAll = false;
                        }
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                    }
                }
            });
        }
    };

    private static long LONG_PRESS_DELAY = 500;
    private static float DOUBLE_TAP_POINT_LEEWAY = 50f;
    private static float SCALE_LEEWAY = 150f;
    private static long DOUBLE_TAP_TIME_LEEWAY = 450L;
    private static long PINCH_TAP_TIME_LEEWAY = 100L;
    private static long mLastTap = -1;

    private final static SparseArray<Runnable> mPointerHandlerRunners = new SparseArray<>();
    private final static SparseArray<Handler> mPointerHandlers = new SparseArray<>();
    private static SparseArray<Pointer> mPointers = new SparseArray<>();
    private static SparseArray<PointerPointer> mPointerPointers = new SparseArray<>();

    private class TimelineGestureDetector extends GestureDetector.SimpleOnGestureListener implements View.OnTouchListener {

        private final float mYOffset;

        public TimelineGestureDetector(float yOffset) {
            mYOffset = yOffset;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            CCLog.write(Event.COLLO_POINT, "{x=" + e.getX() + ",y=" + mYOffset + e.getY() + "}");
            ColloManager.broadcast(ColloDict.ACTION_POINT, e.getX(), mYOffset + e.getY());

            showPointer(Instance.user(), e.getX(), mYOffset + e.getY());

            return true;
        }

        public boolean onDown(MotionEvent e) {
            super.onDown(e);
            return true;
        }

        @Override
        public boolean onTouch(View v, MotionEvent e) {
            long now = System.currentTimeMillis();

            if(e.getAction() == MotionEvent.ACTION_CANCEL
                    || e.getAction() == MotionEvent.ACTION_UP
                    || e.getAction() == MotionEvent.ACTION_SCROLL) {
                mLongPressHandler.removeCallbacks(mLongPressed);
            }

            if(e.getAction() == MotionEvent.ACTION_UP) {
                if(Math.abs(mLayoutTouchX - e.getX()) < DOUBLE_TAP_POINT_LEEWAY) {
                    float diffY = Math.abs(mLayoutTouchY - (e.getY() + mYOffset));
                    if(now - mLastTap < PINCH_TAP_TIME_LEEWAY && diffY > SCALE_LEEWAY) {
                        Intent i = new Intent(TimelineActivity.this, OverviewActivity.class);
                        startActivity(i);
                    } else if (now - mLastTap < DOUBLE_TAP_TIME_LEEWAY && diffY < DOUBLE_TAP_POINT_LEEWAY) {
                        return onDoubleTap(e);
                    }
                }

                mLastTap = now;
                mLayoutTouchY = e.getY() + mYOffset;
            }

            if(e.getAction() == MotionEvent.ACTION_DOWN) {
                mLongPressHandler.postDelayed(mLongPressed, LONG_PRESS_DELAY);
            }

            mLayoutTouchX = e.getX();

            return false;
        }
    }

    final Handler mLongPressHandler = new Handler();
    Runnable mLongPressed = new Runnable() {
        public void run() {
            promptAdd(mLayoutTouchX);
        }
    };

    class OverviewDetector extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            TimelineActivity.this.fadeOut(new AnimationReactor() {
                @Override
                public void onAnimationEnd() {
                    Intent i = new Intent(TimelineActivity.this, OverviewActivity.class);
                    startActivity(i);
                }
            });
        }

    }

    private synchronized void showPointer(final User user, float x, float y) {
        final int userId = user.userId;

        hidePointer(user);

        final Pointer p = new Pointer(user, x, y);
        p.setTranslationX(x);
        //p.setTranslationY(y);
        p.setTranslationY(Style.layoutHalfPadding + user.centrelineOffset - (Style.pointerMinSize / 2) - 3);
        mFrameLayout.addView(p);
        p.bringToFront();
        mPointers.put(userId, p);

        if(x < mScrollView.getScrollX()) {
            showPointerPointer(user, y, false);
        } else if(x > mScrollView.getScrollX() + mScrollView.getWidth()) {
            showPointerPointer(user, y, true);
        } else {
            hidePointerPointer(user, null);
        }

        if(Style.pointerVisibleFor > 0) {
            mPointerHandlerRunners.put(userId, new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Remove pointer for userId=" + userId);
                    mPointers.remove(userId);
                    mFrameLayout.removeView(p);
                    hidePointerPointer(user, null);
                }
            });

            mPointerHandlers.get(userId, new Handler()).postDelayed(mPointerHandlerRunners.get(userId), Style.pointerVisibleFor);
        }
    }

    public synchronized void hidePointer(final User user) {
        int userId = user.userId;

        Pointer p = mPointers.get(userId);
        if(p != null) {
            mFrameLayout.removeView(p);
            mPointers.remove(userId);
        }

        Handler h = mPointerHandlers.get(userId);
        if(h != null) {
            h.removeCallbacks(mPointerHandlerRunners.get(userId));
        }
    }

    public synchronized void showPointerPointer(final User user, final float y, final boolean pointRight) {
        final PointerPointer pp = mPointerPointers.get(user.userId);
        if(pp == null || pp.getPointRight() != pointRight || y != pp.getYPosition()) {
            hidePointerPointer(user, new AnimationReactor() {
                @Override
                public void onAnimationEnd() {
                    Log.d(TAG, "Show Pointer Pointer");
                    final PointerPointer pp = new PointerPointer(user, y, pointRight);
                    if (pointRight) {
                        pp.setTranslationX(mOuterFrameLayout.getWidth() - Style.pointerPointerXOffset - Style.pointerPointerArrowLength - Style.pointerPointerCircleSize);
                    } else {
                        pp.setTranslationX(Style.pointerPointerXOffset);
                    }


                    pp.setTranslationY(Style.layoutHalfPadding + user.centrelineOffset - (Style.pointerPointerCircleSize / 2) + 4);
                    //pp.setTranslationY(y + Style.pointerPointerYOffset);
                    //pp.setTranslationY(Style.pointerPointerYOffset + (mPointerPointers.size() * (Style.pointerPointerYOffset + Style.pointerPointerCircleSize)));

                    mPointerPointers.put(user.userId, pp);
                    mOuterFrameLayout.addView(pp);
                    pp.bringToFront();
                }
            });
        }
    }

    public synchronized void hidePointerPointer(final User user, final AnimationReactor animationReactor) {
        final PointerPointer pp = mPointerPointers.get(user.userId);
        if(pp != null) {
            mPointerPointers.remove(user.userId);
            pp.animate().alpha(0f).setDuration(FADE_POINTERS).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mOuterFrameLayout.removeView(pp);

                    if (animationReactor != null) {
                        animationReactor.onAnimationEnd();
                    }
                }
            });
        } else if(animationReactor != null) {
            animationReactor.onAnimationEnd();
        }
    }

    public synchronized void testPointers(int x1, int x2) {
        for(int index = 0; index < mPointers.size(); index++) {
            int userId = mPointers.keyAt(index);
            Pointer p = mPointers.get(userId);

            boolean left = x1 > p.getTriggeredX();
            boolean right = p.getTriggeredX() > (x2 - Style.pointerPointerCircleSize - Style.pointerPointerXOffset);// + Style.pointerPointerArrowLength)

            if (!left && !right) {
                hidePointerPointer(p.getUser(), null);
            } else if(!left) {
                showPointerPointer(p.getUser(), p.getTriggeredY(), true);
            } else {
                showPointerPointer(p.getUser(), p.getTriggeredY(), false);
            }
        }
    }

    private void cleanUpPointers() {
        for(int index = 0; index < mPointers.size(); index++) {
            int userId = mPointers.keyAt(index);

            // Remove cleanup handler
            Handler h = mPointerHandlers.get(userId);
            if (h != null) {
                h.removeCallbacks(mPointerHandlerRunners.get(userId));
            }

            // Remove pointer
            Pointer p = mPointers.get(userId);
            if(p != null) {
                mPointers.remove(userId);
                mFrameLayout.removeView(p);
            }

            // Remove pointer pointer
            PointerPointer pp = mPointerPointers.get(userId);
            if(pp != null) {
                mPointerPointers.remove(userId);
                mOuterFrameLayout.removeView(pp);
            }
        }
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if(!DialogManager.dialogShown() && ColloManager.totalBoundTo() > 0) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showLoadingDialog(R.string.dialogScreenshot);
                            saveAsImage(new OnCompleteRunner() {
                                @Override
                                public void run() {
                                    ColloManager.broadcast(ColloDict.ACTION_UNBIND);
                                    ColloManager.unBindFromUsers();

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            hideLoadingDialog();
                                        }
                                    });
                                }
                            });
                        }
                    });
            }
            return true;
        }

        return super.onKeyUp(keyCode, event);
    }

    private void saveAsImage(final OnCompleteRunner onCompleteRunner) {
        if(mFrameLayout == null) {
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bitmap = Bitmap.createBitmap(mFrameLayout.getWidth(), mFrameLayout.getHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);

                    drawCanvas(canvas);
                    mFrameLayout.draw(canvas);

                    // Create directory
                    File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                    if (!dir.exists() && !dir.mkdirs()) {
                        Log.e(TAG, "Directory not created");
                        return;
                    }

                    // Save file
                    String file = "/timeline-" + Instance.groupId + "-" + Instance.userId + "-" + Instance.globalUserId + "-" + System.currentTimeMillis() + ".png";
                    Log.d(TAG, "Save screenshot to " + dir.getPath() + file);
                    FileOutputStream fileOutputStream = new FileOutputStream(dir.getPath() + file);

                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);

                    fileOutputStream.flush();
                    fileOutputStream.close();

                    Log.d(TAG, "Saved screenshot!");
                } catch (Exception e) {
                    // TODO: handle exception
                    Log.e(TAG, "Error saving file - " + e.getMessage());
                } finally {
                    mFrameLayout.destroyDrawingCache();
                    if(onCompleteRunner != null) {
                        onCompleteRunner.run();
                    }
                }
            }
        }).start();
    }

    public interface OnCompleteRunner {
        void run();
    }



}

