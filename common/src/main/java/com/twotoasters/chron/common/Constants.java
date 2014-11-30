package com.twotoasters.chron.common;

import android.support.annotation.StringRes;

public class Constants {
    public static final String SCHEME_WEAR = "wear";

    // Message and Data paths
    public static final String CONFIG_PATH = "/config";

    // Bundle and DataMap keys
    public static final String KEY_PRIMARY_COLOR = "PRIMARY_COLOR";
    public static final String KEY_ACCENT_COLOR = "ACCENT_COLOR";

    @StringRes public static final int DEFAULT_PRIMARY_COLOR_NAME = R.string.color_name_cyan;
    @StringRes public static final int DEFAULT_ACCENT_COLOR_NAME = R.string.color_name_orange;
}
