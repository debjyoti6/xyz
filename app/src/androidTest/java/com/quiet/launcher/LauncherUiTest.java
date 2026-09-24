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

/** Acceptance checks run on Android emulators, not yet executed in the authoring environment. */
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
                    !info.activityInfo.packageName.equals(context.getPackageName())) {
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
        assertTrue(prefs.getString("favorites", "").contains(sampleId));
        pressBack(); scenario.recreate();
        onView(isRoot()).perform(new ViewAction() {
            public Matcher<View> getConstraints() { return isRoot(); }
            public String getDescription() { return "wait for favorite after recreation"; }
            public void perform(UiController ui, View root) {
                long deadline=android.os.SystemClock.uptimeMillis()+8000;
                do {
                    for(View v: androidx.test.espresso.util.TreeIterables.breadthFirstViewTraversal(root)) {
                        if("Test Essential, favorite".contentEquals(v.getContentDescription()==null?"":v.getContentDescription())) return;
                    }
                    ui.loopMainThreadForAtLeast(50);
                } while(android.os.SystemClock.uptimeMillis()<deadline);
                throw new AssertionError("Favorite did not reappear after recreation");
            }
        });
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
}
