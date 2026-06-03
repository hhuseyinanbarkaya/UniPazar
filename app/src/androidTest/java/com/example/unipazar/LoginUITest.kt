package com.example.unipazar

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginUITest {

    @get:Rule
    val activityRule = ActivityScenarioRule(LoginActivity::class.java)

    @Test
    fun testUserLoginFlow() {
        // Kullanıcının görebilmesi için kısa bir bekleme
        Thread.sleep(2000)

        // E-posta alanına metin gir
        onView(withId(R.id.etEmail))
            .perform(typeText("soyyilmaz@stu.okan.edu.tr"), closeSoftKeyboard())

        Thread.sleep(1500)

        // Şifre alanına metin gir
        onView(withId(R.id.etPassword))
            .perform(typeText("1234567"), closeSoftKeyboard())

        Thread.sleep(1500)

        // Giriş yap butonuna tıkla
        onView(withId(R.id.btnLogin)).perform(click())

        // Kullanıcının Toast mesajını veya sonucu görebilmesi için bekle
        Thread.sleep(5000)
        
        // Kayıt ol butonuna da örnek olarak tıklayalım ki görebilsin
        onView(withId(R.id.btnRegister)).perform(click())
        
        Thread.sleep(4000)
    }
}
