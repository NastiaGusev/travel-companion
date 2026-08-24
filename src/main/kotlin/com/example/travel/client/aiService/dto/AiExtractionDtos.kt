package com.example.travel.client.aiService.dto

/**
 * Wire shape for the ai-service /extract contract. Field names are already camelCase on both
 * sides (Kotlin data class defaults <-> Python's Field(alias=...)), so no @JsonProperty mapping
 * is needed — see ai-service/app/models/extract.py for the Python side of this same contract.
 */
data class ExtractRequest(val text: String)

data class ExtractResponse(val days: List<ExtractedDay> = emptyList())

data class ExtractedDay(
    val position: Int,
    val stops: List<ExtractedStop> = emptyList(),
)

data class ExtractedStop(
    val searchQuery: String,
    val name: String,
    val timeHint: String? = null,
)
