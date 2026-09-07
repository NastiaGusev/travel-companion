import { useState, type FormEvent } from 'react'
import Modal from './Modal'
import ErrorBanner from './ErrorBanner'
import PlaceAutocomplete from './PlaceAutocomplete'
import CollaboratorsPanel from './CollaboratorsPanel'
import type { Collaborator, PlaceSearchResult, Trip, TripRequest } from '../types'

interface Props {
  trip?: Trip // omit for "create"
  collaborators?: Collaborator[]
  isOwner?: boolean
  onAddCollaborator?: (email: string) => Promise<void>
  onRemoveCollaborator?: (userId: string) => Promise<void>
  onClose: () => void
  onSubmit: (request: TripRequest) => Promise<void>
}

export default function TripFormModal({
  trip,
  collaborators,
  isOwner,
  onAddCollaborator,
  onRemoveCollaborator,
  onClose,
  onSubmit,
}: Props) {
  const [title, setTitle] = useState(trip?.title ?? '')
  const [startDate, setStartDate] = useState(trip?.startDate ?? '')
  const [endDate, setEndDate] = useState(trip?.endDate ?? '')
  const [description, setDescription] = useState(trip?.description ?? '')
  const [destination, setDestination] = useState<PlaceSearchResult | null>(
    trip?.destination?.placeId ? { placeId: trip.destination.placeId, description: trip.destination.name ?? '' } : null,
  )
  const [destinationText, setDestinationText] = useState(trip?.destination?.name ?? '')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await onSubmit({
        title,
        startDate: startDate || null,
        endDate: endDate || null,
        description: description || null,
        destinationName: destination?.description || destinationText || null,
        destinationPlaceId: destination?.placeId || null,
        version: trip?.version,
      })
    } catch (err) {
      setError(err)
      setSubmitting(false)
    }
  }

  return (
    <Modal title={trip ? 'Edit trip' : 'New trip'} onClose={onClose}>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="mb-1 block text-sm font-medium text-stone-500">Title</label>
          <input
            type="text"
            required
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
          />
        </div>

        <PlaceAutocomplete
          label="Destination"
          placeholder="Search for a city or place…"
          initialText={trip?.destination?.name ?? ''}
          onSelect={(result) => {
            setDestination(result)
            setDestinationText(result?.description ?? '')
          }}
        />

        {/* Stacked rather than side-by-side: native date pickers on phones
            (especially iOS) can render wider than a halved column and push
            past the edge of the modal, so each field always gets the full
            width instead. */}
        <div className="space-y-3">
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-500">Start date</label>
            <input
              type="date"
              value={startDate ?? ''}
              onChange={(e) => setStartDate(e.target.value)}
              className="block w-full max-w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-500">End date</label>
            <input
              type="date"
              value={endDate ?? ''}
              onChange={(e) => setEndDate(e.target.value)}
              className="block w-full max-w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
          </div>
        </div>

        <div>
          <label className="mb-1 block text-sm font-medium text-stone-500">Description</label>
          <textarea
            rows={3}
            value={description ?? ''}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
          />
        </div>

        {trip && collaborators && onAddCollaborator && onRemoveCollaborator && (
          <div className="border-t border-slate-100 pt-4">
            <CollaboratorsPanel
              collaborators={collaborators}
              isOwner={!!isOwner}
              onAdd={onAddCollaborator}
              onRemove={onRemoveCollaborator}
            />
          </div>
        )}

        <ErrorBanner error={error} />

        <div className="flex flex-wrap justify-end gap-2 pt-2">
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
            {submitting ? 'Saving…' : trip ? 'Save changes' : 'Create trip'}
          </button>
        </div>
      </form>
    </Modal>
  )
}
