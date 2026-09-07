import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ErrorBanner from '../components/ErrorBanner'
import { JourneyIllustration } from '../components/Illustrations'

export default function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register(email, password, displayName)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto mt-10 max-w-sm overflow-hidden rounded-3xl border border-coral-100 bg-white shadow-warm-lg">
      <div className="bg-cream-200 px-6 pt-6">
        <JourneyIllustration />
      </div>

      <div className="p-8">
        <h1 className="mb-1 text-2xl font-bold leading-snug text-[#544541]">
          Plan trips <span className="text-coral-500">together, beautifully</span>
        </h1>
        <p className="mb-6 text-sm text-slate-500">Create an account — takes a couple of minutes.</p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-500">Display name (optional)</label>
            <input
              type="text"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-500">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
          </div>
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-500">Password</label>
            <input
              type="password"
              required
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
            <p className="mt-1 text-xs text-slate-400">At least 8 characters.</p>
          </div>

          <ErrorBanner error={error} />

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-full bg-coral-500 px-4 py-2.5 text-sm font-semibold text-white shadow-warm hover:bg-coral-600 disabled:opacity-60"
          >
            {submitting ? 'Creating account…' : 'Register'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-500">
          Already have an account? <Link to="/login" className="font-semibold text-teal-600">Log in</Link>
        </p>
      </div>
    </div>
  )
}
