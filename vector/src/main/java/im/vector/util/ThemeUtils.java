/*
 * Copyright 2017 OpenMarket Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.vector.util;


import android.app.Activity;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.IntDef;
import android.util.TypedValue;

import java.util.HashMap;
import java.util.Map;
import im.vector.R;

/**
 * Util class for managing themes.
 */
public class ThemeUtils {

    private static Integer currentTheme = null;

    @IntDef({THEME_DARK, THEME_LIGHT})
    public @interface ThemeList {}
    public static final int THEME_DARK = R.style.Theme_Vector_Dark;
    public static final int THEME_LIGHT = R.style.Theme_Vector_Light;

    public static void setTheme(String theme) {
        if (theme.equals("light")) {
            currentTheme = THEME_LIGHT;
        } else if (theme.equals("dark")) {
            currentTheme = THEME_DARK;
        }
    }

    public static void activitySetTheme(Activity a) {
        if (currentTheme != null) {
            a.setTheme(currentTheme);
        } else {
            // Just in case we had some problem anywhere reading, set the theme to light.
            a.setTheme(THEME_LIGHT);
        }
    }

    /**
     * Translates color attributes to colors
     * @param c Context
     * @param colorAttribute Color Attribute
     * @return Requested Color
     */
    public static @ColorInt int getColor(Context c, @AttrRes final int colorAttribute) {
        TypedValue color = new TypedValue();
        c.getTheme().resolveAttribute(colorAttribute, color, true);
        return color.data;
    }
}
