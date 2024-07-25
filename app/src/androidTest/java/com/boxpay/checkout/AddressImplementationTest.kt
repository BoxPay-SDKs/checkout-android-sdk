package com.boxpay.checkout

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.clearText
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boxpay.checkout.demoapp.MainActivity
import com.boxpay.checkout.demoapp.R
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddressImplementationTest {

    @Test
    fun proceedToEnterNewAddress() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.enterTokenButton)).perform(click())
        onView(withId(R.id.editTextText)).check(matches(isDisplayed()))
        onView(withId(R.id.editTextText)).perform(typeText("246cc1fd-e74a-453c-9732-417b59b9d621"))
        onView(withId(R.id.button)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).check(matches(not(
            isDisplayed()
        )))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.frameLayout1)).check(matches(isDisplayed()))
    }

    @Test
    fun verifyNameIsFilledAndShownNoError() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.enterTokenButton)).perform(click())
        onView(withId(R.id.editTextText)).check(matches(isDisplayed()))
        onView(withId(R.id.editTextText)).perform(typeText("246cc1fd-e74a-453c-9732-417b59b9d621"))
        onView(withId(R.id.button)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).check(matches(not(
            isDisplayed()
        )))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.frameLayout1)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.fullNameEditText)).perform(typeText("ishika bansal"))
        onView(withId(com.boxpay.checkout.sdk.R.id.fullNameErrorTex)).check(matches(not(isDisplayed())))
    }

    @Test
    fun fillAllAddressDetails() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.enterTokenButton)).perform(click())
        onView(withId(R.id.editTextText)).check(matches(isDisplayed()))
        onView(withId(R.id.editTextText)).perform(typeText("246cc1fd-e74a-453c-9732-417b59b9d621"))
        onView(withId(R.id.button)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).check(matches(not(
            isDisplayed()
        )))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.frameLayout1)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.fullNameEditText)).perform(typeText("ishika bansal"))
        onView(withId(com.boxpay.checkout.sdk.R.id.mobileNumberEditText)).perform(typeText("9876543210"))
        onView(withId(com.boxpay.checkout.sdk.R.id.emailEditText)).perform(typeText("ishika.bansal@boxpay.tech"))
        onView(withId(com.boxpay.checkout.sdk.R.id.postalCodeEditText)).perform(typeText("147147"))
        onView(withId(com.boxpay.checkout.sdk.R.id.stateEditText)).perform(scrollTo(), typeText("Delhi"))
        onView(withId(com.boxpay.checkout.sdk.R.id.cityEditText)).perform(scrollTo(), typeText("New Delhi"))
        onView(withId(com.boxpay.checkout.sdk.R.id.addressEditText1)).perform(scrollTo(), typeText("cbxhvdc  cbdhgcnd cbdhvd  d vbdsv vd vhd"))
        onView(withId(com.boxpay.checkout.sdk.R.id.addressEditText2)).perform(scrollTo(), typeText("cbxhvdc  cbdhgcnd cbdhvd  d vbdsv vd vhd"))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(isEnabled()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).check(matches(
            isDisplayed()
        ))
    }

    @Test
    fun editTheFilledNameToADifferentName() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.openByDefault)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).check(matches(
            isDisplayed()
        ))
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.frameLayout1)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.fullNameEditText)).perform(clearText())
        onView(withId(com.boxpay.checkout.sdk.R.id.fullNameErrorTex)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(not(isEnabled())))
        onView(withId(com.boxpay.checkout.sdk.R.id.fullNameEditText)).perform(typeText("integration test"))
        onView(withId(com.boxpay.checkout.sdk.R.id.fullNameErrorTex)).check(matches(not(isDisplayed())))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(isEnabled()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).check(matches(
            isDisplayed()
        ))
        onView(withId(com.boxpay.checkout.sdk.R.id.nameTextView)).check(matches(
            isDisplayed()
        ))
        onView(withId(com.boxpay.checkout.sdk.R.id.nameTextView)).check(matches(withText("integration test")))
    }

    @Test
    fun editTheFilledNumberToADifferentNumber() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.openByDefault)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).check(matches(
            isDisplayed()
        ))
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.frameLayout1)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.mobileNumberEditText)).perform(clearText())
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(not(isEnabled())))
        onView(withId(com.boxpay.checkout.sdk.R.id.mobileErrorText)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.mobileNumberEditText)).perform(typeText("9123456789"))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(isEnabled()))
        onView(withId(com.boxpay.checkout.sdk.R.id.mobileErrorText)).check(matches(not(isDisplayed())))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.deliveryAddressConstraintLayout)).check(matches(
            isDisplayed()
        ))
        onView(withId(com.boxpay.checkout.sdk.R.id.mobileNumberTextViewMain)).check(matches(
            isDisplayed()
        ))
        onView(withId(com.boxpay.checkout.sdk.R.id.mobileNumberTextViewMain)).check(matches(withText("(+919123456789)")))
    }
}