package com.twotoasters.chron.gl;

import android.content.Context;

import com.twotoasters.chron.R;
import com.twotoasters.chron.gl.models.Square;
import com.twotoasters.chron.gl.utils.TextureUtils;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService.Renderer;

import java.util.Calendar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class ChronRenderer implements Renderer {

    private static final int BACKGROUND = 0;
    private static final int HOUR = 1;
    private static final int MINUTE = 2;
    private static final int SECOND = 3;

    boolean sweepSeconds = true;

    private int[] textureHandles;

    private Context appContext;
    private Square square;
    private Calendar time;

    public ChronRenderer(Context context) {
        appContext = context.getApplicationContext();
        square = new Square(false);
        time = Calendar.getInstance();
    }

    public void onDrawFrame(GL10 gl) {
        time.setTimeInMillis(System.currentTimeMillis());

        int hour = time.get(Calendar.HOUR_OF_DAY) % 12;
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        int millisecond = time.get(Calendar.MILLISECOND);

        float hourRotation = 30 * (hour + (minute / 60f) + (second / 3600f));
        float minuteRotation = 6 * (minute + (second / 60f));
        float secondRotation = 6 * (second + (sweepSeconds ? (millisecond / 1000f) : 0));

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        drawSquareLayer(gl, BACKGROUND, 0);
        drawSquareLayer(gl, HOUR, hourRotation);
        drawSquareLayer(gl, MINUTE, minuteRotation);
        drawSquareLayer(gl, SECOND, secondRotation);
    }

    private void drawSquareLayer(GL10 gl, int textureIndex, float rotationAngle) {
        gl.glPushMatrix();
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureHandles[textureIndex]);
        gl.glRotatef(-rotationAngle, 0, 0, 1.0f);
        square.draw(gl);
        gl.glPopMatrix();
    }

    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        // By default, OpenGL enables features that improve quality but reduce performance. Need to tweak that on software renderer.
        gl.glDisable(GL10.GL_DITHER);

        gl.glClearColor(0.0f, 0.0f, 0.0f, 1f);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glDisable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        // Added since watchface hands contain transparency
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);

        // Set our projection matrix. Usually a new projection needs to be set when the viewport is resized
        float ratio = (float) w / h;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glOrthof(-1, 1, -1/ratio, 1/ratio, -.0001f, .0001f);

        final int[] texResIds = new int[]{
                R.drawable.watch_bg_normal, R.drawable.hand_hour_normal,
                R.drawable.hand_minute_normal, R.drawable.hand_second_normal
        };
        textureHandles = TextureUtils.loadTextures(appContext, gl, texResIds);
    }

    public void release() {
        // no op
    }
}
