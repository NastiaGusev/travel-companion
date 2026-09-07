import { apiRequest } from './client'
import type { QuickAddResponse } from '../types'

export const quickAddApi = {
  // Backend resolves this synchronously: it calls the ai-service to turn
  // free text into days/stops, resolves each stop against Google Places,
  // and persists everything to the trip in one transaction — the response
  // is the actual created rows, not a preview to confirm.
  extract: (tripId: string, text: string) =>
    apiRequest<QuickAddResponse>(`/api/trips/${tripId}/quick-add`, {
      method: 'POST',
      body: { text },
    }),
}
