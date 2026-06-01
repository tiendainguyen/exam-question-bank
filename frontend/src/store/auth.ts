import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface AuthState {
  token: string | null
  userId: string | null
  email: string | null
  setAuth: (auth: { token: string; userId: string; email: string }) => void
  logout: () => void
  isAuthenticated: () => boolean
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      userId: null,
      email: null,
      setAuth: ({ token, userId, email }) => set({ token, userId, email }),
      logout: () => set({ token: null, userId: null, email: null }),
      isAuthenticated: () => !!get().token,
    }),
    { name: 'eqb-auth' },
  ),
)
