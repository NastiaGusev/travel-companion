import { useState } from 'react'
import { ArrowDown, ArrowUp, Trash2 } from 'lucide-react'
import type { ItineraryDay, Stop } from '../types'
import StopItem from './StopItem'

interface Props {
  day: ItineraryDay
  stops: Stop[] | undefined // undefined while loading
  isFirst: boolean
  isLast: boolean
  onMoveUp: () => void
  onMoveDown: () => void
  onDelete: () => void
  onSaveNotes: (notes: string) => Promise<void>
  onAddStop: () => void
  onEditStop: (stop: Stop) => void
  onDeleteStop: (stop: Stop) => void
}

/**
 * A single day, shown as a full-width card in a vertical stack (the whole
 * itinerary scrolls like a normal list — no tabs, no hidden state). Every
 * action a user can take on the day (reorder, delete) is a plainly labeled
 * button, not a bare icon, so it's obvious what it does at a glance.
 */
export default function DayCard({
  day,
  stops,
  isFirst,
  isLast,
  onMoveUp,
  onMoveDown,
  onDelete,
  onSaveNotes,
  onAddStop,
  onEditStop,
  onDeleteStop,
}: Props) {
  const [editingNotes, setEditingNotes] = useState(false)
  const [notesDraft, setNotesDraft] = useState(day.notes ?? '')
  const [savingNotes, setSavingNotes] = useState(false)

  async function saveNotes() {
    setSavingNotes(true)
    try {
      await onSaveNotes(notesDraft)
      setEditingNotes(false)
    } finally {
      setSavingNotes(false)
    }
  }

  return (
    <div className="rounded-2xl border border-coral-100 bg-white p-5 shadow-sm">
      <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-3">
          <span
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-coral-500 text-sm font-bold text-white"
            aria-label={`Day ${day.dayNumber}`}
          >
            {day.dayNumber}
          </span>
          {day.dayDate && <p className="text-sm font-semibold text-[#a6968e]">{day.dayDate}</p>}
        </div>

        <div className="flex gap-1.5">
          <button
            disabled={isFirst}
            onClick={onMoveUp}
            title="Swap places with the day before this one"
            className="flex items-center gap-1 rounded-full border border-slate-200 px-2.5 py-1 text-xs font-medium text-slate-600 hover:border-coral-300 hover:text-coral-700 disabled:cursor-not-allowed disabled:opacity-30"
          >
            <ArrowUp size={13} /> Earlier
          </button>
          <button
            disabled={isLast}
            onClick={onMoveDown}
            title="Swap places with the day after this one"
            className="flex items-center gap-1 rounded-full border border-slate-200 px-2.5 py-1 text-xs font-medium text-slate-600 hover:border-coral-300 hover:text-coral-700 disabled:cursor-not-allowed disabled:opacity-30"
          >
            <ArrowDown size={13} /> Later
          </button>
        </div>
      </div>

      {editingNotes ? (
        <div className="mb-4">
          <textarea
            rows={2}
            value={notesDraft}
            onChange={(e) => setNotesDraft(e.target.value)}
            className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            autoFocus
          />
          <div className="mt-1 flex gap-3 text-xs">
            <button onClick={saveNotes} disabled={savingNotes} className="font-semibold text-coral-600">
              {savingNotes ? 'Saving…' : 'Save'}
            </button>
            <button
              onClick={() => {
                setNotesDraft(day.notes ?? '')
                setEditingNotes(false)
              }}
              className="text-slate-500"
            >
              Cancel
            </button>
          </div>
        </div>
      ) : (
        <button
          onClick={() => setEditingNotes(true)}
          className="mb-4 min-h-[1.5rem] text-left text-sm text-[#374750] hover:text-ink"
        >
          {day.notes || <span className="italic text-slate-400">Add notes…</span>}
        </button>
      )}

      <ul className="space-y-2.5">
        {stops === undefined && <li className="text-sm text-slate-400">Loading stops…</li>}
        {stops?.length === 0 && <li className="text-sm italic text-slate-400">No stops yet.</li>}
        {stops?.map((stop) => (
          <StopItem key={stop.id} stop={stop} onEdit={() => onEditStop(stop)} onDelete={() => onDeleteStop(stop)} />
        ))}
      </ul>

      <button
        onClick={onAddStop}
        className="mt-4 w-full rounded-xl border-2 border-dashed border-coral-200 py-2 text-sm font-semibold text-coral-500 hover:border-coral-400 hover:bg-coral-50 hover:text-coral-600"
      >
        + Add stop
      </button>

      <div className="mt-4 flex justify-end border-t border-slate-100 pt-3">
        <button
          onClick={onDelete}
          className="flex items-center gap-1 rounded-full px-2.5 py-1 text-xs font-medium text-slate-400 hover:bg-red-50 hover:text-red-600"
        >
          <Trash2 size={13} /> Delete this day
        </button>
      </div>
    </div>
  )
}
