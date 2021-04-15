package com.reactnativeshortcut;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ShortCutModule extends ReactContextBaseJavaModule {
    private final String SHORTCUT_NOT_EXIST = "SHORTCUT_NOT_EXIST";
    private final String DEFAULT_ACTIVITY = "MainActivity";
    private final String ID_KEY = "id";
    private final String SHORT_LABEL_KEY = "shortLabel";
    private final String LONG_LABEL_KEY = "longLabel";
    private final String ICON_FOLDER_KEY = "iconFolderName";
    private final String ICON_NAME_KEY = "iconName";
    private final String ACTIVITY_NAME_KEY = "activityName";
    private final String LABEL_NAME = "name";
    private final String MESSAGE_TOAST = "message";

    ShortCutModule(ReactApplicationContext context) {
        super(context);
    }

    @NonNull
    @Override
    public String getName() {
        return "ShortCutModule";
    }

    @ReactMethod
    public void createShortCut(ReadableMap shortcutDetail) {
        int iconId = getReactApplicationContext().getResources().getIdentifier(shortcutDetail.getString(ICON_NAME_KEY), shortcutDetail.getString(ICON_FOLDER_KEY), getReactApplicationContext().getPackageName());
        // Checking if ShortCut was already added
        SharedPreferences sharedPreferences = getReactApplicationContext().getSharedPreferences(shortcutDetail.getString(LABEL_NAME), getReactApplicationContext().MODE_PRIVATE);
//        boolean shortCutWasAlreadyAdded = sharedPreferences.getBoolean(name, false);
//        if (shortCutWasAlreadyAdded) return;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            final Intent shortcutIntent = new Intent(getReactApplicationContext(), MainActivity.class);
            shortcutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            shortcutIntent.putExtra("shortcutKey", shortcutDetail.getString(LABEL_NAME));
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            final Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutDetail.getString(LABEL_NAME));
            intent.putExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID, shortcutDetail.getString(ID_KEY));
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getReactApplicationContext(), iconId));
            intent.putExtra("duplicate", false);
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            getReactApplicationContext().sendBroadcast(intent);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                addShortCutBigAndroid(shortcutIntent, shortcutDetail);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                ShortcutInfoCompat webShortcut = addShortCutBigAndroid(new Intent(Intent.ACTION_MAIN, Uri.EMPTY, getReactApplicationContext(), MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra("shortcutKey", shortcutDetail.getString(LABEL_NAME)), shortcutDetail);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && ShortcutManagerCompat.isRequestPinShortcutSupported(getReactApplicationContext())) {
                    ShortcutManagerCompat.requestPinShortcut(getReactApplicationContext(), webShortcut, null);
                }
            }
        }
        Toast toast = Toast.makeText(getReactApplicationContext(), shortcutDetail.getString(MESSAGE_TOAST), Toast.LENGTH_SHORT);
        toast.show();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(shortcutDetail.getString(LABEL_NAME), true);
        editor.commit();
    }

    public ShortcutInfoCompat addShortCutBigAndroid(Intent shortcutIntent, ReadableMap shortcutDetail) {
        int iconId = getReactApplicationContext().getResources().getIdentifier(shortcutDetail.getString(ICON_NAME_KEY), shortcutDetail.getString(ICON_FOLDER_KEY), getReactApplicationContext().getPackageName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutInfoCompat webShortcut = null;
            webShortcut = new ShortcutInfoCompat.Builder(getReactApplicationContext(), "shortcut_bluzone")
                    .setShortLabel( shortcutDetail.getString(LABEL_NAME))
                    .setLongLabel( shortcutDetail.getString(LABEL_NAME))
                    .setIcon(IconCompat.createWithResource(getReactApplicationContext(), iconId))
                    .setIntent(shortcutIntent)
                    .build();
            ShortcutManagerCompat.addDynamicShortcuts(getReactApplicationContext(), Collections.singletonList(webShortcut));

            return webShortcut;
        }
        return null;
    }

    @ReactMethod
    public void getCurrentShortcut(Callback callback) {
        String shortcutUrl = getReactApplicationContext().getCurrentActivity().getIntent().getStringExtra("shortcutKey");
        callback.invoke(shortcutUrl);
    }

    @ReactMethod
    public void getShortcutExist(Callback callback) {
        List<ShortcutInfoCompat> shortcutInfoCompats = ShortcutManagerCompat.getDynamicShortcuts(getReactApplicationContext());
        System.out.println("shortcutInfoCompats = " + shortcutInfoCompats);
        callback.invoke(shortcutInfoCompats.toString());
    }

    @ReactMethod
    public void getAllShortcut(Callback onDone) {
        if (Build.VERSION.SDK_INT < 26) {
            List<ShortcutInfoCompat> shortcutInfoCompats = ShortcutManagerCompat.getDynamicShortcuts(getReactApplicationContext());
            onDone.invoke(shortcutInfoCompats.size());
        } else {
            List<ShortcutInfo> shortcuts = getReactApplicationContext().getSystemService(ShortcutManager.class)
                    .getPinnedShortcuts();
            onDone.invoke(shortcuts.size());
        }
    }
}
