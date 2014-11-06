package com.twotoasters.chron.service;

import android.graphics.Canvas;
import android.os.Handler;
import android.os.Message;
import android.os.Trace;
import android.support.wearable.watchface.WatchFaceService;
import android.util.Log;
import android.view.Choreographer;
import android.view.SurfaceHolder;

/**
 * Base class for watch faces that draw on a {@link android.graphics.Canvas}. Provides an invalidate mechanism
 * similar to {@link android.view.View#invalidate}.
 */
public abstract class CanvasWatchFaceService extends WatchFaceService {
    private static final String TAG = "CanvasWatchFaceService";

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    public class Engine extends WatchFaceService.Engine {

        private static final int MSG_INVALIDATE = 0;

        private boolean mDrawRequested;
        private boolean mDestroyed;

        private Choreographer mChoreographer = Choreographer.getInstance();

        private final Choreographer.FrameCallback mFrameCallback =
                new Choreographer.FrameCallback() {
                    @Override
                    public void doFrame(long frameTimeNs) {
                        if (mDestroyed) {
                            return;
                        }
                        if (mDrawRequested) {
                            draw(getSurfaceHolder());
                        }
                    }
                };

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                switch (message.what) {
                    case MSG_INVALIDATE:
                        invalidate();
                        break;
                }
            }
        };

        @Override
        public void onDestroy() {
            mDestroyed = true;
            mHandler.removeMessages(MSG_INVALIDATE);
            mChoreographer.removeFrameCallback(mFrameCallback);
            super.onDestroy();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceChanged");
            }
            super.onSurfaceChanged(holder, format, width, height);
            invalidate();
        }

        @Override
        public void onSurfaceRedrawNeeded(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceRedrawNeeded");
            }
            super.onSurfaceRedrawNeeded(holder);
            draw(holder);  // Draw immediately.
        }

        @Override
        public void onSurfaceCreated(SurfaceHolder holder) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "onSurfaceCreated");
            }
            super.onSurfaceCreated(holder);
            invalidate();
        }

        /**
         * Schedules a call to {@link #onDraw} to draw the next frame. Must be called on the main
         * thread.
         */
        public void invalidate() {
            if (!mDrawRequested) {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "invalidate: requesting draw");
                }
                mDrawRequested = true;
                mChoreographer.postFrameCallback(mFrameCallback);
            } else {
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "invalidate: draw already requested");
                }
            }
        }

        /**
         * Posts a message to schedule a call to {@link #onDraw} to draw the next frame. Unlike
         * {@link #invalidate}, this method is thread-safe and may be called on any thread.
         */
        public void postInvalidate() {
            mHandler.sendEmptyMessage(MSG_INVALIDATE);
        }

        /**
         * Draws the watch face.
         *
         * @param canvas the canvas to draw into
         */
        public void onDraw(Canvas canvas) { }

        private void draw(SurfaceHolder holder) {
            mDrawRequested = false;
            Canvas canvas = holder.lockCanvas();
            if (canvas == null) {
                return;
            }
            try {
                Trace.beginSection("onDraw");
                if (Log.isLoggable(TAG, Log.VERBOSE)) {
                    Log.v(TAG, "drawing frame");
                }
                onDraw(canvas);
            } finally {
                Trace.endSection();
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }
}
