package com.quiet.launcher;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;
import java.util.Collections;

/** Observes package switches only. Screen-content retrieval is disabled in XML. */
public final class GuardService extends AccessibilityService {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private String foreground="";
    private long entered, lastHome;
    private final Runnable tick=new Runnable() {
        @Override public void run() { evaluate(); handler.postDelayed(this,1000); }
    };
    @Override protected void onServiceConnected() {
        prefs=getSharedPreferences("quiet",MODE_PRIVATE);
        handler.removeCallbacks(tick); handler.post(tick);
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getPackageName()==null) return;
        String pkg=event.getPackageName().toString();
        // A keyboard or system overlay is not a switch to another content app.
        String ime=Settings.Secure.getString(getContentResolver(),Settings.Secure.DEFAULT_INPUT_METHOD);
        ComponentName keyboard=ime==null?null:ComponentName.unflattenFromString(ime);
        if(pkg.equals("com.android.systemui") || (keyboard!=null && pkg.equals(keyboard.getPackageName()))) return;
        if(!pkg.equals(foreground)) { foreground=pkg; entered=SystemClock.elapsedRealtime(); }
        evaluate();
    }
    private void evaluate() {
        if(prefs==null || foreground.isEmpty()) return;
        PowerManager power=getSystemService(PowerManager.class);
        KeyguardManager keyguard=getSystemService(KeyguardManager.class);
        if((power!=null && !power.isInteractive()) || (keyguard!=null && keyguard.isKeyguardLocked())) {
            foreground=""; return;
        }
        String pkg=foreground;
        if(!prefs.getStringSet("guarded",Collections.emptySet()).contains(pkg) || GuardPolicy.isEssential(this,pkg)) return;
        long now=System.currentTimeMillis(), elapsed=SystemClock.elapsedRealtime();
        boolean timedOut=elapsed-entered>=Math.max(2,Math.min(15,prefs.getInt("sessionMinutes",5)))*60000L;
        if(!GuardPolicy.blocked(prefs,pkg,now) && !timedOut) return;
        if(elapsed-lastHome<3000) return;
        lastHome=elapsed;
        if(performGlobalAction(GLOBAL_ACTION_HOME)) {
            if(timedOut) prefs.edit().putLong("break:"+pkg,now+60000L).apply();
            Toast.makeText(this,prefs.getLong("focusUntil",0)>now?"Focus time: this app is paused.":"Time for a one-minute break from scrolling.",Toast.LENGTH_LONG).show();
            foreground="";
        }
    }
    @Override public void onInterrupt() { foreground=""; }
    @Override public void onDestroy() { handler.removeCallbacksAndMessages(null); super.onDestroy(); }
    public static boolean isEnabled(Context c) {
        AccessibilityManager manager=c.getSystemService(AccessibilityManager.class);
        if(manager==null) return false;
        for(AccessibilityServiceInfo i:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)) {
            android.content.pm.ServiceInfo s=i.getResolveInfo().serviceInfo;
            if(s.packageName.equals(c.getPackageName()) && s.name.equals(GuardService.class.getName())) return true;
        }
        return false;
    }
}
