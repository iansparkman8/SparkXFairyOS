package com.sparkx.fairyos

import android.app.Application
import com.sparkx.fairyos.domain.memory.TeachGrowRepository

class SparkXApplication : Application() {

    lateinit var teachGrowRepository: TeachGrowRepository
        private set

    override fun onCreate() {
        super.onCreate()
        teachGrowRepository = TeachGrowRepository(this)
        // Future: init other singletons
    }
}