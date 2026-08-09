package com.example.travel.service

import com.example.travel.dto.StopRequest
import com.example.travel.dto.StopResponse
import com.example.travel.entity.Stop
import com.example.travel.repository.ItineraryDayRepository
import com.example.travel.repository.StopRepository
import com.example.travel.repository.TripRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.OffsetDateTime
import java.util.UUID

@Service
class StopService(
    private val stopRepository: StopRepository,
    private val dayRepository: ItineraryDayRepository,
    private val tripRepository: TripRepository,
) {
    @Transactional
    fun create(userId: UUID, dayId: UUID, request: StopRequest): StopResponse {
        verifyDayOwnership(dayId, userId)
        validateTimes(request.startTime, request.endTime)

        val stop = Stop(
            dayId = dayId,
            name = request.name,
            position = 0,
            startTime = request.startTime,
            endTime = request.endTime,
            notes = request.notes,
        )
        val saved = stopRepository.saveAndFlush(stop)
        resequence(dayId)
        return stopRepository.findById(saved.id!!).get().toResponse()
    }

    @Transactional
    fun update(userId: UUID, stopId: UUID, request: StopRequest): StopResponse {
        val stop = loadOwnedStop(stopId, userId)
        validateTimes(request.startTime, request.endTime)

        val timeChanged = stop.startTime != request.startTime || stop.endTime != request.endTime

        stop.name = request.name
        stop.startTime = request.startTime
        stop.endTime = request.endTime
        stop.notes = request.notes
        stop.updatedAt = OffsetDateTime.now()
        stopRepository.saveAndFlush(stop)

        if (timeChanged) {
            resequence(stop.dayId)
        }

        return stopRepository.findById(stopId).get().toResponse()
    }

    @Transactional
    fun delete(userId: UUID, stopId: UUID): List<StopResponse> {
        val stop = loadOwnedStop(stopId, userId)
        val dayId = stop.dayId
        stopRepository.delete(stop)
        resequence(dayId)
        return stopRepository.findByDayIdOrderByPosition(dayId).map { it.toResponse() }
    }

    fun listForDay(userId: UUID, dayId: UUID): List<StopResponse> {
        verifyDayOwnership(dayId, userId)
        return stopRepository.findByDayIdOrderByPosition(dayId).sortedBy { it.position }.map { it.toResponse() }
    }

    private fun verifyDayOwnership(dayId: UUID, userId: UUID) {
        val day = dayRepository.findById(dayId)
            .orElseThrow { NoSuchElementException("Day not found") }
        tripRepository.findByIdAndUserId(day.tripId, userId)
            ?: throw NoSuchElementException("Day not found")
    }

    private fun loadOwnedStop(stopId: UUID, userId: UUID): Stop {
        val stop = stopRepository.findById(stopId)
            .orElseThrow { NoSuchElementException("Stop not found") }
        verifyDayOwnership(stop.dayId, userId)
        return stop
    }

    private fun Stop.toResponse() = StopResponse(
        id = id!!, dayId = dayId, name = name, position = position,
        startTime = startTime, endTime = endTime, notes = notes,
    )

    private fun validateTimes(startTime: LocalTime?, endTime: LocalTime?) {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw IllegalArgumentException("End time must be after start time")
        }
    }

    private fun resequence(dayId: UUID) {
        val ordered = stopRepository.findByDayIdOrderByPosition(dayId)
            .sortedWith(
                compareBy<Stop, LocalTime?>(nullsLast()) { it.startTime }
                    .thenBy { it.createdAt }
            )
        // Phase 1: park at temporary negative positions to avoid unique collisions
        ordered.forEachIndexed { i, stop -> stop.position = -(i + 1) }
        stopRepository.saveAllAndFlush(ordered)
        // Phase 2: assign final 1..N
        ordered.forEachIndexed { i, stop -> stop.position = i + 1 }
        stopRepository.saveAllAndFlush(ordered)
    }
}