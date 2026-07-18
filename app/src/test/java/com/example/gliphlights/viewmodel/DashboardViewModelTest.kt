package com.example.gliphlights.viewmodel

import com.example.gliphlights.models.AnimationParams
import com.example.gliphlights.models.DeviceInfo
import com.example.gliphlights.models.GlyphState
import com.example.gliphlights.models.SdkResult
import com.example.gliphlights.repository.GlyphRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: GlyphRepository
    private lateinit var viewModel: DashboardViewModel

    private val glyphStateFlow = MutableStateFlow(GlyphState.INACTIVE)
    private val deviceInfoFlow = MutableStateFlow(DeviceInfo.UNKNOWN)
    private val isConnectedFlow = MutableStateFlow(false)
    private val isSessionActiveFlow = MutableStateFlow(false)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        val settingsRepository = mockk<com.example.gliphlights.repository.SettingsRepository>(relaxed = true)
        val presetRepository = mockk<com.example.gliphlights.repository.PresetRepository>(relaxed = true)
        val presetPlayer = mockk<com.example.gliphlights.presets.PresetPlayer>(relaxed = true)
        val glyphPackShare = mockk<com.example.gliphlights.presets.GlyphPackShare>(relaxed = true)
        val context = mockk<android.content.Context>(relaxed = true)

        coEvery { repository.glyphState } returns glyphStateFlow
        coEvery { repository.deviceInfo } returns deviceInfoFlow
        coEvery { repository.isConnected } returns isConnectedFlow
        coEvery { repository.isSessionActive } returns isSessionActiveFlow
        coEvery { repository.initialize() } returns SdkResult.Success(Unit)
        coEvery { repository.register() } returns SdkResult.Success(Unit)
        coEvery { repository.openSession() } returns SdkResult.Success(Unit)
        coEvery { repository.closeSession() } returns SdkResult.Success(Unit)
        coEvery { repository.toggleAll() } returns SdkResult.Success(Unit)
        coEvery { repository.animateAll(any()) } returns SdkResult.Success(Unit)
        coEvery { repository.turnOff() } returns SdkResult.Success(Unit)
        coEvery { repository.applyStartupBehavior() } returns Unit
        coEvery { settingsRepository.lastStudioRoute } returns kotlinx.coroutines.flow.flowOf("editor")
        coEvery { presetRepository.presets } returns kotlinx.coroutines.flow.flowOf(emptyList())

        viewModel = DashboardViewModel(
            context,
            repository,
            settingsRepository,
            presetRepository,
            presetPlayer,
            glyphPackShare
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        // Then
        assertTrue(viewModel.uiState.value is com.example.gliphlights.models.GlyphUiState.Loading)
    }

    @Test
    fun `initializeSdk calls repository methods in order`() = runTest {
        // When
        advanceUntilIdle()

        // Then
        coVerify { repository.initialize() }
        coVerify { repository.register() }
        coVerify { repository.openSession() }
        coVerify { repository.applyStartupBehavior() }
    }

    @Test
    fun `toggleAll calls repository toggleAll`() = runTest {
        // When
        viewModel.toggleAll()
        advanceUntilIdle()

        // Then
        coVerify { repository.toggleAll() }
    }

    @Test
    fun `animateAll calls repository animateAll`() = runTest {
        // When
        viewModel.animateAll()
        advanceUntilIdle()

        // Then
        coVerify { repository.animateAll(any()) }
    }

    @Test
    fun `turnOff calls repository turnOff`() = runTest {
        // When
        viewModel.turnOff()
        advanceUntilIdle()

        // Then
        coVerify { repository.turnOff() }
    }
}
