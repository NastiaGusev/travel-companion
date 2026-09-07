/**
 * Small hand-drawn line-art illustrations, built as plain inline SVG so the
 * app never depends on external art or emoji. Flat fills only (no gradients),
 * using the coral / teal / gold palette.
 */

export function JourneyIllustration({ className = 'h-24 w-full' }: { className?: string }) {
  return (
    <svg viewBox="0 0 320 120" className={className} fill="none" xmlns="http://www.w3.org/2000/svg">
      {/* sun */}
      <circle cx="272" cy="30" r="16" fill="#FFC857" />
      {/* hills */}
      <path d="M0 92 C 50 60, 90 60, 140 92 C 180 116, 230 116, 260 92 L320 92 L320 120 L0 120 Z" fill="#0F6B66" opacity="0.16" />
      <path d="M0 104 C 60 78, 120 78, 170 104 C 210 124, 270 124, 320 100 L320 120 L0 120 Z" fill="#FF6B4A" opacity="0.14" />
      {/* dashed flight path */}
      <path
        d="M18 78 C 70 20, 150 96, 210 40 C 235 16, 255 18, 268 30"
        stroke="#FF6B4A"
        strokeWidth="2.5"
        strokeDasharray="1 9"
        strokeLinecap="round"
      />
      {/* paper plane */}
      <g transform="translate(255 18) rotate(28)">
        <path d="M0 10 L26 0 L4 22 L2 15 L-9 12 Z" fill="#2B2B2B" />
        <path d="M0 10 L14 5 L4 22 Z" fill="#F04E2B" />
      </g>
    </svg>
  )
}

export function EmptyTripsIllustration({ className = 'mx-auto h-28 w-28' }: { className?: string }) {
  return (
    <svg viewBox="0 0 120 120" className={className} fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="60" cy="60" r="56" fill="#FFF3E4" />
      {/* suitcase */}
      <rect x="32" y="52" width="56" height="38" rx="6" fill="#FF6B4A" />
      <rect x="32" y="52" width="56" height="38" rx="6" stroke="#C93B1E" strokeWidth="2" />
      <rect x="48" y="42" width="24" height="12" rx="4" fill="none" stroke="#C93B1E" strokeWidth="3" />
      <line x1="60" y1="52" x2="60" y2="90" stroke="#C93B1E" strokeWidth="2" />
      <circle cx="42" cy="96" r="4" fill="#2B2B2B" />
      <circle cx="78" cy="96" r="4" fill="#2B2B2B" />
      {/* tag */}
      <path d="M84 58 L96 46 L104 54 L92 66 Z" fill="#FFC857" />
      <circle cx="98.5" cy="51.5" r="2" fill="#7A2716" />
    </svg>
  )
}

export function CompassIllustration({ className = 'mx-auto h-24 w-24' }: { className?: string }) {
  return (
    <svg viewBox="0 0 120 120" className={className} fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="60" cy="60" r="46" fill="#EDF7F6" stroke="#0F6B66" strokeWidth="3" />
      <circle cx="60" cy="60" r="4" fill="#0F6B66" />
      <path d="M60 30 L70 55 L60 62 L50 55 Z" fill="#FF6B4A" />
      <path d="M60 90 L50 65 L60 58 L70 65 Z" fill="#0F6B66" />
    </svg>
  )
}
