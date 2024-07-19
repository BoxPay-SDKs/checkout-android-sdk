package com.boxpay.checkout

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boxpay.checkout.demoapp.MainActivity
import com.boxpay.checkout.demoapp.R
import com.boxpay.checkout.server.MockWebServerRule
import org.hamcrest.Matchers.anything
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class EnvironmentTest {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @get:Rule
    val serverRule = MockWebServerRule()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        sharedPreferences =
            context.getSharedPreferences("TransactionDetails", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    @Test
    fun openSdkThroughDefaultToken() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.openByDefault)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
    }


    @Test
    fun openSdkThroughEnterTokenTestEnv() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.enterTokenButton)).perform(click())
        onView(withId(R.id.editTextText)).check(matches(isDisplayed()))
        onView(withId(R.id.editTextText)).perform(typeText("9863977f-d4c6-4c97-b1cf-ad5a4021d308"))
        onView(withId(R.id.button)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
    }

    @Test
    fun openSdkThroughEnterTokenSandboxEnv() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.enterTokenButton)).perform(click())
        onView(withId(R.id.editTextText)).check(matches(isDisplayed()))
        onView(withId(R.id.editTextText)).perform(typeText("1248fa1d-acc1-49fd-bb0a-7e5e4186e33c"))
        onView(withId(R.id.environmentSpinner)).perform(click())
        onData(anything()).atPosition(1).perform(click())
        onView(withId(R.id.button)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
    }

    @Test
    fun openSdkThroughEnterTokenProdEnv() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.enterTokenButton)).perform(click())
        onView(withId(R.id.editTextText)).check(matches(isDisplayed()))
        onView(withId(R.id.editTextText)).perform(typeText("536d2dd5-99cf-456b-96e6-c033def6cc54"))
        onView(withId(R.id.environmentSpinner)).perform(click())
        onData(anything()).atPosition(2).perform(click())
        onView(withId(R.id.button)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
    }
}