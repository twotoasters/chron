package com.twotoasters.chron;

import com.twotoasters.chron.gl.InstrumentsRenderer;

import net.rbgrn.android.glwallpaperservice.GLWallpaperService;

public class InstrumentsLiveWallpaperGL extends GLWallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new InstrumentsEngine();
    }

    class InstrumentsEngine extends GLEngine {

        private InstrumentsRenderer renderer;

        InstrumentsEngine() {
            renderer = new InstrumentsRenderer(getApplicationContext());
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
