import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, CalendarDays, MapPin, Pencil, Sparkles } from 'lucide-react'
import { tripsApi } from '../api/trips'
import { daysApi } from '../api/days'
import { stopsApi } from '../api/stops'
import { collaboratorsApi } from '../api/collaborators'
import { quickAddApi } from '../api/quickAdd'
import { useAuth } from '../context/AuthContext'
import type { Collaborator, ItineraryDay, Stop, Trip, TripRequest } from '../types'
import { ApiError } from '../api/client'
import ErrorBanner from '../components/ErrorBanner'
import Spinner from '../components/Spinner'
import DayCard from '../components/DayCard'
import StopFormModal from '../components/StopFormModal'
import TripFormModal from '../components/TripFormModal'
import CollaboratorAvatars from '../components/CollaboratorAvatars'
import QuickAddModal from '../components/QuickAddModal'

export default function TripDetailPage() {
  const { tripId } = useParams<{ tripId: string }>()
  const navigate = useNavigate()
  const { user } = useAuth()

  const [trip, setTrip] = useState<Trip | null>(null)
  const [days, setDays] = useState<ItineraryDay[] | null>(null)
  const [stopsByDay, setStopsByDay] = useState<Record<string, Stop[]>>({})
  const [collaborators, setCollaborators] = useState<Collaborator[]>([])
  const [error, setError] = useState<unknown>(null)
  const [notFound, setNotFound] = useState(false)

  const [editingTrip, setEditingTrip] = useState(false)
  const [stopModal, setStopModal] = useState<{ dayId: string; stop?: Stop } | null>(null)
  const [quickAddOpen, setQuickAddOpen] = useState(false)

  const isOwner = collaborators.some((c) => c.userId === user?.userId && c.role === 'OWNER')

  async function loadAll() {
    if (!tripId) return
    setError(null)
    try {
      const [loadedTrip, loadedDays, loadedCollaborators] = await Promise.all([
        tripsApi.get(tripId),
        daysApi.list(tripId),
        collaboratorsApi.list(tripId),
      ])
      setTrip(loadedTrip)
      setDays(loadedDays)
      setCollaborators(loadedCollaborators)

      const stopEntries = await Promise.all(
        loadedDays.map(async (day) => [day.id, await stopsApi.list(day.id)] as const),
      )
      setStopsByDay(Object.fromEntries(stopEntries))
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        setNotFound(true)
      } else {
        setError(err)
      }
    }
  }

  useEffect(() => {
    loadAll()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tripId])

  useEffect(() => {
    if (notFound) navigate('/404', { replace: true })
  }, [notFound, navigate])

  if (notFound || !tripId) return null

  async function handleUpdateTrip(request: TripRequest) {
    const updated = await tripsApi.update(tripId!, request)
    setTrip(updated)
    setEditingTrip(false)
  }

  async function handleAddDay() {
    try {
      const created = await daysApi.create(tripId!, {})
      setDays((prev) => [...(prev ?? []), created])
      setStopsByDay((prev) => ({ ...prev, [created.id]: [] }))
    } catch (err) {
      setError(err)
    }
  }

  async function handleDeleteDay(day: ItineraryDay) {
    if (!confirm(`Delete Day ${day.dayNumber}? Its stops will be removed too.`)) return
    try {
      const updated = await daysApi.remove(day.id)
      setDays(updated)
      setStopsByDay((prev) => {
        const next = { ...prev }
        delete next[day.id]
        return next
      })
    } catch (err) {
      setError(err)
    }
  }

  async function handleSwap(dayA: ItineraryDay, dayB: ItineraryDay) {
    try {
      const updated = await daysApi.swap(tripId!, dayA.id, dayB.id)
      setDays(updated)
    } catch (err) {
      setError(err)
    }
  }

  async function handleSaveDayNotes(day: ItineraryDay, notes: string) {
    const updated = await daysApi.updateNotes(day.id, { notes, version: day.version })
    setDays((prev) => prev?.map((d) => (d.id === day.id ? updated : d)) ?? null)
  }

  async function refreshStopsForDay(dayId: string) {
    const fresh = await stopsApi.list(dayId)
    setStopsByDay((prev) => ({ ...prev, [dayId]: fresh }))
  }

  async function handleDeleteStop(dayId: string, stop: Stop) {
    if (!confirm(`Delete "${stop.title}"?`)) return
    try {
      const updated = await stopsApi.remove(stop.id)
      setStopsByDay((prev) => ({ ...prev, [dayId]: updated }))
    } catch (err) {
      setError(err)
    }
  }

  async function handleQuickAdd(text: string) {
    const result = await quickAddApi.extract(tripId!, text)
    await loadAll() // quick-add persists directly, so just re-sync days/stops with the server
    return result
  }

  async function handleAddCollaborator(email: string) {
    const created = await collaboratorsApi.add(tripId!, email)
    setCollaborators((prev) => [...prev, created])
  }

  async function handleRemoveCollaborator(targetUserId: string) {
    if (!confirm('Remove this collaborator?')) return
    await collaboratorsApi.remove(tripId!, targetUserId)
    setCollaborators((prev) => prev.filter((c) => c.userId !== targetUserId))
  }

  return (
    <div>
      <Link to="/" className="flex items-center gap-1 text-sm font-semibold text-coral-600">
        <ArrowLeft size={15} /> All trips
      </Link>

      <ErrorBanner error={error} />

      {!trip && !error && <Spinner label="Loading trip…" />}

      {trip && (
        <>
          <div className="mt-3 mb-6 overflow-hidden rounded-2xl border border-coral-100 bg-white shadow-sm">
            <div className="flex flex-wrap items-start justify-between gap-4 bg-coral-500 p-5 text-white">
              <div>
                <h1 className="text-2xl font-bold">{trip.title}</h1>
                <p className="mt-1 flex flex-wrap items-center gap-x-3 gap-y-1 text-sm text-white/90">
                  {trip.destination?.name && (
                    <span className="flex items-center gap-1">
                      <MapPin size={14} /> {trip.destination.name}
                    </span>
                  )}
                  {trip.startDate && trip.endDate && (
                    <span className="flex items-center gap-1">
                      <CalendarDays size={14} /> {trip.startDate} → {trip.endDate}
                    </span>
                  )}
                </p>
              </div>
              <button
                onClick={() => setEditingTrip(true)}
                className="flex items-center gap-1.5 rounded-full border border-white/60 px-3 py-1.5 text-sm font-medium text-white hover:bg-white/10"
              >
                <Pencil size={14} /> Edit trip
              </button>
            </div>
            {trip.description && <p className="p-4 text-sm text-slate-600">{trip.description}</p>}
            {collaborators.length > 0 && (
              <div className="flex items-center gap-2 border-t border-coral-50 px-4 py-3">
                <CollaboratorAvatars collaborators={collaborators} />
                <span className="text-xs text-slate-500">
                  {collaborators.length} {collaborators.length === 1 ? 'person' : 'people'} on this trip
                </span>
              </div>
            )}
          </div>

          <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
            <h2 className="text-lg font-bold text-ink">Itinerary</h2>
            <div className="flex gap-2">
              <button
                onClick={() => setQuickAddOpen(true)}
                className="flex items-center gap-1.5 rounded-full border border-gold-500 px-4 py-1.5 text-sm font-semibold text-gold-600 hover:bg-gold-50"
              >
                <Sparkles size={15} /> Quick add with AI
              </button>
              <button
                onClick={handleAddDay}
                className="rounded-full bg-coral-500 px-4 py-1.5 text-sm font-semibold text-white shadow-warm hover:bg-coral-600"
              >
                + Add day
              </button>
            </div>
          </div>

          {days === null && <Spinner label="Loading itinerary…" />}
          {days?.length === 0 && (
            <div className="rounded-2xl border-2 border-dashed border-coral-200 bg-white p-8 text-center text-slate-500">
              No days yet. Add the first day of this trip.
            </div>
          )}

          {days && days.length > 0 && (
            <div className="space-y-4">
              {days.map((day, index) => (
                <DayCard
                  key={day.id}
                  day={day}
                  stops={stopsByDay[day.id]}
                  isFirst={index === 0}
                  isLast={index === days.length - 1}
                  onMoveUp={() => handleSwap(day, days[index - 1])}
                  onMoveDown={() => handleSwap(day, days[index + 1])}
                  onDelete={() => handleDeleteDay(day)}
                  onSaveNotes={(notes) => handleSaveDayNotes(day, notes)}
                  onAddStop={() => setStopModal({ dayId: day.id })}
                  onEditStop={(stop) => setStopModal({ dayId: day.id, stop })}
                  onDeleteStop={(stop) => handleDeleteStop(day.id, stop)}
                />
              ))}
            </div>
          )}
        </>
      )}

      {quickAddOpen && <QuickAddModal onClose={() => setQuickAddOpen(false)} onSubmit={handleQuickAdd} />}

      {editingTrip && trip && (
        <TripFormModal
          trip={trip}
          collaborators={collaborators}
          isOwner={isOwner}
          onAddCollaborator={handleAddCollaborator}
          onRemoveCollaborator={handleRemoveCollaborator}
          onClose={() => setEditingTrip(false)}
          onSubmit={handleUpdateTrip}
        />
      )}

      {stopModal && (
        <StopFormModal
          stop={stopModal.stop}
          tripId={tripId}
          onClose={() => setStopModal(null)}
          onSubmit={async (request) => {
            if (stopModal.stop) {
              await stopsApi.update(stopModal.stop.id, request)
            } else {
              await stopsApi.create(stopModal.dayId, request)
            }
            await refreshStopsForDay(stopModal.dayId)
            setStopModal(null)
          }}
        />
      )}
    </div>
  )
}
