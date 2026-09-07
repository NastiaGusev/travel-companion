// Mirrors the Kotlin backend's DTOs 1:1 (see src/main/kotlin/.../model/dto).

export interface AuthResponse {
  token: string
}

export interface Destination {
  name: string | null
  placeId: string | null
  latitude: number | null
  longitude: number | null
}

export interface Trip {
  id: string
  title: string
  startDate: string | null // ISO date (yyyy-MM-dd)
  endDate: string | null
  description: string | null
  destination: Destination | null
  version: number
}

export interface TripRequest {
  title: string
  startDate?: string | null
  endDate?: string | null
  description?: string | null
  destinationName?: string | null
  destinationPlaceId?: string | null
  version?: number | null
}

export interface ItineraryDay {
  id: string
  tripId: string
  dayNumber: number
  dayDate: string | null
  notes: string | null
  version: number
}

export interface DayRequest {
  notes?: string | null
  version?: number | null
}

export interface Place {
  name: string | null
  placeId: string | null
  latitude: number | null
  longitude: number | null
  address: string | null
  category: string | null
}

export interface Stop {
  id: string
  dayId: string
  title: string
  position: number
  startTime: string | null // HH:mm:ss
  endTime: string | null
  notes: string | null
  place: Place | null
}

export interface StopRequest {
  title?: string | null
  startTime?: string | null
  endTime?: string | null
  notes?: string | null
  placeId?: string | null
}

export interface PlaceSearchResult {
  placeId: string
  description: string
}

export interface Collaborator {
  userId: string
  email: string
  role: string
}

export interface QuickAddDayResult {
  dayId: string
  dayNumber: number
  stops: Stop[]
}

export interface QuickAddResponse {
  days: QuickAddDayResult[]
}

// RFC 7807 problem+json, as emitted by GlobalExceptionHandler
export interface ProblemDetail {
  type?: string
  title?: string
  status?: number
  detail?: string
  code?: string
  errors?: Record<string, string>
}
