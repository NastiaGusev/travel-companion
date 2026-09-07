import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import ErrorBanner from '../components/ErrorBanner'
import { JourneyIllustration } from '../components/Illustrations'

export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      const redirectTo = (location.state as { from?: Location })?.from?.pathname || '/'
      navigate(redirectTo, { replace: true })
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
          Plan <span className="text-coral-500">your next adventure</span>
        </h1>
        <p className="mb-6 text-sm text-slate-500">Log in to keep planning.</p>

        <form onSubmit={handleSubmit} className="space-y-4">
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
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className="w-full rounded-xl border border-slate-200 px-3 py-2 text-sm focus:border-coral-400 focus:outline-none focus:ring-1 focus:ring-coral-400"
            />
          </div>

          <ErrorBanner error={error} />

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-full bg-coral-500 px-4 py-2.5 text-sm font-semibold text-white shadow-warm hover:bg-coral-600 disabled:opacity-60"
          >
            {submitting ? 'Logging in…' : 'Log in'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-slate-500">
          No account? <Link to="/register" className="font-semibold text-teal-600">Register</Link>
        </p>
      </div>
    </div>
  )
}
