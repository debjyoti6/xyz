package com.quiet.launcher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.view.View;
import android.widget.ListView;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Collections;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/** Acceptance checks for launcher registration, preferences and focus behavior. */
@RunWith(AndroidJUnit4.class)
public class LauncherUiTest {
    private ActivityScenario<MainActivity> scenario;
    private SharedPreferences prefs;
    private String sampleId;

    @Before public void setup() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefs = context.getSharedPreferences("quiet", Context.MODE_PRIVATE);
        Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        for (ResolveInfo info: context.getPackageManager().queryIntentActivities(query,0)) {
            if (info.activityInfo != null && info.activityInfo.exported &&
                    !info.activityInfo.packageName.equals(context.getPackageName()) &&
                    !GuardPolicy.isEssential(context,info.activityInfo.packageName)) {
                sampleId = new ComponentName(info.activityInfo.packageName,info.activityInfo.name).flattenToString();
                break;
            }
        }
        assertNotNull("Emulator must contain a launchable app",sampleId);
        assertTrue(prefs.edit().clear().putBoolean("welcomed",true).putString("name:"+sampleId,"Test Essential").commit());
    }
    @After public void cleanup() {
        if(scenario!=null) scenario.close();
        prefs.edit().clear().commit();
    }
    private void openSampleList() {
        scenario = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.all_apps)).perform(scrollTo(),click());
        onView(withId(R.id.search_apps)).perform(replaceText("Test Essential"),closeSoftKeyboard());
        onView(isRoot()).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return isRoot(); }
            public String getDescription() { return "wait up to 8 seconds for asynchronous app discovery"; }
            public void perform(UiController ui, View root) {
                long deadline = android.os.SystemClock.uptimeMillis()+8000;
                do {
                    ListView list=root.findViewById(R.id.app_list);
                    if(list!=null && list.getAdapter()!=null && list.getAdapter().getCount()==1) return;
                    ui.loopMainThreadForAtLeast(50);
                } while(android.os.SystemClock.uptimeMillis()<deadline);
                throw new AssertionError("Expected one matching app after discovery");
            }
        });
    }
    @Test public void favoriteSurvivesActivityRecreation() {
        openSampleList();
        onData(anything()).inAdapterView(withId(R.id.app_list)).atPosition(0).perform(longClick());
        onView(withText("Add to favorites")).perform(click());
        // Parse JSON: Android may escape the slash in a component name.
        try { assertEquals(sampleId,new org.json.JSONArray(prefs.getString("favorites","[]")).getString(0)); }
        catch(org.json.JSONException e) { throw new AssertionError(e); }
        pressBack(); scenario.recreate();
        long deadline=android.os.SystemClock.uptimeMillis()+8000;
        boolean[] found={false};
        do {
            scenario.onActivity(activity->{
                for(View v:androidx.test.espresso.util.TreeIterables.breadthFirstViewTraversal(activity.getWindow().getDecorView()))
                    if("Test Essential, favorite".contentEquals(v.getContentDescription()==null?"":v.getContentDescription())) found[0]=true;
            });
            if(found[0]) break;
            android.os.SystemClock.sleep(50);
        } while(android.os.SystemClock.uptimeMillis()<deadline);
        assertTrue("Favorite must survive activity recreation",found[0]);
        onView(withContentDescription("Test Essential, favorite")).check(matches(isDisplayed()));
    }
    @Test public void hiddenAppCanBeRestoredThroughSettings() {
        openSampleList();
        onData(anything()).inAdapterView(withId(R.id.app_list)).atPosition(0).perform(longClick());
        onView(withText("Hide from Quiet")).perform(click());
        onView(withText("Hide")).perform(click());
        assertTrue(prefs.getStringSet("hidden",Collections.emptySet()).contains(sampleId));
        onView(withId(R.id.empty_apps)).check(matches(isDisplayed()));
        pressBack();
        onView(withId(R.id.preferences)).perform(scrollTo(),click());
        onView(withId(R.id.hidden_apps)).perform(scrollTo(),click());
        onView(withText("Test Essential")).perform(click());
        assertFalse(prefs.getStringSet("hidden",Collections.emptySet()).contains(sampleId));
    }
    @Test public void pauseCannotOpenImmediatelyAndCancelsInBackground() {
        prefs.edit().putStringSet("paused",Collections.singleton(sampleId)).putInt("pauseSeconds",15).commit();
        openSampleList();
        onData(anything()).inAdapterView(withId(R.id.app_list)).atPosition(0).perform(click());
        onView(withId(android.R.id.button1)).check(matches(not(isEnabled())));
        scenario.moveToState(Lifecycle.State.STARTED);
        scenario.moveToState(Lifecycle.State.RESUMED);
        onView(withText("Take a breath.")).check(doesNotExist());
        onView(withId(R.id.search_apps)).check(matches(isDisplayed()));
    }
    @Test public void renameChangesDisplayAndHandlesEmptySearch() {
        openSampleList();
        onData(anything()).inAdapterView(withId(R.id.app_list)).atPosition(0).perform(longClick());
        onView(withText("Rename in Quiet")).perform(click());
        onView(withContentDescription("App display name")).perform(replaceText("Daily tool"),closeSoftKeyboard());
        onView(withText("Save")).perform(click());
        assertEquals("Daily tool",prefs.getString("name:"+sampleId,""));
        onView(withId(R.id.search_apps)).perform(replaceText("Daily tool"),closeSoftKeyboard());
        onData(anything()).inAdapterView(withId(R.id.app_list)).atPosition(0).check(matches(withText("Daily tool")));
        onView(withId(R.id.search_apps)).perform(replaceText("zzzz-unmatched-app"),closeSoftKeyboard());
        onView(withId(R.id.empty_apps)).check(matches(isDisplayed()));
    }
    @Test public void registeredAsHomeAndRoleRequestResolves() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        Intent home=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        boolean found=false;
        for(ResolveInfo r:c.getPackageManager().queryIntentActivities(home,0))
            if(r.activityInfo.packageName.equals(c.getPackageName()) && r.activityInfo.exported) found=true;
        assertTrue("Quiet must be a HOME candidate",found);
        android.app.role.RoleManager role=c.getSystemService(android.app.role.RoleManager.class);
        assertTrue(role.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME));
        assertNotNull(c.getPackageManager().resolveActivity(role.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME),0));
    }
    @Test public void focusBlocksSelectedAppAndExpires() {
        String pkg=ComponentName.unflattenFromString(sampleId).getPackageName();
        prefs.edit().putStringSet("guarded",Collections.singleton(pkg)).commit();
        FocusSession.start(prefs,1,100_000,10_000,5);
        assertTrue(GuardPolicy.blocked(prefs,pkg,FocusSession.remaining(prefs,159_999,69_999,5)));
        assertFalse(GuardPolicy.blocked(prefs,pkg,FocusSession.remaining(prefs,160_000,70_000,5)));
        assertFalse(GuardPolicy.blocked(prefs,"not.selected",60_000));
        assertTrue(GuardPolicy.isEssential(InstrumentationRegistry.getInstrumentation().getTargetContext(),"com.android.settings"));
    }
    @Test public void clockChangesDoNotShortenOrExtendFocusWithinBoot() {
        FocusSession.start(prefs,25,1_000_000,100_000,7);
        long expected=1_490_000;
        assertEquals(expected,FocusSession.remaining(prefs,99_000_000,110_000,7));
        assertEquals(expected,FocusSession.remaining(prefs,1,110_000,7));
        // Simulate process recreation by reopening the same persistent preferences.
        SharedPreferences reopened=InstrumentationRegistry.getInstrumentation().getTargetContext()
            .getSharedPreferences("quiet",Context.MODE_PRIVATE);
        assertEquals(expected,FocusSession.remaining(reopened,1,110_000,7));
        FocusSession.end(prefs);
        assertEquals(0,FocusSession.remaining(prefs,1_000_000,110_000,7));
    }
    @Test public void rebootAndLegacyFocusHaveBoundedRecovery() {
        FocusSession.start(prefs,15,100_000,10_000,2);
        assertEquals(899_000,FocusSession.remaining(prefs,101_000,500,3));
        assertEquals(900_000,FocusSession.remaining(prefs,1,500,3));
        assertEquals(0,FocusSession.remaining(prefs,1_100_000,500,3));
        prefs.edit().clear().putLong("focusUntil",500_000).commit();
        assertEquals(100_000,FocusSession.remaining(prefs,400_000,500,3));
    }
    @Test public void focusScreenUpdatesWhenSessionExpires() {
        Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();
        FocusSession.start(c,prefs,1);
        scenario=ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.focus_controls)).perform(scrollTo(),click());
        scenario.onActivity(a->prefs.edit().putLong("focusElapsedEnd",android.os.SystemClock.elapsedRealtime()-1).commit());
        onView(isRoot()).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return isRoot(); }
            public String getDescription() { return "wait for visible focus expiry"; }
            public void perform(UiController ui,View root) { ui.loopMainThreadForAtLeast(1200); }
        });
        onView(withId(R.id.focus_status)).check(matches(withText("No active focus session")));
        onView(withText("End focus session")).check(matches(not(isDisplayed())));
    }

}
