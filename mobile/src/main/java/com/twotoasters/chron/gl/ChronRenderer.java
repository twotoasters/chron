package com.twotoasters.chron.gl;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Paint;
import android.os.SystemClock;

import com.twotoasters.chron.R;
import com.twotoasters.chron.common.Utils;
import com.twotoasters.chron.gl.models.LabelMaker;
import com.twotoasters.chron.gl.models.NumericSprite;
import com.twotoasters.chron.gl.models.Square;
import com.twotoasters.chron.gl.utils.TextureUtils;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService.Renderer;

import java.text.SimpleDateFormat;
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

    private boolean active = true; // TODO: impl this

    private int mWidth;
    private int mHeight;

    private SimpleDateFormat sdfHour12, sdfHour24;
    private SimpleDateFormat sdfDate;

    private Paint hourPaint;
    private Paint minuteSecondPaint;
    private Paint datePaint;
    private Paint fpsPaint;

    private LabelMaker mLabels;
    private int mLabelDate;
    private int mLabelFps;
    private NumericSprite mHourSprite;
    private NumericSprite mMinuteSprite;
    private NumericSprite mSecondSprite;
    private NumericSprite mFpsSprite;

    private int mFrames;
    private int mFps;
    private final static int SAMPLE_PERIOD_FRAMES = 12;
    private final static float SAMPLE_FACTOR = 1.0f / SAMPLE_PERIOD_FRAMES;
    private long mStartTime;

    private Context appContext;
    private Resources res;
    private Square square;
    private Calendar time;

    private int hour, minute, second, millisecond;

    public ChronRenderer(Context context) {
        appContext = context.getApplicationContext();
        res = appContext.getResources();
        square = new Square(false);
        time = Calendar.getInstance();

        sdfHour12 = new SimpleDateFormat("hh");
        sdfHour24 = new SimpleDateFormat("HH");
        sdfDate = new SimpleDateFormat("MMM dd");

        minuteSecondPaint = new Paint();
        minuteSecondPaint.setAntiAlias(true);
        minuteSecondPaint.setTextSize(88);
        minuteSecondPaint.setTypeface(Utils.loadTypeface(appContext, com.twotoasters.chron.common.R.string.font_share_tech_mono_regular));

        hourPaint = new Paint(minuteSecondPaint);

        datePaint = new Paint(minuteSecondPaint);
        datePaint.setTextSize(48);

        fpsPaint = new Paint();
        fpsPaint.setAntiAlias(true);
        fpsPaint.setARGB(0xFF, 0xFF, 0xFF, 0xFF);
        fpsPaint.setTextSize(32);

        setColorResources();
    }

    private void setColorResources() {
        hourPaint.setColor(res.getColor(active ? com.twotoasters.chron.common.R.color.orange : android.R.color.white));
        minuteSecondPaint.setColor(res.getColor(active ? com.twotoasters.chron.common.R.color.teal : android.R.color.white));
        datePaint.setColor(res.getColor(active ? com.twotoasters.chron.common.R.color.teal : android.R.color.white));

        float shadowRadius = active ? 24f : 0f;
        hourPaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(com.twotoasters.chron.common.R.color.orange_shadow));
        minuteSecondPaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(com.twotoasters.chron.common.R.color.teal_shadow));
        datePaint.setShadowLayer(shadowRadius, 0, 0, res.getColor(com.twotoasters.chron.common.R.color.teal_shadow));
    }

    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();

        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        updateTime();
        drawSquareLayer(gl, BACKGROUND, 0);
        drawHands(gl);
        drawTextLabelsAndSprites(gl);
    }

    private void updateTime() {
        time.setTimeInMillis(System.currentTimeMillis());
        hour = time.get(Calendar.HOUR_OF_DAY) % 12;
        minute = time.get(Calendar.MINUTE);
        second = time.get(Calendar.SECOND);
        millisecond = time.get(Calendar.MILLISECOND);

        mHourSprite.setValue(sdfHour12.format(time.getTimeInMillis()));
        mMinuteSprite.setValue(Utils.twoDigitNum(minute));
        mSecondSprite.setValue(Utils.twoDigitNum(second));
    }

    private void drawHands(GL10 gl) {
        float hourRotation = 30 * (hour + (minute / 60f) + (second / 3600f));
        float minuteRotation = 6 * (minute + (second / 60f));
        float secondRotation = 6 * (second + (sweepSeconds ? (millisecond / 1000f) : 0));

        gl.glEnable(GL10.GL_BLEND);
        gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);

        drawSquareLayer(gl, HOUR, hourRotation);
        drawSquareLayer(gl, MINUTE, minuteRotation);
        drawSquareLayer(gl, SECOND, secondRotation);

        gl.glDisable(GL10.GL_BLEND);
    }

    private void drawSquareLayer(GL10 gl, int textureIndex, float rotationAngle) {
        gl.glPushMatrix();
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textureHandles[textureIndex]);
        gl.glRotatef(-rotationAngle, 0, 0, 1.0f);
        square.draw(gl);
        gl.glPopMatrix();
    }

    private void drawTextLabelsAndSprites(GL10 gl) {
        float cx = mWidth / 2f;
        float cy = mHeight / 2f;
        float fpsX = mWidth - mLabels.getWidth(mLabelFps) - 1;

        mLabels.beginDrawing(gl, mWidth, mHeight);
        drawLabel(gl, cx, cy - cx * (13f / 32f), mLabelDate);
        mLabels.draw(gl, fpsX, 112, mLabelFps);
        mLabels.endDrawing(gl);

        mHourSprite.draw(gl, cx * (9f / 16f) - mHourSprite.width() / 4f, cy, mWidth, mHeight);
        mMinuteSprite.draw(gl, cx - mMinuteSprite.width() / 4f, cy, mWidth, mHeight);
        mSecondSprite.draw(gl, cx * (23f / 16f) - mSecondSprite.width() / 4f, cy, mWidth, mHeight);

        drawFps(gl, fpsX);
    }

    private void drawLabel(GL10 gl, float x, float y, int labelId) {
        mLabels.draw(gl, x, y, labelId);
    }

    private void drawFps(GL10 gl, float rightMargin) {
        long time = SystemClock.uptimeMillis();
        if (mStartTime == 0) {
            mStartTime = time;
        }
        if (mFrames++ == SAMPLE_PERIOD_FRAMES) {
            mFrames = 0;
            long delta = time - mStartTime;
            mStartTime = time;
            mFps = (int) (1000 / (delta * SAMPLE_FACTOR));
        }
        if (mFps > 0) {
            mFpsSprite.setValue(Integer.toString(mFps));
            float numWidth = mFpsSprite.width();
            float x = rightMargin - numWidth * 2f;
            mFpsSprite.draw(gl, x, 112, mWidth, mHeight);
        }
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
        mWidth = w;
        mHeight = h;

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

        if (mLabels != null) {
            mLabels.shutdown(gl);
        } else {
            mLabels = new LabelMaker(true, 512, 128);
        }
        mLabels.initialize(gl);
        mLabels.beginAdding(gl);
        mLabelDate = mLabels.add(gl, sdfDate.format(time.getTimeInMillis()).toUpperCase(), datePaint);
        mLabelFps = mLabels.add(gl, "fps", fpsPaint);
        mLabels.endAdding(gl);

        if (mHourSprite != null) {
            mHourSprite.shutdown(gl);
        } else {
            mHourSprite = new NumericSprite();
        }
        mHourSprite.initialize(gl, hourPaint);

        if (mMinuteSprite != null) {
            mMinuteSprite.shutdown(gl);
        } else {
            mMinuteSprite = new NumericSprite();
        }
        mMinuteSprite.initialize(gl, minuteSecondPaint);

        if (mSecondSprite != null) {
            mSecondSprite.shutdown(gl);
        } else {
            mSecondSprite = new NumericSprite();
        }
        mSecondSprite.initialize(gl, minuteSecondPaint);

        if (mFpsSprite != null) {
            mFpsSprite.shutdown(gl);
        } else {
            mFpsSprite = new NumericSprite();
        }
        mFpsSprite.initialize(gl, fpsPaint);
    }

    public void release() {
        // no op
    }
}
