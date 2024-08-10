package com.boxpay.checkout

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.boxpay.checkout.demoapp.MainActivity
import com.boxpay.checkout.demoapp.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.hamcrest.Matchers.not
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class UpiImplementationTest {

    private val client = OkHttpClient()

    private fun setup(paymentList: List<String>) {
        val url =
            "https://test-apis.boxpay.tech/v0/merchants/lGfqzNSKKA/legal-entities/boxpay/psp-configs"
        val json = """
        {
    "pspCode": "RAZORPAY",
    "rules": [
        {
            "currencyCodes": [
                "INR"
            ],
            "countryCode": "IN",
            "configs": {
                "keyId": "rzp_test_lY96gIWbKVcpff",
                "keySecret": "a062FVM8LH3X1efOzuKqWQMi",
                "mid": "FpzpqpAijicbYS"
            },
            "paymentMethodIds": ${paymentList.map { "\"$it\"" }}
        }
    ]
}
    """
        val requestBody = RequestBody.create("application/json; charset=utf-8".toMediaType(), json)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader(
                "Authorization",
                "Bearer 3z3G6PT8vDhxQCKRQzmRsujsO5xtsQAYLUR3zcKrPwVrphfAqfyS20bvvCg2X95APJsT5UeeS5YdD41aHbz6mg"
            )
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                // no response to be handled
            }
        })
    }

    fun tearUp() {
        val url =
            "https://test-apis.boxpay.tech/v0/merchants/lGfqzNSKKA/legal-entities/boxpay/psp-configs/RAZORPAY"

        val request = Request.Builder()
            .url(url)
            .delete()
            .addHeader(
                "Authorization",
                "Bearer 3z3G6PT8vDhxQCKRQzmRsujsO5xtsQAYLUR3zcKrPwVrphfAqfyS20bvvCg2X95APJsT5UeeS5YdD41aHbz6mg"
            )
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                // no response to be handled
            }
        })

    }

    @Test
    fun upiShouldBeDisableAsNotAvailableForMerchant() {
        setup(
            listOf(
                "aba15ac0-20a2-32a3-8af4-e827d05d9b28",
                "384a848e-7f50-3e42-a60c-24f64f0ef26d",
                "5f29c05e-9647-39fe-ae77-991f0dd97b07",
                "c3a1c32e-8d9b-35d3-ae28-57566a9a0865",
                "5f29c05e-9647-39fe-ae77-991f0dd97b07",
                "e1174d9f-f61c-3726-907d-d741eb6bd9b3",
                "fd45ffb0-d769-35f0-8ea0-23d79b085bc1",
                "66e9d00e-fb72-3475-80e6-b5f8e58863d7",
                "f1550548-6ba4-3323-bbf3-ff467b09a86f",
                "42b55266-116b-33f5-b35e-18f655ecb094",
                "71b10b4a-474e-3b3f-b43e-11b096fb9dfb",
                "4396b929-ccaa-3419-867a-0a6a299f0fa5",
                "f2010482-b2d8-39d3-b0fe-533f7d9dee9f",
                "23da9310-81eb-369a-a919-ee8ba6cd811f",
                "e9fb2709-5ca0-3468-bbcc-364cae5e91af",
                "f8b7271f-2c79-3f11-ab1b-c1e010d0f754",
                "edb42aaf-db91-382b-a2d6-323ec46d24c5",
                "a7e71cf1-cdab-3df2-a211-57db617be89d",
                "36114804-b0d8-3ff9-b731-a5f48a3d5eff",
                "f4c6e3bd-0fc3-3125-bd76-4c7a503590df",
                "aed3be01-d47d-395c-a212-1d5f9973c2fd",
                "384a848e-7f50-3e42-a60c-24f64f0ef26d",
                "d345a4ca-7df3-32be-bdd4-16597ad35fe9",
                "6f43aefe-af23-3de7-9443-8056094c0914",
                "aba15ac0-20a2-32a3-8af4-e827d05d9b28",
                "07763134-7517-3d6c-ab42-1a81c499c538",
                "8b52c0ed-7cb1-30eb-b3a9-4fc31725cc1d",
                "f829f755-fff3-3ddd-9c32-d3d3a9b71605",
                "41c9d6f2-40f8-3de6-8984-4698f9552ca3",
                "6f26c660-46bf-354c-983a-168385637811",
                "32edef5a-70be-3d1f-bc41-642fd6837fc9",
                "090fd9a8-fc39-3ec1-9845-4063d954ab97",
                "8593cd63-358e-3825-b5b1-fd9637a49087",
                "31916b53-ea28-33b7-a2bf-3b079ab5d27e",
                "f5a165ff-b682-3779-bd3a-133a9126009d",
                "03735a13-f694-3379-8625-bee17db193f7",
                "7c383d17-7d79-3abe-ba5f-a6a92338eb30",
                "d7e33593-ad9f-3cfb-8398-abb68f85bf60",
                "f302a7d2-ef7d-362b-bbe3-62c2c1f4ee15",
                "b9aa4101-98c6-33f4-9cc9-4fff0475f7d1",
                "5afdcaf7-e02d-3c9f-9e3a-ccda62ddccf0",
                "835e9efe-d2ed-3494-a4bc-987bd82010e0"
            )
        )
        Thread.sleep(1000L)
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.openByDefault)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.upiLinearLayout)).check(matches(not(isDisplayed())))
        tearUp()
    }

    @Test
    fun upiEnabledButOnlyUpiCollectAvailableForMerchant() {
        setup(
            listOf(
                "0dd4beee-ba7e-32d7-ac23-214a606605c4"
            )
        )
        Thread.sleep(1000L)
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.openByDefault)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.upiLinearLayout)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.popularUPIAppsConstraint)).check(matches(not(isDisplayed())))
        onView(withId(com.boxpay.checkout.sdk.R.id.addNewUPIIDConstraint)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.UPIQRConstraint)).check(matches(not(isDisplayed())))
        tearUp()
    }

    @Test
    fun enteringWrongUpiId() {
        setup(
            listOf(
                "0dd4beee-ba7e-32d7-ac23-214a606605c4"
            )
        )
        Thread.sleep(1000L)
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.openByDefault)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.upiLinearLayout)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.popularUPIAppsConstraint)).check(matches(not(isDisplayed())))
        onView(withId(com.boxpay.checkout.sdk.R.id.addNewUPIIDConstraint)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.UPIQRConstraint)).check(matches(not(isDisplayed())))
        onView(withId(com.boxpay.checkout.sdk.R.id.addNewUPIIDConstraint)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.frameLayout1)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.editText)).perform(typeText("wrong_formatupi"))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(not(isEnabled())))
        tearUp()
    }

    @Test
    fun enteringCorrectUpiId() {
        setup(
            listOf(
                "0dd4beee-ba7e-32d7-ac23-214a606605c4"
            )
        )
        Thread.sleep(1000L)
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.openByDefault)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.linearLayoutMain)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.upiLinearLayout)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.popularUPIAppsConstraint)).check(matches(not(isDisplayed())))
        onView(withId(com.boxpay.checkout.sdk.R.id.addNewUPIIDConstraint)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.UPIQRConstraint)).check(matches(not(isDisplayed())))
        onView(withId(com.boxpay.checkout.sdk.R.id.addNewUPIIDConstraint)).perform(click())
        onView(withId(com.boxpay.checkout.sdk.R.id.frameLayout1)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.editText)).perform(typeText("test@boxpay"))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).check(matches(isEnabled()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).perform(click())
        Thread.sleep(1000L)
        onView(withId(com.boxpay.checkout.sdk.R.id.frameLayout1)).check(matches(isDisplayed()))
        onView(withId(com.boxpay.checkout.sdk.R.id.proceedButton)).perform(click())
        tearUp()
    }
}