package com.twotoasters.chron.common;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.DisplayMetrics;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class Utils {

    public static Map<String, Integer> colorMap;

    // Loads a bitmap scaled according to its aspect ratio so that its largest dimension fits within the provided container width
    public static Bitmap loadScaledBitmapRes(Resources res, @DrawableRes int resId, float containerWidth, float containerHeight) {
        BitmapFactory.Options options = new Options();
        options.inScaled = false;
        Bitmap bitmap = BitmapFactory.decodeResource(res, resId, options);

        if ((bitmap.getWidth() <= Math.round(containerWidth) && bitmap.getHeight() <= Math.round(containerHeight))
                && (bitmap.getWidth() == containerWidth || bitmap.getHeight() == containerHeight)) {
            return bitmap;
        }

        float xScale = containerWidth / (float) bitmap.getWidth();
        float yScale = containerHeight / (float) bitmap.getHeight();
        float scale = Math.min(xScale, yScale);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap createColorizedImage(Bitmap grayscale, int color) {
        Paint paint = new Paint(color);
        //ColorFilter filter = new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN);
        ColorFilter filter = new LightingColorFilter(color, 1);
        paint.setColorFilter(filter);

        Bitmap bitmap = Bitmap.createBitmap(grayscale.getWidth(), grayscale.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(grayscale, 0, 0, paint);
        return bitmap;
    }

    @NonNull
    public static int[] getScreenDimensPx(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return new int[]{ displayMetrics.widthPixels, displayMetrics.heightPixels };
    }

    public static Typeface loadTypeface(Context context, @StringRes int typefaceNameResId) {
        String typefaceName = context.getResources().getString(typefaceNameResId);
        return Typeface.createFromAsset(context.getAssets(), typefaceName);
    }

    public static int colorForName(Context context, String colorName) {
        Integer bgColor = getColorMap(context).get(colorName);
        if (bgColor != null) {
            return bgColor;
        } else {
            Timber.w("Invalid background color name, using default");
            return context.getResources().getColor(R.color.color_cyan);
        }
    }

    private static Map<String, Integer> getColorMap(Context context) {
        if (colorMap == null) {
            String[] colorNames = context.getResources().getStringArray(R.array.color_name_array);
            TypedArray ta = context.getResources().obtainTypedArray(R.array.color_array);

            if (colorNames.length != ta.length()) {
                throw new AssertionError("Background color and color name arrays must have the same length");
            }

            colorMap = new HashMap<>();
            for (int i = 0; i < ta.length(); i++) {
                int color = ta.getColor(i, 0);
                colorMap.put(colorNames[i], color);
            }
            ta.recycle();
        }
        return colorMap;
    }

    public static String formatTwoDigitNum(int num) {
        return String.format("%02d", num);
    }

    public static String toTimeText(long timestampMillis, boolean is24HourMode) {
        String formatString = is24HourMode ? "H:mm" : "h:mma";
        String timeText = new SimpleDateFormat(formatString).format(timestampMillis);
        if (!is24HourMode) {
            timeText = timeText.toLowerCase().substring(0, timeText.length() - 1);
        }
        return timeText;
    }

    public static String debugTime(long timestampMillis) {
        return new SimpleDateFormat("hh:mm:ss").format(timestampMillis);
    }

    public static float degreesToRadians(float degrees) {
        return degrees * (float) Math.PI / 180f;
    }

    public static float radiansToDegrees(float radians) {
        return radians * 180f / (float) Math.PI;
    }

    // TODO: need to use still?
    @NonNull
    public static float[] getScreenDimensDp(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        return new float[]{ dpWidth, dpHeight };
    }

    private Utils() { }
}
