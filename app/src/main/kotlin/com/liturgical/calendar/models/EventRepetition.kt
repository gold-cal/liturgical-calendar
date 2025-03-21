package com.liturgical.calendar.models

data class EventRepetition(val repeatInterval: Int, val repeatRule: Int, val repeatLimit: Long, val extendedRule: Int)
