/**
 * Original app mark — a rounded coral badge with a two-tone compass needle,
 * built as inline SVG so it stays crisp at any size and needs no external
 * image or icon library.
 */
export default function Logo({ size = 28, className = '' }: { size?: number; className?: string }) {
  return (
    <svg
      viewBox="0 0 32 32"
      width={size}
      height={size}
      className={className}
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect width="32" height="32" rx="9" fill="#FF6B4A" />
      <circle cx="16" cy="16" r="10.5" fill="#FFF9F2" />
      <circle cx="16" cy="16" r="10.5" fill="none" stroke="#FFC857" strokeWidth="1.4" />
      <path d="M16 7.5 L19.5 16 L16 14.6 L12.5 16 Z" fill="#0F6B66" />
      <path d="M16 24.5 L12.5 16 L16 17.4 L19.5 16 Z" fill="#2B2B2B" />
      <circle cx="16" cy="16" r="1.8" fill="#FFC857" />
    </svg>
  )
}
