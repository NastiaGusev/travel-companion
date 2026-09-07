import { Pencil, Trash2 } from 'lucide-react'
import type { Stop } from '../types'

function formatTime(t: string | null): string | null {
  return t ? t.slice(0, 5) : null
}

export default function StopItem({
  stop,
  onEdit,
  onDelete,
}: {
  stop: Stop
  onEdit: () => void
  onDelete: () => void
}) {
  const start = formatTime(stop.startTime)
  const end = formatTime(stop.endTime)

  return (
    <li className="flex items-start justify-between gap-3 rounded-xl border border-coral-100 bg-white p-3 shadow-sm">
      <div className="min-w-0">
        <div className="flex items-center gap-2 text-sm">
          {(start || end) && (
            <span className="shrink-0 rounded-full bg-teal-50 px-2 py-0.5 font-mono text-xs font-medium text-teal-700">
              {start}
              {end ? `–${end}` : ''}
            </span>
          )}
          <span className="truncate font-medium text-ink">{stop.title}</span>
        </div>
        {stop.place?.address && <p className="mt-0.5 truncate text-xs text-slate-500">{stop.place.address}</p>}
        {stop.notes && <p className="mt-1 text-sm text-slate-600">{stop.notes}</p>}
      </div>
      <div className="flex shrink-0 gap-1">
        <button onClick={onEdit} aria-label="Edit stop" title="Edit stop" className="rounded-full p-1.5 text-coral-500 hover:bg-coral-50 hover:text-coral-700">
          <Pencil size={15} />
        </button>
        <button onClick={onDelete} aria-label="Delete stop" title="Delete stop" className="rounded-full p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600">
          <Trash2 size={15} />
        </button>
      </div>
    </li>
  )
}
