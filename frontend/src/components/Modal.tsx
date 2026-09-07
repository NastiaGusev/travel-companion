import type { ReactNode } from 'react'
import { X } from 'lucide-react'

export default function Modal({
  title,
  onClose,
  children,
}: {
  title: string
  onClose: () => void
  children: ReactNode
}) {
  return (
    // The overlay itself is the scroll container (not the box inside it).
    // A fixed-position box with a vh-based max-height looks fine until the
    // content is taller than the visible screen — on mobile that clips it
    // with no way to reach the rest. Making the overlay scroll, and letting
    // the box size to its natural content height, means a short popup
    // still centers normally and a tall one just scrolls like a page.
    <div className="fixed inset-0 z-50 overflow-y-auto bg-ink/40" onClick={onClose}>
      <div className="flex min-h-full items-center justify-center p-2 sm:p-4">
        <div
          className="w-full min-w-0 max-w-lg overflow-x-hidden rounded-2xl bg-white p-4 shadow-warm-lg sm:p-6"
          onClick={(e) => e.stopPropagation()}
        >
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-ink">{title}</h2>
            <button
              onClick={onClose}
              className="rounded-full p-1 text-slate-400 hover:bg-coral-50 hover:text-coral-600"
              aria-label="Close"
            >
              <X size={18} />
            </button>
          </div>
          {children}
        </div>
      </div>
    </div>
  )
}
