import { useState, type FormEvent } from 'react'
import Modal from './Modal'
import ErrorBanner from './ErrorBanner'
import PlaceAutocomplete from './PlaceAutocomplete'
import type { PlaceSearchResult, Stop, StopRequest } from '../types'

interface Props {
  stop?: Stop // omit for "create"
  tripId: string
  onClose: () => void
  onSubmit: (request: StopRequest) => Promise<void>
}

export default function StopFormModal({ stop, tripId, onClose, onSubmit }: Props) {
  const [title, setTitle] = useState(stop?.title ?? '')
  const [startTime, setStartTime] = useState(stop?.startTime?.slice(0, 5) ?? '')
  const [endTime, setEndTime] = useState(stop?.endTime?.slice(0, 5) ?? '')
  const [notes, setNotes] = useState(stop?.notes ?? '')
  const [place, setPlace] = useState<PlaceSearchResult | null>(
    stop?.place?.placeId ? { placeId: stop.place.placeId, description: stop.place.name ?? '' } : null,
  )
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    if (!title.trim() && !place) {
      setError(new Error('Give the stop a name, or pick a place.'))
      return
    }

    setSubmitting(true)
    try {
      await onSubmit({
        title: title.trim() || null,
        startTime: startTime ? `${startTime}:00` : null,
        endTime: endTime ? `${endTime}:00` : null,
        notes: notes || null,
        placeId: place?.placeId ?? null,
      })
    } catch (err) {
      setError(err)
      setSubmitting(false)
    }
  }

  return (
    <Modal title={stop ? 'Edit stop' : 'Add a stop'} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <PlaceAutocomplete
          label="Place"
          placeholder="Search for a restaurant, sight, hotel…"
          initialText={stop?.place?.name ?? ''}
          tripId={tripId}
          onSelect={setPlace}
        />

        <div>
          <label className="mb-1 block text-sm font-medium text-stone-500">
            Title {place ? '(optional — defaults to place name)' : ''}
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
          />
        </div>

        {/* Stacked rather than side-by-side: native time pickers on phones
            (especially iOS) can render wider than a halved column and push
            past the edge of the modal, so each field always gets the full
            width instead. */}
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-500">Start time</label>
            <input
              type="time"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              className="block w-full max-w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-500">End time</label>
            <input
              type="time"
              value={endTime}
              onChange={(e) => setEndTime(e.target.value)}
              className="block w-full max-w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
          </div>
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-stone-500">Notes</label>
          <textarea
            rows={2}
            value={notes ?? ''}
            onChange={(e) => setNotes(e.target.value)}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
          />
        </div>

        <ErrorBanner error={error} />

        <div className="flex justify-end gap-2 pt-2">
          <button
            type="button"
            onClick={onClose}
            className="rounded-full border border-slate-200 px-4 py-2 text-sm font-medium text-slate-600 hover:bg-slate-50"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={submitting}
            className="rounded-full bg-coral-500 px-5 py-2 text-sm font-semibold text-white shadow-warm hover:bg-coral-600 disabled:opacity-60"
          >
            {submitting ? 'Saving…' : stop ? 'Save changes' : 'Add stop'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
