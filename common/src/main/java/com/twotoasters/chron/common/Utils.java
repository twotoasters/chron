package com.twotoasters.chron.common;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.util.DisplayMetrics;
import android.util.Log;

import java.util.ArrayList;

public class Utils {

    private static final String TAG = "Chron";
    private static final ArrayList<String> AMOLED_MODELS;
    static {
        AMOLED_MODELS = new ArrayList<>();
        AMOLED_MODELS.add("Gear Live");
    }

    public static boolean hasAmoledScreen() {
        return AMOLED_MODELS.contains(Build.MODEL);
    }

    public static Bitmap loadScaledBitmapRes(Resources res, @DrawableRes int resId, int containerWidth, int containerHeight) {
        Bitmap bitmap = BitmapFactory.decodeResource(res, resId);
        if (containerWidth == bitmap.getWidth() && containerHeight == bitmap.getHeight()) {
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
        ColorFilter filter = new LightingColorFilter(color, 1);
        paint.setColorFilter(filter);

        Bitmap bitmap = Bitmap.createBitmap(grayscale.getWidth(), grayscale.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(grayscale, 0, 0, paint);
        return bitmap;
    }

    @NonNull
    public static float[] getScreenDimensDp(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        return new float[]{ dpWidth, dpHeight };
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

    public static String twoDigitNum(int num) {
        return String.format("%02d", num);
    }

    public static void logd(String text, Object... objects) {
        Log.d(TAG, String.format(text, objects));
    }

    private static void assertTrue(boolean condition, String errorMessage) {
        if (!condition) throw new AssertionError(errorMessage);
    }

    private Utils() { }
}
