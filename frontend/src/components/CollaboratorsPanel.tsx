import { useState, type FormEvent } from 'react'
import { Crown } from 'lucide-react'
import type { Collaborator } from '../types'
import ErrorBanner from './ErrorBanner'

interface Props {
  collaborators: Collaborator[]
  isOwner: boolean
  onAdd: (email: string) => Promise<void>
  onRemove: (userId: string) => Promise<void>
}

function RoleBadge({ role }: { role: string }) {
  if (role === 'OWNER') {
    return (
      <span className="flex items-center gap-1 rounded-full bg-gold-500 px-2.5 py-0.5 text-xs font-semibold text-cream-100">
        <Crown size={12} /> Owner
      </span>
    )
  }
  return <span className="rounded-full bg-teal-600 px-2.5 py-0.5 text-xs font-semibold text-cream-100">Editor</span>
}

export default function CollaboratorsPanel({ collaborators, isOwner, onAdd, onRemove }: Props) {
  const [email, setEmail] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleAdd(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await onAdd(email)
      setEmail('')
    } catch (err) {
      setError(err)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="rounded-2xl border border-coral-100 bg-white p-4 shadow-sm">
      <h3 className="mb-3 text-base font-bold text-ink">Trip Collaborators</h3>
      <ul className="space-y-2.5">
        {collaborators.map((c) => (
          <li key={c.userId} className="flex items-center justify-between gap-2 text-sm">
            <span className="flex min-w-0 items-center gap-2 text-slate-700">
              <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-coral-100 text-xs font-semibold text-coral-700">
                {c.email[0]?.toUpperCase()}
              </span>
              <span className="min-w-0">
                <span className="block truncate font-medium text-ink">{c.email}</span>
              </span>
              <RoleBadge role={c.role} />
            </span>
            {isOwner && c.role !== 'OWNER' && (
              <button onClick={() => onRemove(c.userId)} className="shrink-0 text-xs font-semibold text-coral-500 hover:text-coral-700">
                Remove
              </button>
            )}
          </li>
        ))}
        {collaborators.length === 0 && <li className="text-sm text-slate-400">No collaborators yet.</li>}
      </ul>

      {isOwner && (
        <form onSubmit={handleAdd} className="mt-3 flex gap-2">
          <input
            type="email"
            required
            placeholder="Invite by email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="min-w-0 flex-1 rounded-full border border-slate-200 px-3 py-1.5 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
          />
          <button
            type="submit"
            disabled={submitting}
            className="shrink-0 rounded-full bg-coral-500 px-4 py-1.5 text-sm font-semibold text-white shadow-warm hover:bg-coral-600 disabled:opacity-60"
          >
            {submitting ? 'Adding…' : 'Add'}
          </button>
        </form>
      )}
      <div className="mt-2">
        <ErrorBanner error={error} />
      </div>
    </div>
  )
}
