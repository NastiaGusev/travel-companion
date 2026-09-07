import { apiRequest } from './client'
import type { AuthResponse } from '../types'

export function register(email: string, password: string, displayName?: string) {
  return apiRequest<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: { email, password, displayName: displayName || undefined },
  })
}

export function login(email: string, password: string) {
  return apiRequest<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: { email, password },
  })
}

/** Decodes the JWT payload client-side (no verification — display only). */
export function decodeToken(token: string): { userId: string; email: string; exp: number } | null {
  try {
    const payload = token.split('.')[1]
    const json = atob(payload.replace(/-/g, '+').replace(/_/g, '/'))
    const claims = JSON.parse(json)
    return { userId: claims.sub, email: claims.email, exp: claims.exp }
  } catch {
    return null
  }
}
