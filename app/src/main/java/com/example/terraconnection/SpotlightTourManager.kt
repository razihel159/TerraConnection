package com.example.terraconnection

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.getkeepsafe.taptargetview.TapTarget
import com.getkeepsafe.taptargetview.TapTargetSequence

object SpotlightTourManager {
    private const val PREF_NAME = "spotlight_tour"
    private const val KEY_TOUR_SHOWN = "tour_shown"

    fun shouldShowTour(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return !prefs.getBoolean(KEY_TOUR_SHOWN, false)
    }

    fun markTourAsShown(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_TOUR_SHOWN, true).apply()
    }

    fun startTour(context: Context, role: String, views: Map<String, View>) {
        val targets = when (role) {
            "student" -> getStudentTourTargets(context, views)
            "professor" -> getProfessorTourTargets(context, views)
            "guardian" -> getGuardianTourTargets(context, views)
            else -> emptyList()
        }

        if (targets.isEmpty()) return

        TapTargetSequence(context as android.app.Activity)
            .targets(targets)
            .listener(object : TapTargetSequence.Listener {
                override fun onSequenceFinish() {
                    markTourAsShown(context)
                }
                override fun onSequenceStep(lastTarget: TapTarget, targetClicked: Boolean) {}
                override fun onSequenceCanceled(lastTarget: TapTarget) {
                    markTourAsShown(context)
                }
            })
            .start()
    }

    private fun getStudentTourTargets(context: Context, views: Map<String, View>): List<TapTarget> {
        val targets = mutableListOf<TapTarget>()
        
        views["subjectNotification"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Class Notifications",
                "Stay updated with important announcements from your professors")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        views["attendanceLog"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Attendance Log",
                "Track your attendance records for all classes")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        views["bottomNavCalendar"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Class Schedule",
                "View your daily and weekly class schedules")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        return targets
    }

    private fun getProfessorTourTargets(context: Context, views: Map<String, View>): List<TapTarget> {
        val targets = mutableListOf<TapTarget>()

        views["scheduleSection"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Class Schedule",
                "View and manage your teaching schedule")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        views["bottomNavCalendar"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Calendar",
                "Access your teaching calendar and class details")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        views["bottomNavLocation"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Location Services",
                "Track student attendance based on location")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        return targets
    }

    private fun getGuardianTourTargets(context: Context, views: Map<String, View>): List<TapTarget> {
        val targets = mutableListOf<TapTarget>()

        views["studentStatusContainer"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Student Status",
                "Monitor your child's attendance and campus presence")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        views["bottomNavCalendar"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Schedule",
                "View your child's class schedule")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        views["bottomNavLocation"]?.let { view ->
            targets.add(TapTarget.forView(view,
                "Location",
                "Check your child's location on campus")
                .tintTarget(false)
                .targetCircleColor(android.R.color.white)
                .titleTextColor(android.R.color.white)
                .descriptionTextColor(android.R.color.white)
                .outerCircleColor(R.color.violet))
        }

        return targets
    }
} 