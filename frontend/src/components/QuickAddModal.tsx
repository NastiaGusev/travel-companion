import { useState, type FormEvent } from 'react'
import Modal from './Modal'
import ErrorBanner from './ErrorBanner'
import type { QuickAddResponse } from '../types'

interface Props {
  onClose: () => void
  onSubmit: (text: string) => Promise<QuickAddResponse>
}

const PLACEHOLDER =
  'e.g. Day 1: land at 3pm, check into the hotel, dinner at an old city restaurant around 7. Day 2: the market in the morning, a museum in the afternoon.'

export default function QuickAddModal({ onClose, onSubmit }: Props) {
  const [text, setText] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)
  const [result, setResult] = useState<QuickAddResponse | null>(null)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      setResult(await onSubmit(text))
    } catch (err) {
      setError(err)
    } finally {
      setSubmitting(false)
    }
  }

  const totalStops = result?.days.reduce((sum, d) => sum + d.stops.length, 0) ?? 0

  return (
    <Modal title="Quick add with AI" onClose={onClose}>
      {result ? (
        <div className="space-y-4">
          <p className="text-sm text-ink">
            {result.days.length === 0
              ? "Couldn't turn that into anything concrete — try mentioning specific times or places."
              : `Added ${result.days.length} day${result.days.length === 1 ? '' : 's'} and ${totalStops} stop${totalStops === 1 ? '' : 's'} to your itinerary.`}
          </p>
          <div className="flex justify-end">
            <button
              onClick={onClose}
              className="rounded-full bg-coral-500 px-5 py-2 text-sm font-semibold text-white shadow-warm hover:bg-coral-600"
            >
              Done
            </button>
          </div>
        </div>
      ) : (
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-500">Describe your plans in plain text</label>
            <textarea
              rows={5}
              required
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder={PLACEHOLDER}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
            <p className="mt-1 text-xs text-slate-400">
              This turns your text into itinerary days and stops automatically, resolving each place along the way.
            </p>
          </div>

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
              disabled={submitting || !text.trim()}
              className="rounded-full bg-coral-500 px-5 py-2 text-sm font-semibold text-white shadow-warm hover:bg-coral-600 disabled:opacity-60"
            >
              {submitting ? 'Thinking…' : 'Generate'}
            </button>
          </div>
        </form>
      )}
    </Modal>
  )
}
