import { apiRequest } from './client'
import type { PlaceSearchResult } from '../types'

export const placesApi = {
  search: (query: string, tripId?: string) =>
    apiRequest<PlaceSearchResult[]>('/api/places/search', { query: { query, tripId } }),
}
