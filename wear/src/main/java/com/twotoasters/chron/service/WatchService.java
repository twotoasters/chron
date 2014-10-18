package com.twotoasters.chron.service;

import android.graphics.Canvas;
import android.service.wallpaper.WallpaperService;
import android.support.wearable.watchface.WatchFaceService;

import com.twotoasters.chron.common.ChronWatch;

public class WatchService extends WatchFaceService {

    @Override
    public WallpaperService.Engine onCreateEngine() {
        return new WatchfaceEngine();
    }

    class WatchfaceEngine extends Engine {

        private static final boolean IS_24_HOUR = false; // TODO: accomodate the setting

        private ChronWatch chronWatch;

        WatchfaceEngine() {
            chronWatch = new ChronWatch(getApplicationContext());
        }

        @Override
        public void onTimeTick() {
            Canvas canvas = getSurfaceHolder().lockCanvas();
            if (canvas == null) {
                return;
            }

            try {
                chronWatch.setTime(System.currentTimeMillis()); // TODO: set this better
                chronWatch.draw(canvas);
            } finally {
                getSurfaceHolder().unlockCanvasAndPost(canvas);
            }
        }
    }
}
