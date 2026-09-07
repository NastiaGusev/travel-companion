import { Link } from 'react-router-dom'
import { CompassIllustration } from '../components/Illustrations'

export default function NotFoundPage() {
  return (
    <div className="mx-auto mt-24 max-w-md text-center">
      <CompassIllustration />
      <h1 className="mt-4 text-2xl font-bold text-ink">Page not found</h1>
      <p className="mt-2 text-slate-500">That trip or page doesn't exist, or you don't have access to it.</p>
      <Link to="/" className="mt-4 inline-block font-semibold text-coral-600">
        Back to trips
      </Link>
    </div>
  )
}
