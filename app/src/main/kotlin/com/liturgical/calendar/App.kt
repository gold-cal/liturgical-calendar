package com.liturgical.calendar

import androidx.multidex.MultiDexApplication
import com.secure.commons.extensions.checkUseEnglish

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
    }
}
