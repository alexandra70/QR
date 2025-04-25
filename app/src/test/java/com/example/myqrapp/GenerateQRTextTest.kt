package com.example.myqrapp

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito


@RunWith(JUnit4::class)

class GenerateQRTextTest {

    private lateinit var activity: GenerateQRText

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        activity = Mockito.mock(GenerateQRText::class.java)
    }

    @Test
    fun whenCallEncoderInvalid() {
        Mockito.`when`(activity.callEncoderForWindow(String(), "window")).thenReturn(false)

        Mockito.`when`(activity.callEncoderForWindow("valid", "")).thenReturn(false)

        Mockito.`when`(activity.callEncoderForWindow("valid", "invalid")).thenReturn(false)
    }

    @Test
    fun whenCallEncoderValid() {
        Mockito.`when`(activity.callEncoderForWindow("valid", "widow")).thenReturn(true)
    }
}
