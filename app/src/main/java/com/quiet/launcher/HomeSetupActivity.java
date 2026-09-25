package com.quiet.launcher;

import android.app.Activity;
import android.app.role.RoleManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.UserManager;
import android.provider.Settings;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** A standard activity owns the role request independently of the singleTask home. */
public final class HomeSetupActivity extends Activity {
    private TextView status;
    private final Handler handler=new Handler(Looper.getMainLooper());
    public static ResolveInfo resolvedHome(Context c) {
        return c.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
    }
    public static boolean isDefault(Context c) {
        ResolveInfo r=resolvedHome(c);
        if(r!=null && r.activityInfo!=null) {
            if(c.getPackageName().equals(r.activityInfo.packageName)) return true;
            // A concrete competing launcher takes precedence over a stale role flag.
            if(!r.activityInfo.name.contains("ResolverActivity")) return false;
        }
        RoleManager rm=c.getSystemService(RoleManager.class);
        return rm!=null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && rm.isRoleHeld(RoleManager.ROLE_HOME);
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll=new ScrollView(this);
        LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(Color.BLACK);scroll.addView(body);setContentView(scroll);
        getWindow().setDecorFitsSystemWindows(false);
        if (getWindow().getInsetsController()!=null)
            getWindow().getInsetsController().setSystemBarsAppearance(0,
                android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS |
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{
            Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            body.setPadding(dp(24)+i.left,dp(20)+i.top,dp(24)+i.right,dp(20)+i.bottom);return insets;
        });scroll.requestApplyInsets();
        text(body,"Set up your home screen",28);
        text(body,"Quiet Launcher 1.5",16);
        status=text(body,"Checking Android's current Home app…",19);
        text(body,"Choose Quiet Launcher in the Android dialog and confirm Set as default or Always. Then press your phone's Home button or use its Home gesture.",17);
        button(body,"Choose Quiet as default Home",this::requestHome);
        button(body,"Open Android Home settings",this::openHomeSettings);
        button(body,"Test Home button",()->{
            try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch(RuntimeException e) {status.setText("Android could not open Home. Use Home settings above.");}
        });
        text(body,"If Quiet is missing, disabled or the choice does not stick, copy the setup details below and send them with a screenshot of Android's Home app list. If installed in a work profile, install Quiet in your personal profile instead; managed-device policies may prevent changing Home.",16);
        button(body,"Copy setup details",()->{
            getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("Quiet home setup",diagnostics()));
            Toast.makeText(this,"Setup details copied",Toast.LENGTH_SHORT).show();
        });
        button(body,"Back to Quiet",this::finish);
    }
    private int dp(int x) {return Math.round(x*getResources().getDisplayMetrics().density);}
    private TextView text(LinearLayout body,String value,int size) {
        TextView v=new TextView(this);v.setText(value);v.setTextColor(Color.WHITE);v.setTextSize(size);v.setPadding(0,dp(12),0,dp(12));body.addView(v);return v;
    }
    private void button(LinearLayout body,String title,Runnable action) {
        Button b=new Button(this);b.setText(title);b.setAllCaps(false);b.setMinHeight(dp(56));b.setOnClickListener(v->action.run());body.addView(b);
    }
    @Override protected void onResume() {super.onResume();refresh();}
    private void refresh() {
        if(status==null) return;
        if(isDefault(this)) {
            getSharedPreferences("quiet",MODE_PRIVATE).edit().putBoolean("welcomed",true).apply();
            status.setText("Active: Quiet is your default Home app. Press Home to check it.");
        } else {
            ResolveInfo r=resolvedHome(this);
            String current=r==null||r.activityInfo==null||r.activityInfo.name.contains("ResolverActivity")?"Not selected":r.loadLabel(getPackageManager()).toString();
            status.setText("Current Home app: "+current+"\nQuiet has not been selected yet.");
        }
    }
    private void requestHome() {
        UserManager um=getSystemService(UserManager.class);
        if(um!=null && um.isManagedProfile()) {status.setText("Home cannot be changed from a work profile. Open the personal-profile installation of Quiet.");return;}
        RoleManager rm=getSystemService(RoleManager.class);
        if(isDefault(this)) {refresh();return;}
        try {
            if(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME),100);return;
            }
        } catch(RuntimeException e) {status.setText("Android's chooser was unavailable. Try Home settings.");}
        openHomeSettings();
    }
    private void openHomeSettings() {
        for(String action:new String[]{Settings.ACTION_HOME_SETTINGS,Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS,Settings.ACTION_SETTINGS}) {
            try {startActivity(new Intent(action));return;} catch(RuntimeException ignored) { }
        }
        status.setText("Open your phone's Settings manually, then search for Default apps or Home app.");
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==100) {refresh();handler.postDelayed(this::refresh,1000);}
    }
    @Override protected void onDestroy() {handler.removeCallbacksAndMessages(null);super.onDestroy();}
    private String diagnostics() {
        StringBuilder out=new StringBuilder("Quiet 1.5 Home setup\n");
        out.append("Phone: ").append(Build.MANUFACTURER).append(' ').append(Build.MODEL)
            .append("\nAndroid: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")");
        UserManager um=getSystemService(UserManager.class);
        out.append("\nWork profile: ").append(um!=null && um.isManagedProfile());
        RoleManager rm=getSystemService(RoleManager.class);
        out.append("\nHOME role available: ").append(rm!=null && rm.isRoleAvailable(RoleManager.ROLE_HOME));
        out.append("\nHOME role held: ").append(rm!=null && rm.isRoleHeld(RoleManager.ROLE_HOME));
        ResolveInfo home=resolvedHome(this);
        out.append("\nResolved Home: ").append(home==null||home.activityInfo==null?"none":home.activityInfo.packageName+"/"+home.activityInfo.name);
        out.append("\nHOME candidates:");
        for(ResolveInfo r:getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY))
            if(r.activityInfo!=null) out.append("\n").append(r.activityInfo.packageName).append('/').append(r.activityInfo.name);
        return out.toString();
    }
}
