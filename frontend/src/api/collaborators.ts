import { apiRequest } from './client'
import type { Collaborator } from '../types'

export const collaboratorsApi = {
  list: (tripId: string) => apiRequest<Collaborator[]>(`/api/trips/${tripId}/collaborators`),
  add: (tripId: string, email: string) =>
    apiRequest<Collaborator>(`/api/trips/${tripId}/collaborators`, {
      method: 'POST',
      body: { email },
    }),
  remove: (tripId: string, targetUserId: string) =>
    apiRequest<void>(`/api/trips/${tripId}/collaborators/${targetUserId}`, { method: 'DELETE' }),
}
