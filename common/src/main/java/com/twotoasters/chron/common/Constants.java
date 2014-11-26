package com.twotoasters.chron.common;

import android.graphics.Color;

import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final String SCHEME_WEAR = "wear";
    public static final String PATH_WITH_FEATURE = "/watch_face_config/Chron";
    public static final String KEY_PRIMARY_COLOR = "PRIMARY_COLOR";
    public static final String KEY_ACCENT_COLOR = "ACCENT_COLOR";
    public static final int DEFAULT_PRIMARY_COLOR = Color.parseColor("#04ffff"); // cyan
    public static final int DEFAULT_ACCENT_COLOR = Color.parseColor("#ffbe35"); // orange

    public static final Map<String, Integer> COLOR_MAP;

    static {
        COLOR_MAP = new HashMap<>();
        COLOR_MAP.put("Red", Color.RED);
        COLOR_MAP.put("Orange", Color.parseColor("#ffbe35"));
        COLOR_MAP.put("Yellow", Color.YELLOW);
        COLOR_MAP.put("Green", Color.GREEN);
        COLOR_MAP.put("Cyan", Color.parseColor("#04ffff"));
        COLOR_MAP.put("Blue", Color.BLUE);
        COLOR_MAP.put("Navy", Color.parseColor("#000080"));
        COLOR_MAP.put("Purple", Color.parseColor("#8000FF"));
        COLOR_MAP.put("Black", Color.BLACK);
        COLOR_MAP.put("Gray", Color.GRAY);
        COLOR_MAP.put("White", Color.WHITE);
    }

    public static int colorForName(String colorName) {
        Integer color = COLOR_MAP.get(colorName);
        if (color != null) {
            return color;
        } else {
            throw new IllegalArgumentException("Invalid color name");
        }
    }
}
