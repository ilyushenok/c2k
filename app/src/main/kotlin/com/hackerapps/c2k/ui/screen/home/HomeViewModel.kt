package com.hackerapps.c2k.ui.screen.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.hackerapps.c2k.C2KApp
import com.hackerapps.c2k.data.db.entity.WorkoutSessionEntity
import com.hackerapps.c2k.data.model.Programs
import com.hackerapps.c2k.data.model.WorkoutPlan
import com.hackerapps.c2k.data.prefs.UserPreferences
import com.hackerapps.c2k.service.WorkoutService

data class NextWorkout(
    val programId: String,
    val displayName: String,
    val week: Int,
    val day: Int
)

data class HomeUiState(
    val programs: List<WorkoutPlan> = Programs.all(),
    val recentSessions: List<WorkoutSessionEntity> = emptyList(),
    val workoutActive: Boolean = false,
    val activeWorkoutInfo: WorkoutService.WorkoutInfo? = null,
    val nextWorkout: NextWorkout? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = (app as C2KApp).sessionRepository
    private val prefs = UserPreferences(app)

    private val nextWorkoutFlow = prefs.lastProgramId
        .flatMapLatest { lastProgId ->
            if (lastProgId == null) {
                flowOf(null)
            } else {
                val plan = Programs.all().find { it.programId == lastProgId }
                if (plan == null) flowOf(null)
                else repo.observeCompletedDays(lastProgId).map { completedDays ->
                    computeNextWorkout(plan, completedDays)
                }
            }
        }

    val uiState = combine(
        repo.observeRecentSessions(),
        WorkoutService.isRunning,
        WorkoutService.currentWorkout,
        nextWorkoutFlow
    ) { sessions, active, workoutInfo, nextWorkout ->
        HomeUiState(
            recentSessions = sessions,
            workoutActive = active,
            activeWorkoutInfo = workoutInfo,
            nextWorkout = if (active) null else nextWorkout
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private fun computeNextWorkout(
        plan: WorkoutPlan,
        completedDays: Set<Pair<Int, Int>>
    ): NextWorkout? {
        for ((weekIdx, days) in plan.weeks.withIndex()) {
            for (dayIdx in days.indices) {
                val week = weekIdx + 1
                val day = dayIdx + 1
                if ((week to day) !in completedDays) {
                    return NextWorkout(plan.programId, plan.displayName, week, day)
                }
            }
        }
        return null
    }
}
