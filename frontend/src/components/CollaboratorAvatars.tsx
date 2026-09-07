import type { Collaborator } from '../types'

// Deterministic per-person color so the same collaborator always gets the
// same avatar color across reloads (picked from their own user id).
// Kept away from orange/coral and green/teal so collaborators are never
// confused with the app's primary accent or the account avatar's green.
const AVATAR_COLORS = ['bg-blue-500', 'bg-purple-500', 'bg-pink-500', 'bg-indigo-500', 'bg-sky-600', 'bg-fuchsia-500']

function colorForUser(id: string): string {
  const sum = [...id].reduce((acc, ch) => acc + ch.charCodeAt(0), 0)
  return AVATAR_COLORS[sum % AVATAR_COLORS.length]
}

/** Small overlapping initials avatars — a quick "who's on this trip" glance. */
export default function CollaboratorAvatars({ collaborators }: { collaborators: Collaborator[] }) {
  if (collaborators.length === 0) return null

  return (
    <div className="flex -space-x-2">
      {collaborators.map((c) => (
        <span
          key={c.userId}
          title={`${c.email}${c.role === 'OWNER' ? ' · Owner' : ''}`}
          className={`flex h-8 w-8 shrink-0 items-center justify-center rounded-full border-2 border-white text-xs font-semibold text-white ${colorForUser(c.userId)}`}
        >
          {c.email[0]?.toUpperCase()}
        </span>
      ))}
    </div>
  )
}
