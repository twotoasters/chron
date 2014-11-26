package com.twotoasters.chron.service;

import android.graphics.Canvas;
import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

import com.twotoasters.chron.common.ChronWatch;

import hugo.weaving.DebugLog;

public class ChronLiveWallpaper extends WallpaperService {

    @DebugLog
    @Override
    public Engine onCreateEngine() {
        return new ChronEngine();
    }

    class ChronEngine extends Engine {

        private static final long UPDATE_RATE_MS = 33;

        private final ChronWatch chronWatch;
        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                onTimeTick();
            }

        };

        @DebugLog
        ChronEngine() {
            chronWatch = new ChronWatch(getApplicationContext(), UPDATE_RATE_MS < 1000);
            handler.post(drawRunner);
        }

        @DebugLog
        @Override
        public void onVisibilityChanged(boolean visible) {
            chronWatch.setVisibility(visible);
            if (visible) {
                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @DebugLog
        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            chronWatch.setVisibility(false);
            handler.removeCallbacks(drawRunner);
        }

        @DebugLog
        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            chronWatch.setSize(width, height);
            super.onSurfaceChanged(holder, format, width, height);
        }

        public void onTimeTick() {
            Canvas canvas = getSurfaceHolder().lockCanvas();
            if (canvas == null) {
                return;
            }

            try {
                chronWatch.setTime(System.currentTimeMillis());
                chronWatch.draw(canvas);
            } finally {
                getSurfaceHolder().unlockCanvasAndPost(canvas);
            }

            handler.removeCallbacks(drawRunner);

            if (chronWatch.isVisible()) {
                handler.postDelayed(drawRunner, UPDATE_RATE_MS);
            }
        }
    }
}
