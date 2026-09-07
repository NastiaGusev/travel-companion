import { apiRequest } from './client'
import type { Stop, StopRequest } from '../types'

export const stopsApi = {
  list: (dayId: string) => apiRequest<Stop[]>(`/api/days/${dayId}/stops`),
  create: (dayId: string, request: StopRequest) =>
    apiRequest<Stop>(`/api/days/${dayId}/stops`, { method: 'POST', body: request }),
  update: (stopId: string, request: StopRequest) =>
    apiRequest<Stop>(`/api/stops/${stopId}`, { method: 'PUT', body: request }),
  remove: (stopId: string) => apiRequest<Stop[]>(`/api/stops/${stopId}`, { method: 'DELETE' }),
}
