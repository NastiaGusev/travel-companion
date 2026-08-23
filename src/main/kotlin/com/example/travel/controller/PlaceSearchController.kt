package com.example.travel.controller

import com.example.travel.model.dto.PlaceSearchResultDto
import com.example.travel.service.PlaceSearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.*

@Tag(
    name = "Place Search",
    description = "Search for places via Google Places, optionally biased toward a trip's destination",
)
@RestController
@RequestMapping("/api/places")
class PlaceSearchController(
    private val placeSearchService: PlaceSearchService,
) {

    @Operation(
        summary = "Search for places",
        description = "Returns place predictions for the query. When a tripId is supplied, " +
                "results are biased toward that trip's destination; otherwise the search is global. " +
                "Used both for picking a trip destination and for adding stops within a trip.",
    )
    @GetMapping("/search")
    fun searchPlaces(
        @RequestParam query: String,
        @RequestParam(required = false) tripId: UUID?,
        @AuthenticationPrincipal userId: UUID,
    ): List<PlaceSearchResultDto> =
        placeSearchService.search(query, userId, tripId)
}