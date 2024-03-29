package uk.porcheron.co_curator.val;

import android.content.res.Resources;
import android.graphics.Paint;

import uk.porcheron.co_curator.R;
import uk.porcheron.co_curator.TimelineActivity;

/**
 * Style properties for the application.
 */
public class Style {
    private static final String TAG = "CC:Style";

    public static int backgroundColor;

    public static float layoutCentreHeight;

    public static float autoscrollSlack;
    public static float autoscrollExtra;

    public static int[] userPositions = new int[4];
    public static int[] userBgColors = new int[4];
    public static int[] userMeBgColors = new int[4];
    public static int[] userFgColors = new int[4];
    public static int[] userOffsets = new int[4];
    public static int[] userLayers = new int[4];

    public static float lineCentreGap;
    public static float lineWidth;

    public static int layoutHalfHeight;
    public static int layoutHalfPadding;

    public static float dialogMinXSpace;
    public static float dialogMinYSpace;

    public static float itemXGapMin;
    public static float itemXGapMax;
    public static float itemXGapOffset;
    public static float itemOutlineSize;
    public static int itemRoundedCorners;
    public static float itemStemNarrowBy;
    public static float itemFullHeight;
    public static int itemStemGrowOver;
    public static int itemStemGrowSteps;

    public static float photoWidth;
    public static float photoHeight;
    public static float photoPadding;

    public static float urlWidth;
    public static float urlHeight;
    public static float urlPadding;

    public static float videoWidth;
    public static float videoHeight;
    public static float videoPadding;

    public static float noteWidth;
    public static float noteHeight;
    public static float notePadding;
    public static int noteLines;
    public static int noteFontSize;
    public static float noteLineSpacing;
    public static int noteLength;

    public static int overviewNumItems;
    public static int overviewItemSize;
    public static float overviewItemBorder;

    public static Paint normalPaint;

    public static float pointerMaxSize;
    public static float pointerMinSize;
    public static int pointerPulseSteps;
    public static float pointerPulseStepIncrement;
    public static int pointerVisibleFor;

    public static float pointerPointerCircleSize;
    public static float pointerPointerArrowLineWidth;
    public static float pointerPointerArrowLength;
    public static float pointerPointerArrowHeadDepth;
    public static float pointerPointerArrowHeadHeight;
    public static double pointerPointerArrowHeadArmLength;
    public static float pointerPointerXOffset;
    public static float pointerPointerYOffset;


