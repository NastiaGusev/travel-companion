package com.example.travel.controller

import com.example.travel.model.dto.QuickAddRequest
import com.example.travel.model.dto.QuickAddResponse
import com.example.travel.service.QuickAddService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(
    name = "Quick Add",
    description = "AI-assisted planning: paste free text describing a trip and get structured days and stops",
)
@RestController
@RequestMapping("/api/trips/{tripId}/quick-add")
class QuickAddController(
    private val quickAddService: QuickAddService,
) {
    @Operation(
        summary = "Quick-add days and stops from free text",
        description = "Sends the text to the AI extraction service, resolves each stop against Google Places, " +
                "and appends the resulting days/stops to the trip. Returns 200 with an empty list when nothing " +
                "could be extracted (not an error).",
    )
    @PostMapping
    fun quickAdd(
        @AuthenticationPrincipal userId: UUID,
        @PathVariable tripId: UUID,
        @Valid @RequestBody request: QuickAddRequest,
    ): QuickAddResponse = quickAddService.quickAdd(userId, tripId, request)
}
