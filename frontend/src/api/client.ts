import type { ProblemDetail } from '../types'

// Empty string -> relative paths, caught by the vite dev proxy (see vite.config.ts).
// Set VITE_API_BASE_URL for a production build pointed at a deployed backend.
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

const TOKEN_KEY = 'travel-companion.token'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

/**
 * Thrown for any non-2xx response. Carries the parsed RFC 7807 problem
 * details (title/detail/code) when the backend returned application/problem+json,
 * so callers can branch on `code` (e.g. VERSION_CONFLICT, PLACE_NOT_FOUND).
 */
export class ApiError extends Error {
  status: number
  problem: ProblemDetail | null

  constructor(status: number, problem: ProblemDetail | null, fallbackMessage: string) {
    super(problem?.detail || problem?.title || fallbackMessage)
    this.name = 'ApiError'
    this.status = status
    this.problem = problem
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  query?: Record<string, string | number | undefined>
}

function buildUrl(path: string, query?: RequestOptions['query']): string {
  const url = new URL(API_BASE_URL + path, window.location.origin)
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, String(value))
      }
    }
  }
  // Relative URLs: strip the origin back off so the dev proxy still matches "/api/...".
  return API_BASE_URL ? url.toString() : url.pathname + url.search
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query } = options
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = getToken()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response: Response
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    })
  } catch {
    throw new ApiError(0, null, 'Could not reach the server. Is the backend running?')
  }

  if (response.status === 204) {
    return undefined as T
  }

  const contentType = response.headers.get('content-type') || ''
  const isJson = contentType.includes('json')
  const data = isJson ? await response.json().catch(() => null) : null

  if (!response.ok) {
    const problem: ProblemDetail | null = contentType.includes('problem+json') || isJson ? data : null
    if (response.status === 401) {
      clearToken()
    }
    throw new ApiError(response.status, problem, `Request failed with status ${response.status}`)
  }

  return data as T
}