    public static void collectAttrs() {
        Resources res = TimelineActivity.getInstance().getResources();

        // Get style settings
        backgroundColor = res.getColor(R.color.background);
        layoutCentreHeight = res.getDimension(R.dimen.layoutCentreHeight);
        layoutHalfHeight = (int) ((Phone.screenHeight / 2) + layoutCentreHeight);
        layoutHalfPadding = (int) ((Phone.screenHeight / 2) - (layoutCentreHeight / 2));

        autoscrollSlack = res.getDimension(R.dimen.autoscrollSlack);
        autoscrollExtra = res.getInteger(R.integer.autoscrollExtra) / 100f;

        userPositions = res.getIntArray(R.array.userPositions);

        userMeBgColors[0] = res.getColor(R.color.userMeBg0);
        userMeBgColors[1] = res.getColor(R.color.userMeBg1);
        userMeBgColors[2] = res.getColor(R.color.userMeBg2);
        userMeBgColors[3] = res.getColor(R.color.userMeBg3);

        userBgColors[0] = res.getColor(R.color.userBg0);
        userBgColors[1] = res.getColor(R.color.userBg1);
        userBgColors[2] = res.getColor(R.color.userBg2);
        userBgColors[3] = res.getColor(R.color.userBg3);

        userFgColors[0] = res.getColor(R.color.userFg0);
        userFgColors[1] = res.getColor(R.color.userFg1);
        userFgColors[2] = res.getColor(R.color.userFg2);
        userFgColors[3] = res.getColor(R.color.userFg3);

        userOffsets = res.getIntArray(R.array.userOffsets);
        userLayers = res.getIntArray(R.array.userLayers);

        dialogMinXSpace = res.getDimension(R.dimen.dialogMinXSpace);
        dialogMinYSpace = res.getDimension(R.dimen.dialogMinYSpace);

        lineCentreGap = res.getDimension(R.dimen.lineCentreGap);
        lineWidth = res.getDimension(R.dimen.lineWidth);

        itemXGapMin = res.getDimension(R.dimen.itemXGapMin);
        itemXGapMax = res.getDimension(R.dimen.itemXGapMax);
        itemXGapOffset = itemXGapMax - itemXGapMin;
        itemOutlineSize = res.getDimension(R.dimen.itemOutlineSize);
        itemRoundedCorners = (int) res.getDimension(R.dimen.itemRoundedCorners);
        itemStemNarrowBy = res.getDimension(R.dimen.itemStemNarrowBy);
        itemFullHeight = ((Phone.screenHeight / 2) - (res.getDimension(R.dimen.layoutCentreHeight) / 2));
        itemStemGrowOver = res.getInteger(R.integer.itemStemGrowOver);
        itemStemGrowSteps = res.getInteger(R.integer.itemStemGrowSteps);

        photoWidth = res.getDimension(R.dimen.photoWidth);
        photoHeight = res.getDimension(R.dimen.photoHeight);
        photoPadding = res.getDimension(R.dimen.photoPadding);

        urlWidth = res.getDimension(R.dimen.urlWidth);
        urlHeight = res.getDimension(R.dimen.urlHeight);
        urlPadding = res.getDimension(R.dimen.urlPadding);

        videoWidth = res.getDimension(R.dimen.videoWidth);
        videoHeight = res.getDimension(R.dimen.videoHeight);
        videoPadding = res.getDimension(R.dimen.videoPadding);

        noteWidth = res.getDimension(R.dimen.noteWidth);
        noteHeight = res.getDimension(R.dimen.noteHeight);
        notePadding = res.getDimension(R.dimen.notePadding);
        noteLines = res.getInteger(R.integer.noteLines);
        noteFontSize = res.getInteger(R.integer.noteFontSize);
        noteLineSpacing  = res.getInteger(R.integer.noteLineSpacing) / 10f;
        noteLength = res.getInteger(R.integer.noteLength);

        float rvWidth = Phone.screenWidth;
        overviewNumItems = res.getInteger(R.integer.overviewNumItems);
        overviewItemSize = (int) ((rvWidth / overviewNumItems) - res.getDimension(R.dimen.overviewItemMargin));
        overviewItemBorder = res.getDimension(R.dimen.overviewItemBorder);

        normalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        pointerMaxSize = res.getDimension(R.dimen.pointerMaxSize);
        pointerMinSize = res.getDimension(R.dimen.pointerMinSize);
        pointerPulseSteps = res.getInteger(R.integer.pointerPulseSteps);
        pointerPulseStepIncrement = res.getDimension(R.dimen.pointerPulseStepIncrement);
        pointerVisibleFor = res.getInteger(R.integer.pointerVisibleFor);

        pointerPointerCircleSize = res.getDimension(R.dimen.pointerPointerCircleSize);
        pointerPointerArrowLineWidth = res.getDimension(R.dimen.pointerPointerArrowLineWidth);
        pointerPointerArrowLength = res.getDimension(R.dimen.pointerPointerArrowLength);
        pointerPointerArrowHeadDepth = res.getDimension(R.dimen.pointerPointerArrowHeadDepth);
        pointerPointerArrowHeadHeight = res.getDimension(R.dimen.pointerPointerArrowHeadHeight);
        pointerPointerArrowHeadArmLength = Math.sqrt((pointerPointerArrowHeadDepth * pointerPointerArrowHeadDepth) + (pointerPointerArrowHeadHeight * pointerPointerArrowHeadHeight));
        pointerPointerXOffset = res.getDimension(R.dimen.pointerPointerXOffset);
        pointerPointerYOffset = (Style.pointerMaxSize - Style.pointerMinSize) / 2;

    }
}
