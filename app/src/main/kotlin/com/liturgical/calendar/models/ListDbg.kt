package com.liturgical.calendar.models

data class ListDbg(
    var id: Long, var title: String, var repeatInterval: Int, var repeatRule: Int, var importId: String, var flags: Int,
    var eventType: Long, var source: String, var extendedRule: Int, var color: Int
) : ListItem()
