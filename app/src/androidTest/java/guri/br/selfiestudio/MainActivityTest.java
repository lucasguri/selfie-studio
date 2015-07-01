package guri.br.selfiestudio;

import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiSelector;
import android.test.ActivityInstrumentationTestCase2;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

/**
 * Created by Monica Cavalcante on 19/06/2015.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    UiDevice mDevice;
    MainActivity mActivity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mActivity = getActivity();
        mDevice = UiDevice.getInstance(getInstrumentation());
    }

    public void testDenyBT() throws Exception {
        // Deve sair do app
        mDevice.findObject(new UiSelector().textContains("Recusar")).click();
    }

    public void testAcceptBT() throws Exception {
        // Deve iniciar o app
        mDevice.findObject(new UiSelector().textContains("Permitir")).click();

        onView(withId(R.id.camera_mode)).check(matches(isDisplayed()));
        onView(withId(R.id.remote_mode)).check(matches(isDisplayed()));
        onView(withId(R.id.textView)).check(matches(isDisplayed()));
    }

    public void testStartWithBTOn() throws Exception {
        // Nao deve pedir para ligar o Bluetooth
        onView(withId(R.id.camera_mode)).check(matches(isDisplayed()));
        onView(withId(R.id.remote_mode)).check(matches(isDisplayed()));
        onView(withId(R.id.textView)).check(matches(isDisplayed()));
    }

    //OK
    public void testOptionsDisplayed() {
        onView(withId(R.id.camera_mode)).check(matches(isDisplayed()));
        onView(withId(R.id.remote_mode)).check(matches(isDisplayed()));
        onView(withId(R.id.textView)).check(matches(isDisplayed()));
    }

    public void testClickCameraMode() throws Exception {
        onView(withId(R.id.camera_mode)).perform(click());
        mDevice.findObject(new UiSelector().textContains("Permitir")).click();
    }

    public void testClickRemoteMode() throws Exception {

        onView(withId(R.id.remote_mode)).perform(click());
    }

}