

package com.example.smartshopping

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)



    // Test 3: Artikel bearbeiten (langer Klick auf Liste)
    @Test
    fun testArtikelBearbeiten() {
        onView(withId(R.id.btnArtikel)).perform(click())

        // Vorher neuen Artikel hinzufügen
        onView(withId(R.id.fabAddArtikel)).perform(click())
        onView(withId(R.id.etName)).perform(typeText("Butter"), closeSoftKeyboard())
        onView(withId(R.id.etHaltbarkeitTage)).perform(typeText("10"), closeSoftKeyboard())
        onView(withId(R.id.btnSpeichern)).perform(click())

        // Artikel per Long-Click bearbeiten
        onView(withText("Butter")).perform(longClick())

        // Menge ändern
        onView(withId(R.id.btnSpeichern)).perform(click())

        // Test: Artikelname noch da
        onView(withText("Butter")).check(matches(isDisplayed()))
    }

    // Test 4: Artikel-Details anzeigen
    @Test
    fun testArtikelDetailsAnzeigen() {
        onView(withId(R.id.btnArtikel)).perform(click())

        // Artikel anlegen, falls nicht vorhanden
        onView(withId(R.id.fabAddArtikel)).perform(click())
        onView(withId(R.id.etName)).perform(typeText("Käse"), closeSoftKeyboard())
        onView(withId(R.id.etHaltbarkeitTage)).perform(typeText("12"), closeSoftKeyboard())
        onView(withId(R.id.btnSpeichern)).perform(click())

        // Klicke auf Käse → Details sollten erscheinen
        onView(withText("Käse")).perform(click())

        onView(withText(containsString("Käse"))).check(matches(isDisplayed()))
        onView(withText(containsString("200g"))).check(matches(isDisplayed()))
    }
}
