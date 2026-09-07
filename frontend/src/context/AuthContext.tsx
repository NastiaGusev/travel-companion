import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import * as authApi from '../api/auth'
import { clearToken, getToken, setToken as persistToken } from '../api/client'

interface AuthUser {
  userId: string
  email: string
}

interface AuthContextValue {
  user: AuthUser | null
  isAuthenticated: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string, displayName?: string) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function userFromToken(token: string | null): AuthUser | null {
  if (!token) return null
  const decoded = authApi.decodeToken(token)
  if (!decoded) return null
  if (decoded.exp && decoded.exp * 1000 < Date.now()) {
    clearToken()
    return null
  }
  return { userId: decoded.userId, email: decoded.email }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => userFromToken(getToken()))

  const applyToken = useCallback((token: string) => {
    persistToken(token)
    setUser(userFromToken(token))
  }, [])

  const login = useCallback(
    async (email: string, password: string) => {
      const { token } = await authApi.login(email, password)
      applyToken(token)
    },
    [applyToken],
  )

  const register = useCallback(
    async (email: string, password: string, displayName?: string) => {
      const { token } = await authApi.register(email, password, displayName)
      applyToken(token)
    },
    [applyToken],
  )

  const logout = useCallback(() => {
    clearToken()
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, isAuthenticated: !!user, login, register, logout }),
    [user, login, register, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
