package com.quiet.launcher;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.provider.Settings;
import android.telecom.TelecomManager;
import java.util.Collections;

/** Shared rule checks; never blocks essential system escape routes. */
public final class GuardPolicy {
    public static boolean blocked(SharedPreferences p, String pkg, long now) {
        return p.getStringSet("guarded", Collections.emptySet()).contains(pkg)
            && (p.getLong("focusUntil",0)>now || p.getLong("break:"+pkg,0)>now);
    }
    public static boolean isEssential(Context c, String pkg) {
        if(pkg.equals(c.getPackageName()) || pkg.equals("android") || pkg.equals("com.android.systemui")
            || pkg.equals("com.android.settings") || pkg.contains("permissioncontroller")) return true;
        TelecomManager telecom=c.getSystemService(TelecomManager.class);
        if(telecom!=null && pkg.equals(telecom.getDefaultDialerPackage())) return true;
        Intent[] intents={new Intent(Settings.ACTION_SETTINGS),new Intent(Intent.ACTION_DIAL,Uri.parse("tel:")),
            new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)};
        for(Intent i:intents) {
            ResolveInfo r=c.getPackageManager().resolveActivity(i,0);
            if(r!=null && r.activityInfo!=null && pkg.equals(r.activityInfo.packageName)) return true;
        }
        return false;
    }
}
