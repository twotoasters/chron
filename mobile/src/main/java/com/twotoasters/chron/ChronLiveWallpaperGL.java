package com.twotoasters.chron;

import com.twotoasters.chron.gl.ChronRenderer;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

public class ChronLiveWallpaperGL extends GLWallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new ChronEngine();
    }

    class ChronEngine extends GLEngine {

        private ChronRenderer renderer;

        ChronEngine() {
            renderer = new ChronRenderer(getApplicationContext());
            setRenderer(renderer);
            setRenderMode(RENDERMODE_CONTINUOUSLY);
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (renderer != null) {
                renderer.release();
            }
            renderer = null;
        }
    }
}
