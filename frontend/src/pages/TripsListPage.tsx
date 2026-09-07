import { useEffect, useState } from 'react'
import { Plus } from 'lucide-react'
import { tripsApi } from '../api/trips'
import { collaboratorsApi } from '../api/collaborators'
import { useAuth } from '../context/AuthContext'
import type { Collaborator, Trip, TripRequest } from '../types'
import TripCard from '../components/TripCard'
import TripFormModal from '../components/TripFormModal'
import ErrorBanner from '../components/ErrorBanner'
import Spinner from '../components/Spinner'
import { EmptyTripsIllustration } from '../components/Illustrations'

export default function TripsListPage() {
  const { user } = useAuth()
  const [trips, setTrips] = useState<Trip[] | null>(null)
  const [collaboratorsByTrip, setCollaboratorsByTrip] = useState<Record<string, Collaborator[]>>({})
  const [error, setError] = useState<unknown>(null)
  const [modal, setModal] = useState<'create' | Trip | null>(null)

  function isOwnerOf(tripId: string): boolean {
    return (collaboratorsByTrip[tripId] ?? []).some((c) => c.userId === user?.userId && c.role === 'OWNER')
  }

  async function load() {
    setError(null)
    try {
      const loadedTrips = await tripsApi.list()
      setTrips(loadedTrips)
      const entries = await Promise.all(
        loadedTrips.map(async (t) => [t.id, await collaboratorsApi.list(t.id)] as const),
      )
      setCollaboratorsByTrip(Object.fromEntries(entries))
    } catch (err) {
      setError(err)
    }
  }

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  async function handleCreate(request: TripRequest) {
    const created = await tripsApi.create(request)
    setTrips((prev) => [...(prev ?? []), created])
    setModal(null)
    load()
  }

  async function handleUpdate(id: string, request: TripRequest) {
    const updated = await tripsApi.update(id, request)
    setTrips((prev) => prev?.map((t) => (t.id === id ? updated : t)) ?? null)
    setModal(null)
  }

  async function handleDelete(trip: Trip) {
    if (!confirm(`Delete "${trip.title}"? This can't be undone.`)) return
    try {
      await tripsApi.remove(trip.id)
      setTrips((prev) => prev?.filter((t) => t.id !== trip.id) ?? null)
    } catch (err) {
      setError(err)
    }
  }

  async function handleAddCollaborator(tripId: string, email: string) {
    const created = await collaboratorsApi.add(tripId, email)
    setCollaboratorsByTrip((prev) => ({ ...prev, [tripId]: [...(prev[tripId] ?? []), created] }))
  }

  async function handleRemoveCollaborator(tripId: string, targetUserId: string) {
    if (!confirm('Remove this collaborator?')) return
    await collaboratorsApi.remove(tripId, targetUserId)
    setCollaboratorsByTrip((prev) => ({
      ...prev,
      [tripId]: (prev[tripId] ?? []).filter((c) => c.userId !== targetUserId),
    }))
  }

  return (
    <div>
      <h1 className="mb-6 text-2xl font-bold text-ink">Your trips</h1>

      <ErrorBanner error={error} />

      {trips === null && !error && <Spinner label="Loading trips…" />}

      {trips !== null && trips.length === 0 && (
        <div className="rounded-2xl border-2 border-dashed border-coral-200 bg-white p-10 text-center text-slate-500">
          <EmptyTripsIllustration />
          <p className="mt-3">No trips yet. Tap the button below to plan your first one.</p>
        </div>
      )}

      {trips !== null && trips.length > 0 && (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {trips.map((trip) => (
            <TripCard
              key={trip.id}
              trip={trip}
              isOwner={isOwnerOf(trip.id)}
              onEdit={() => setModal(trip)}
              onDelete={() => handleDelete(trip)}
            />
          ))}
        </div>
      )}

      <button
        onClick={() => setModal('create')}
        aria-label="New trip"
        title="New trip"
        className="fixed bottom-6 right-6 z-30 flex h-14 w-14 items-center justify-center rounded-full bg-coral-500 text-white shadow-warm-lg transition hover:scale-105 hover:bg-coral-600"
      >
        <Plus size={26} />
      </button>

      {modal === 'create' && <TripFormModal onClose={() => setModal(null)} onSubmit={handleCreate} />}
      {modal && modal !== 'create' && (
        <TripFormModal
          trip={modal}
          collaborators={collaboratorsByTrip[modal.id]}
          isOwner={isOwnerOf(modal.id)}
          onAddCollaborator={(email) => handleAddCollaborator(modal.id, email)}
          onRemoveCollaborator={(userId) => handleRemoveCollaborator(modal.id, userId)}
          onClose={() => setModal(null)}
          onSubmit={(req) => handleUpdate(modal.id, req)}
        />
      )}
    </div>
  )
}
