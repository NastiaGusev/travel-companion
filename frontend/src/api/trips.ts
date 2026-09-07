import { apiRequest } from './client'
import type { Trip, TripRequest } from '../types'

export const tripsApi = {
  list: () => apiRequest<Trip[]>('/api/trips'),
  get: (id: string) => apiRequest<Trip>(`/api/trips/${id}`),
  create: (request: TripRequest) =>
    apiRequest<Trip>('/api/trips', { method: 'POST', body: request }),
  update: (id: string, request: TripRequest) =>
    apiRequest<Trip>(`/api/trips/${id}`, { method: 'PUT', body: request }),
  remove: (id: string) => apiRequest<void>(`/api/trips/${id}`, { method: 'DELETE' }),
}
