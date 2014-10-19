package com.twotoasters.chron.gl;

import android.content.Context;
import android.opengl.GLU;

import com.twotoasters.chron.R;
import com.twotoasters.chron.gl.models.Square;
import com.twotoasters.chron.gl.utils.TextureUtils;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService.Renderer;

import java.util.Calendar;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class InstrumentsRenderer implements Renderer {

    private static final int BACKGROUND = 0;
    private static final int HOUR = 1;
    private static final int MINUTE = 2;
    private static final int SECOND = 3;
    private static final int SHADOW = 4;
    private static final int FRAME = 5;
    private static final int GREEN = 6;

    private int[] textureHandles;

    private Context appContext;
    private Square square;
    private Calendar time;

    public InstrumentsRenderer(Context context) {
        appContext = context.getApplicationContext();
        square = new Square(true);
        time = Calendar.getInstance();
    }

    public void onDrawFrame(GL10 gl) {
        time.setTimeInMillis(System.currentTimeMillis());
        boolean sweepSeconds = true;

        int hour = time.get(Calendar.HOUR_OF_DAY) % 12;
        int minute = time.get(Calendar.MINUTE);
        int second = time.get(Calendar.SECOND);
        int millisecond = time.get(Calendar.MILLISECOND);

        float hourRotation = 30 * (hour + (minute / 60f) + (second / 3600f));
        float minuteRotation = 6 * (minute + (second / 60f));
        float secondRotation = 6 * (second + (sweepSeconds ? (millisecond / 1000f) : 0));

        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT | GL10.GL_STENCIL_BUFFER_BIT);

        /*
         * Now we're ready to draw some 3D objects
         */
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
        GLU.gluLookAt(gl, 0, 0, 5.35f, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        drawSquareLayer(gl, GREEN, 0);
        gl.glTranslatef(0, 0, 0.001f);

        drawSquareLayer(gl, FRAME, 0);
        gl.glTranslatef(0, 0, 0.0001f);

//        gl.glTranslatef(0, 0, 0.0001f);
//        drawSquareLayer(gl, BACKGROUND, 0);
//        gl.glTranslatef(0, 0, 0.0001f);
//        drawSquareLayer(gl, SHADOW, 0);



        gl.glTranslatef(0, 0, 0.1f);
        drawSquareLayer(gl, HOUR, hourRotation);
        gl.glTranslatef(0, 0, 0.1f);
        drawSquareLayer(gl, MINUTE, minuteRotation);
        gl.glTranslatef(0, 0, 0.1f);
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
        /*
         * By default, OpenGL enables features that improve quality
         * but reduce performance. One might want to tweak that
         * especially on software renderer.
         */
        gl.glDisable(GL10.GL_DITHER);

        /*
         * Some one-time OpenGL initialization can be made here
         * probably based on features of this particular context
         */
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_FASTEST);

        gl.glClearColor(1.0f, 1.0f, 1.0f, 1f);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glEnable(GL10.GL_TEXTURE_2D);

        // Added culling
        gl.glCullFace(GL10.GL_BACK);
        gl.glEnable(GL10.GL_CULL_FACE);

        // Added since watchface hands contain transparency
        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void onSurfaceChanged(GL10 gl, int w, int h) {
        gl.glViewport(0, 0, w, h);

        /*
         * Set our projection matrix. This doesn't have to be done
         * each time we draw, but usually a new projection needs to
         * be set when the viewport is resized.
         */
        float ratio = (float) w / h;
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        gl.glFrustumf(-ratio, ratio, -1, 1, 3, 7);

        final int[] texResIds = new int[]{
                R.raw.instru_watch_bg_normal, R.raw.instru_hand_hour_normal,
                R.raw.instru_hand_minute_normal, R.raw.instru_hand_second_normal,
                R.raw.instru_overlay_shadow_normal, R.raw.watch_frame,
                R.raw.green
        };
        textureHandles = TextureUtils.loadTextures(appContext, gl, texResIds);

        setupLight(gl);
    }

    private void setupLight(GL10 gl) {
        // Point Light
        float[] position = { 0, .8f, .5f, 1 };
        float[] ambient  = { .2f, .2f, .2f, 1 };
        float[] diffuse  = { .6f, .6f, .6f, 1f };
        float[] specular = { -.2f, -.2f, -.2f, 1 };

        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, position, 0);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT, ambient, 0);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_DIFFUSE, diffuse, 0);
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_SPECULAR, specular, 0);
    }

    /**
     * Called when the engine is destroyed. Do any necessary clean up because
     * at this point your renderer instance is now done for.
     */
    public void release() {
        // no op
    }
}
