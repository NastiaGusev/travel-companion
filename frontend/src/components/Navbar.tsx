import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { LogOut } from 'lucide-react'
import { useAuth } from '../context/AuthContext'
import Logo from './Logo'

export default function Navbar() {
  const { user, isAuthenticated, logout } = useAuth()
  const navigate = useNavigate()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!menuOpen) return
    function handleClickOutside(e: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [menuOpen])

  return (
    <header className="border-b border-coral-100 bg-white">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3">
        <Link to="/" className="flex items-center gap-2.5 text-lg font-bold text-ink">
          <Logo size={30} />
          Travel Companion
        </Link>

        {isAuthenticated && (
          <div className="relative" ref={menuRef}>
            <button
              onClick={() => setMenuOpen((v) => !v)}
              aria-label="Account menu"
              aria-expanded={menuOpen}
              className="flex h-9 w-9 items-center justify-center rounded-full bg-green-600 text-sm font-semibold text-white hover:bg-green-700"
            >
              {user?.email[0]?.toUpperCase()}
            </button>

            {menuOpen && (
              <div className="absolute right-0 z-40 mt-2 w-56 rounded-2xl border border-coral-100 bg-white p-3 shadow-warm-lg">
                <p className="truncate px-1 text-sm font-medium text-ink">{user?.email}</p>
                <button
                  onClick={() => {
                    setMenuOpen(false)
                    logout()
                    navigate('/login')
                  }}
                  className="mt-2 flex w-full items-center gap-2 rounded-xl px-1 py-2 text-sm font-medium text-coral-600 hover:bg-coral-50"
                >
                  <LogOut size={15} />
                  Log out
                </button>
              </div>
            )}
          </div>
        )}
      </div>
    </header>
  )
}
