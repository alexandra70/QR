package com.example.myqrapp

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito
import org.mockito.Mockito.*

@RunWith(JUnit4::class)

class FilesSystemTest {

    private lateinit var activity: FilesSystem

    @get:Rule var rule: TestRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        activity = mock(FilesSystem::class.java)

    }

    @Test
    fun whenFileInvalid() {
        Mockito.`when`(activity.localQRGenerate("string")).thenReturn(true)
    }

    @Test
    fun testFileContentURIInvalid() {
        //Mockito.`when`((activity.readFileContent(Uri.EMPTY))).thenReturn(false)
    }

}
