import { ApiError } from '../api/client'

/** Renders a friendly message from an ApiError (or any error), or nothing at all. */
export default function ErrorBanner({ error }: { error: unknown }) {
  if (!error) return null
  const message = error instanceof ApiError ? error.message : (error as Error)?.message || 'Something went wrong.'
  const fieldErrors = error instanceof ApiError ? error.problem?.errors : undefined

  return (
    <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-800">
      <p className="font-medium">{message}</p>
      {fieldErrors && (
        <ul className="mt-1 list-inside list-disc">
          {Object.entries(fieldErrors).map(([field, msg]) => (
            <li key={field}>
              <span className="font-medium">{field}</span>: {msg}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
