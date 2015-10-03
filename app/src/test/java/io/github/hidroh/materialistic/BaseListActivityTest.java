package io.github.hidroh.materialistic;

import android.content.Intent;
import android.net.Uri;
import android.view.View;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.robolectric.shadows.ShadowResolveInfo;
import org.robolectric.util.ActivityController;

import io.github.hidroh.materialistic.test.ShadowSupportPreferenceManager;
import io.github.hidroh.materialistic.test.TestListActivity;
import io.github.hidroh.materialistic.test.TestWebItem;
import io.github.hidroh.materialistic.test.WebActivity;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.assertj.android.api.Assertions.assertThat;
import static org.robolectric.Shadows.shadowOf;

// TODO switch to API 21 once ShareActionProvider is fixed
@Config(sdk = 19, shadows = {ShadowSupportPreferenceManager.class})
@RunWith(RobolectricGradleTestRunner.class)
public class BaseListActivityTest {
    private ActivityController<TestListActivity> controller;
    private TestListActivity activity;

    @Before
    public void setUp() {
        controller = Robolectric.buildActivity(TestListActivity.class);
        activity = controller.create().start().resume().visible().get();
    }

    @Test
    public void testCreate() {
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment));
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story));
        assertNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
    }

    @Test
    public void testRotate() {
        RuntimeEnvironment.setQualifiers("w820dp-land");
        activity.onConfigurationChanged(RuntimeEnvironment.application.getResources().getConfiguration());
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_comment));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_story));
        assertNotNull(shadowOf(activity).getOptionsMenu().findItem(R.id.menu_share));
    }

    @Test
    public void testSelectItemOpenWeb() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }
        }, new View(activity));
        Intent actual = shadowOf(activity).getNextStartedActivity();
        assertEquals(ItemActivity.class.getName(), actual.getComponent().getClassName());
        assertThat(actual).hasExtra(ItemActivity.EXTRA_OPEN_ARTICLE);
        assertTrue(actual.getBooleanExtra(ItemActivity.EXTRA_OPEN_ARTICLE, false));
    }

    @Test
    public void testSelectItemOpenExternal() {
        RobolectricPackageManager packageManager = (RobolectricPackageManager)
                RuntimeEnvironment.application.getPackageManager();
        packageManager.addResolveInfoForIntent(
                new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://example.com")),
                ShadowResolveInfo.newResolveInfo("label", activity.getPackageName(),
                        WebActivity.class.getName()));
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(activity.getString(R.string.pref_external), true)
                .commit();
        controller.pause().resume();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getUrl() {
                return "http://example.com";
            }
        }, new View(activity));
        assertThat(shadowOf(activity).getNextStartedActivity()).hasAction(Intent.ACTION_VIEW);
    }

    @Test
    public void testSelectItemOpenItem() {
        ShadowSupportPreferenceManager.getDefaultSharedPreferences(activity)
                        .edit()
                        .putString(activity.getString(R.string.pref_story_display),
                                activity.getString(R.string.pref_story_display_value_comments))
                        .commit();
        controller.pause().resume();
        activity.onItemSelected(new TestWebItem() {
            @Override
            public String getId() {
                return "1";
            }
        }, new View(activity));
        assertEquals(ItemActivity.class.getName(),
                shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
    }

    @Test
    public void testGetSelectedItem() {
        activity.onItemSelected(new TestWebItem() {
            @Override
            public boolean isShareable() {
                return true;
            }

            @Override
            public String getId() {
                return "1";
            }
        }, new View(activity));
        assertNull(activity.getSelectedItem());
    }

    @After
    public void tearDown() {
        controller.pause().stop().destroy();
    }
}
