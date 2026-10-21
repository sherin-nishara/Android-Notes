package com.example.floatingnote

import android.content.Context
import android.content.SharedPreferences

object NoteRepository {
    private const val PREFS = "note"
    private const val KEY_TEXT = "text"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"
    private const val KEY_W = "w"
    private const val KEY_H = "h"
    private const val KEY_DARK = "dark"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadText(ctx: Context): String = prefs(ctx).getString(KEY_TEXT, "") ?: ""
    fun saveText(ctx: Context, text: String) = prefs(ctx).edit().putString(KEY_TEXT, text).apply()

    fun loadX(ctx: Context): Int = prefs(ctx).getInt(KEY_X, 100)
    fun loadY(ctx: Context): Int = prefs(ctx).getInt(KEY_Y, 200)
    fun savePos(ctx: Context, x: Int, y: Int) = prefs(ctx).edit().putInt(KEY_X, x).putInt(KEY_Y, y).apply()

    fun loadW(ctx: Context): Int = prefs(ctx).getInt(KEY_W, 320)
    fun loadH(ctx: Context): Int = prefs(ctx).getInt(KEY_H, 200)
    fun saveSize(ctx: Context, w: Int, h: Int) = prefs(ctx).edit().putInt(KEY_W, w).putInt(KEY_H, h).apply()

    fun isDark(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_DARK, false)
    fun setDark(ctx: Context, dark: Boolean) = prefs(ctx).edit().putBoolean(KEY_DARK, dark).apply()
}
