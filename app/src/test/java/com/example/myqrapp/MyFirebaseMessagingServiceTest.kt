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

class MyFirebaseMessagingServiceTest {

    private lateinit var activity: MyFirebaseMessagingService

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        activity = Mockito.mock(MyFirebaseMessagingService::class.java)
    }

    @Test
    fun generateNotificationTest() {
        Mockito.`when`(activity.generateNotification("title", "description", "invlaid")).thenReturn(false)

        Mockito.`when`(activity.generateNotification("title", "description", "notification")).thenReturn(true)
    }

}
