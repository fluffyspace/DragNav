package com.ingokodba.dragnav

/**
 * Shared preference keys used throughout the app
 * Extracted from the old MySettingsFragment for reusability
 */
object PreferenceKeys {
    const val UI_COLOR = "ui_color"
    const val UI_COLOR_ON_PRIMARY = "ui_color_on_primary"
    const val UI_SHADOW_TOGGLE = "ui_shadow_toggle"
    const val UI_BORDER_WIDTH = "ui_border_width"
    const val UI_TEXT_SIZE = "ui_text_size"
    const val UI_SMALLER_TEXT_SIZE = "ui_smaller_text_size"
    const val UI_TRANSPARENCY = "ui_transparency"
    const val UI_CIRCLES_TOGGLE = "ui_circles_toggle"
    const val UI_ICONS_TOGGLE = "ui_icons_toggle"
    const val UI_SHOW_APP_NAMES = "ui_show_app_names"
    const val UI_DESIGN = "ui_design"
    const val UI_BIG_CIRCLE = "ui_big_circle"
    const val UI_LANGUAGE_TOGGLE = "ui_language_toggle"
    const val UI_ONELINE = "ui_oneline_buttons_toggle"
    const val UI_BACKBUTTON = "ui_backbutton_toggle"
    const val DARK_MODE = "dark_mode"
    const val UI_RIGHT_HAND = "ui_right_hand"
    const val IMPORT = "import"
    const val EXPORT = "export"
    const val FEEDBACK = "feedback"
    const val RESTART = "restart"
    const val FROM_BACKUP = "from_backup"
    const val TO_BACKUP = "to_backup"
    const val RENEW_INSTANCE = "renew_instance"
    const val DROP_DATABASE = "drop_database"
    const val DEFAULT_APPS = "default_apps"
    const val NOTIFICATION_EXCLUDED_APPS = "notification_excluded_apps"
}

// Legacy alias for backward compatibility
@Deprecated("Use PreferenceKeys instead", ReplaceWith("PreferenceKeys"))
typealias MySettingsFragment = PreferenceKeys
