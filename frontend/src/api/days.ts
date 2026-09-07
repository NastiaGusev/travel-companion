import { apiRequest } from './client'
import type { DayRequest, ItineraryDay } from '../types'

export const daysApi = {
  list: (tripId: string) => apiRequest<ItineraryDay[]>(`/api/trips/${tripId}/days`),
  create: (tripId: string, request: DayRequest) =>
    apiRequest<ItineraryDay>(`/api/trips/${tripId}/days`, { method: 'POST', body: request }),
  updateNotes: (dayId: string, request: DayRequest) =>
    apiRequest<ItineraryDay>(`/api/days/${dayId}`, { method: 'PUT', body: request }),
  remove: (dayId: string) => apiRequest<ItineraryDay[]>(`/api/days/${dayId}`, { method: 'DELETE' }),
  swap: (tripId: string, dayIdA: string, dayIdB: string) =>
    apiRequest<ItineraryDay[]>(`/api/trips/${tripId}/days/swap`, {
      method: 'POST',
      body: { dayIdA, dayIdB },
    }),
}
