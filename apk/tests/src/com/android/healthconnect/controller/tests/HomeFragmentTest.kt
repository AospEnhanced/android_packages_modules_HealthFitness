/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.healthconnect.controller.tests

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.android.healthconnect.controller.R


import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.android.healthconnect.controller.MainActivity
import org.junit.Rule
import org.junit.Test


class HomeFragmentTest {

  /**
   * [ActivityScenarioRule] is a JUnit [@Rule][Rule]
   * to launch your activity under test.
   *
   * Rules are interceptors which are executed for each test method and are important building
   * blocks of Junit tests.
   */
  @get:Rule
  val rule = ActivityScenarioRule<MainActivity>(MainActivity::class.java)

  @Test
  fun test_HomeFragment_isDisplayed() {
    onView(withText(R.string.home_subtitle)).check(matches(isDisplayed()))
    onView(withText(R.string.connected_apps_title)).check(matches(isDisplayed()))
    onView(withText(R.string.connected_apps_subtitle)).check(matches(isDisplayed()))
    onView(withText(R.string.data_title)).check(matches(isDisplayed()))
    onView(withText(R.string.data_subtitle)).check(matches(isDisplayed()))
  }

}