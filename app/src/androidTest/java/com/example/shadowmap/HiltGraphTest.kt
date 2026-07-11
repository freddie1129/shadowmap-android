package com.example.shadowmap

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shadowmap.domain.BuildingShadowCalculator
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class HiltGraphTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var shadowCalculator: BuildingShadowCalculator

    @Before
    fun inject() {
        hiltRule.inject()
    }

    @Test
    fun applicationGraph_providesShadowCalculator() {
        assertNotNull(shadowCalculator)
    }
}
