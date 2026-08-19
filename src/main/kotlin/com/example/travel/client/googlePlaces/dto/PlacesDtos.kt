package com.example.travel.client.googlePlaces.dto

// ---- Clean types returned to the service layer ----
data class PlacePrediction(val placeId: String, val description: String)

data class ResolvedPlaceData(
    val placeId: String,
    val name: String?,
    val latitude: Double?,
    val longitude: Double?,
    val address: String?,
    val category: String?,
)

// ---- Internal Google request/response DTOs ----

internal data class AutocompleteRequest(
    val input: String,
    val locationBias: LocationBias?,
)

internal data class LocationBias(val circle: Circle)
internal data class Circle(val center: LatLng, val radius: Double)
internal data class LatLng(val latitude: Double, val longitude: Double)

internal data class AutocompleteResponse(val suggestions: List<Suggestion>?)
internal data class Suggestion(val placePrediction: GooglePlacePrediction?)
internal data class GooglePlacePrediction(val placeId: String, val text: PredictionText?)
internal data class PredictionText(val text: String?)

internal data class PlaceDetailsResponse(
    val id: String,
    val displayName: DisplayName?,
    val location: Location?,
    val formattedAddress: String?,
    val types: List<String>?,
)
internal data class DisplayName(val text: String?)
internal data class Location(val latitude: Double?, val longitude: Double?)