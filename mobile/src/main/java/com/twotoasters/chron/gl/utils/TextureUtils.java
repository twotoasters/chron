package com.twotoasters.chron.gl.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLUtils;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.khronos.opengles.GL10;

public class TextureUtils {

    public static int loadRawTexture(@NonNull Context context, @NonNull GL10 gl, @RawRes int textureResIds) {
        int[] textureHandles = loadTextures(context, gl, new int[]{textureResIds});
        return textureHandles[0];
    }

    public static int[] loadRawTextures(@NonNull Context context, @NonNull GL10 gl, @RawRes int[] textureResIds) {
        int[] textureHandles = new int[textureResIds.length];
        gl.glGenTextures(textureHandles.length, textureHandles, 0);

        for (int i = 0; i < textureHandles.length; i++) {
            // Create our texture. This has to be done each time the surface is created.
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureHandles[i]);

            // Set filtering
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

            // Load raw bitmap
            InputStream is = context.getResources().openRawResource(textureResIds[i]);
            Bitmap bitmap;
            try {
                bitmap = BitmapFactory.decodeStream(is);
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    // no op
                }
            }

            // Load bitmap into bound texture
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        return textureHandles;
    }

    public static int loadTexture(@NonNull Context context, @NonNull GL10 gl, @DrawableRes int textureResIds) {
        int[] textureHandles = loadTextures(context, gl, new int[]{textureResIds});
        return textureHandles[0];
    }

    public static int[] loadTextures(@NonNull Context context, @NonNull GL10 gl, @DrawableRes int[] textureResIds) {
        int[] textureHandles = new int[textureResIds.length];
        gl.glGenTextures(textureHandles.length, textureHandles, 0);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        for (int i = 0; i < textureHandles.length; i++) {
            // Create our texture. This has to be done each time the surface is created.
            gl.glBindTexture(GL10.GL_TEXTURE_2D, textureHandles[i]);

            // Set filtering
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_NEAREST);
            gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

            // Load resource bitmap
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), textureResIds[i], options);

            // Load bitmap into bound texture
            GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
            bitmap.recycle();
        }
        return textureHandles;
    }
}
