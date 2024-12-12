package com.liturgical.calendar.models

data class ListSectionDay(val title: String, val code: String,val bg: Int, val isToday: Boolean, val isPastSection: Boolean) : ListItem()
