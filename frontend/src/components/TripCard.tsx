import { Link } from 'react-router-dom'
import { CalendarDays, MapPin, Pencil, Trash2 } from 'lucide-react'
import type { Trip } from '../types'

function formatRange(start: string | null, end: string | null): string | null {
  if (!start && !end) return null
  if (start && end) return `${start} → ${end}`
  return start ?? end
}

// No real cover photos yet, so give each trip a flat, deterministic accent
// color instead of a photo — picked from the trip's own id so it's stable
// across reloads rather than random.
const COVER_COLORS = ['bg-coral-500', 'bg-teal-600', 'bg-gold-500']

function colorFor(id: string): string {
  const sum = [...id].reduce((acc, ch) => acc + ch.charCodeAt(0), 0)
  return COVER_COLORS[sum % COVER_COLORS.length]
}

export default function TripCard({
  trip,
  isOwner,
  onEdit,
  onDelete,
}: {
  trip: Trip
  isOwner: boolean
  onEdit: () => void
  onDelete: () => void
}) {
  const range = formatRange(trip.startDate, trip.endDate)

  return (
    <div className="group overflow-hidden rounded-2xl border border-coral-100 bg-white shadow-warm transition hover:shadow-warm-lg">
      <Link to={`/trips/${trip.id}`} className="block">
        <div className={`flex h-24 items-end ${colorFor(trip.id)} p-4`}>
          <h3 className="truncate text-lg font-bold text-white">{trip.title}</h3>
        </div>
        <div className="p-4">
          {trip.destination?.name && (
            <p className="flex items-center gap-1.5 text-sm text-teal-700">
              <MapPin size={14} /> {trip.destination.name}
            </p>
          )}
          {range && (
            <p className="mt-1 flex items-center gap-1.5 text-sm text-slate-500">
              <CalendarDays size={14} /> {range}
            </p>
          )}
          {trip.description && <p className="mt-2 line-clamp-2 text-sm text-slate-600">{trip.description}</p>}
        </div>
      </Link>
      <div className="flex items-center justify-end gap-1 border-t border-coral-50 px-2 py-1.5">
        <button
          onClick={onEdit}
          aria-label="Edit trip"
          title="Edit trip"
          className="rounded-full p-2 text-coral-600 hover:bg-coral-50 hover:text-coral-700"
        >
          <Pencil size={16} />
        </button>
        {isOwner && (
          <button
            onClick={onDelete}
            aria-label="Delete trip"
            title="Delete trip"
            className="rounded-full p-2 text-slate-400 hover:bg-red-50 hover:text-red-600"
          >
            <Trash2 size={16} />
          </button>
        )}
      </div>
    </div>
  )
}
