package com.gooludou.shadowplanner.location

data class LocationSearchResult(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double?,
    val longitude: Double?
)
