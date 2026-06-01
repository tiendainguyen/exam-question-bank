import { useEffect, useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { api } from '../api/client'
import { useAuthStore } from '../store/auth'

interface Me {
  userId: string
  email: string
}

export default function DashboardPage() {
  const navigate = useNavigate()
  const logout = useAuthStore((s) => s.logout)
  const [me, setMe] = useState<Me | null>(null)

  useEffect(() => {
    // Calls the protected /api/me endpoint to prove the token works end to end.
    api.get<{ data: Me }>('/me').then((res) => setMe(res.data.data)).catch(() => {})
  }, [])

  function onLogout() {
    logout()
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="mx-auto max-w-2xl rounded-xl bg-white p-8 shadow">
        <div className="mb-6 flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-gray-900">Dashboard</h1>
          <button onClick={onLogout} className="rounded-md border px-3 py-1.5 text-sm hover:bg-gray-100">
            Log out
          </button>
        </div>
        {me ? (
          <div className="space-y-4 text-gray-700">
            <div className="space-y-1">
              <p>
                Signed in as <span className="font-medium">{me.email}</span>
              </p>
              <p className="text-sm text-gray-500">User ID: {me.userId}</p>
            </div>
            <Link
              to="/exams/upload"
              className="inline-block rounded-md bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700"
            >
              Upload illustrative exam
            </Link>
          </div>
        ) : (
          <p className="text-gray-500">Loading…</p>
        )}
      </div>
    </div>
  )
}
