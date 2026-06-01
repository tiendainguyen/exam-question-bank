import axios from 'axios'
import { useAuthStore } from '../store/auth'

export const api = axios.create({ baseURL: '/api' })

// Attach the Bearer token to every request when authenticated.
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// On 401, drop the (stale) session so guards redirect to login.
api.interceptors.response.use(
  (res) => res,
  (error) => {
    if (error?.response?.status === 401) {
      useAuthStore.getState().logout()
    }
    return Promise.reject(error)
  },
)

export interface AuthResponse {
  accessToken: string
  tokenType: string
  userId: string
  email: string
}

interface Envelope<T> {
  success: boolean
  data: T
  error: string | null
}

export async function signup(email: string, password: string, displayName?: string) {
  const { data } = await api.post<Envelope<AuthResponse>>('/auth/signup', { email, password, displayName })
  return data.data
}

export async function login(email: string, password: string) {
  const { data } = await api.post<Envelope<AuthResponse>>('/auth/login', { email, password })
  return data.data
}
