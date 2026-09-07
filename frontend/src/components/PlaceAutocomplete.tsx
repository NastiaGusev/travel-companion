import { useEffect, useRef, useState } from 'react'
import { MapPin } from 'lucide-react'
import { placesApi } from '../api/places'
import type { PlaceSearchResult } from '../types'

interface Props {
  label: string
  placeholder?: string
  initialText?: string
  tripId?: string
  onSelect: (result: PlaceSearchResult | null) => void
}

/**
 * Free-text input with a debounced dropdown of Google Places predictions
 * (proxied through GET /api/places/search). Selecting a suggestion reports
 * its placeId; clearing the text reports null so the caller can drop any
 * previously-resolved place.
 *
 * The dropdown only opens while the person is actively typing a new search —
 * never just from mounting with an already-selected place, and never from
 * refocusing a field that already has a value.
 */
export default function PlaceAutocomplete({ label, placeholder, initialText, tripId, onSelect }: Props) {
  const [text, setText] = useState(initialText ?? '')
  const [results, setResults] = useState<PlaceSearchResult[]>([])
  const [open, setOpen] = useState(false)
  const [loading, setLoading] = useState(false)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const requestIdRef = useRef(0)
  const isTyping = useRef(false)

  useEffect(() => {
    if (debounceRef.current) clearTimeout(debounceRef.current)

    // Only search in response to the person actually typing — not on mount
    // (when `text` is pre-filled from an already-selected place) and not
    // right after picking a suggestion (which also changes `text`).
    if (!isTyping.current) return

    if (text.trim().length < 2) {
      setResults([])
      setOpen(false)
      return
    }

    const thisRequestId = ++requestIdRef.current
    setLoading(true)
    debounceRef.current = setTimeout(async () => {
      try {
        const found = await placesApi.search(text.trim(), tripId)
        if (requestIdRef.current === thisRequestId) {
          setResults(found)
          setOpen(true)
        }
      } catch {
        if (requestIdRef.current === thisRequestId) setResults([])
      } finally {
        if (requestIdRef.current === thisRequestId) setLoading(false)
      }
    }, 300)

    return () => {
      if (debounceRef.current) clearTimeout(debounceRef.current)
    }
  }, [text, tripId])

  return (
    <div className="relative">
      <label className="mb-1 block text-sm font-medium text-stone-500">{label}</label>
      <div className="relative">
        <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-teal-600">
          <MapPin size={15} />
        </span>
        <input
          type="text"
          value={text}
          placeholder={placeholder}
          onChange={(e) => {
            isTyping.current = true
            setText(e.target.value)
            if (e.target.value.trim() === '') onSelect(null)
          }}
          onBlur={() => setTimeout(() => setOpen(false), 150)}
          className="w-full rounded-xl border border-slate-200 bg-white py-2 pl-9 pr-3 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
        />
      </div>
      {loading && <p className="mt-1 text-xs text-slate-400">Searching…</p>}
      {open && results.length > 0 && (
        <ul className="absolute z-10 mt-1 max-h-56 w-full overflow-y-auto rounded-xl border border-slate-200 bg-white shadow-warm-lg">
          {results.map((r) => (
            <li key={r.placeId}>
              <button
                type="button"
                className="block w-full whitespace-normal break-words px-3 py-2 text-left text-sm hover:bg-coral-50"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => {
                  isTyping.current = false
                  setText(r.description)
                  setResults([])
                  setOpen(false)
                  onSelect(r)
                }}
              >
                {r.description}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
