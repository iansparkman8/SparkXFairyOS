package com.sparkx.fairyos

import android.app.Application
import com.sparkx.fairyos.domain.memory.TeachGrowRepository
import com.sparkx.fairyos.domain.personality.SparkPersonalityRepository

class SparkXApplication : Application() {

    lateinit var teachGrowRepository: TeachGrowRepository
        private set

    lateinit var sparkPersonalityRepository: SparkPersonalityRepository
        private set

    override fun onCreate() {
        super.onCreate()
        teachGrowRepository = TeachGrowRepository(this)
        sparkPersonalityRepository = SparkPersonalityRepository(this)
    }
}