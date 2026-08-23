package com.example.travel.service

import com.example.travel.client.googlePlaces.PlacesClient
import com.example.travel.model.dto.PlaceSearchResultDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class PlaceSearchService(
    private val placesClient: PlacesClient,
    private val tripAccessService: TripAccessService,
) {
    fun search(query: String, userId: UUID, tripId: UUID? = null): List<PlaceSearchResultDto> {
        val anchor = tripId?.let { tripAccessService.requireEditAccess(it, userId).destination }
        return placesClient.autocomplete(query, anchor?.latitude, anchor?.longitude)
            .map { PlaceSearchResultDto(it.placeId, it.description) }
    }
}