import { useState, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { signup } from '../api/client'
import { useAuthStore } from '../store/auth'
import { AuthCard, Field } from './LoginPage'

export default function SignupPage() {
  const navigate = useNavigate()
  const setAuth = useAuthStore((s) => s.setAuth)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setLoading(true)
    try {
      const res = await signup(email, password, displayName || undefined)
      setAuth({ token: res.accessToken, userId: res.userId, email: res.email })
      navigate('/')
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status
      setError(status === 409 ? 'Email already in use' : 'Signup failed — check your input')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthCard title="Create account">
      <form onSubmit={onSubmit} className="space-y-4">
        <Field label="Display name" type="text" value={displayName} onChange={setDisplayName} />
        <Field label="Email" type="email" value={email} onChange={setEmail} />
        <Field label="Password (min 8 chars)" type="password" value={password} onChange={setPassword} />
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-md bg-indigo-600 py-2 text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {loading ? 'Creating…' : 'Sign up'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-gray-600">
        Have an account?{' '}
        <Link to="/login" className="text-indigo-600 hover:underline">
          Log in
        </Link>
      </p>
    </AuthCard>
  )
}
