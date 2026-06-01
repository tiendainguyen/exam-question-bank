import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { uploadIllustrative, extractExam, type QuestionResponse } from '../api/examApi'

export default function UploadExamPage() {
  const [file, setFile] = useState<File | null>(null)
  const [name, setName] = useState('')
  const [status, setStatus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [questions, setQuestions] = useState<QuestionResponse[] | null>(null)

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!file) return
    setError(null)
    setQuestions(null)
    setBusy(true)
    try {
      setStatus('Uploading…')
      const exam = await uploadIllustrative(file, name || undefined)
      setStatus('Extracting questions (AI)…')
      const result = await extractExam(exam.id)
      setQuestions(result.questions)
      setStatus(`Extracted ${result.questionCount} question(s).`)
    } catch (err: unknown) {
      const detail = (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail
      setError(detail ?? 'Upload or extraction failed')
      setStatus(null)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="mx-auto max-w-3xl space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-gray-900">Upload illustrative exam</h1>
          <Link to="/" className="text-sm text-indigo-600 hover:underline">
            ← Dashboard
          </Link>
        </div>

        <form onSubmit={onSubmit} className="space-y-4 rounded-xl bg-white p-6 shadow">
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-gray-700">Exam name (optional)</span>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
            />
          </label>
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-gray-700">PDF file</span>
            <input
              type="file"
              accept="application/pdf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              required
              className="block w-full text-sm"
            />
          </label>
          {status && <p className="text-sm text-gray-600">{status}</p>}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <button
            type="submit"
            disabled={busy || !file}
            className="rounded-md bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700 disabled:opacity-50"
          >
            {busy ? 'Working…' : 'Upload & extract'}
          </button>
        </form>

        {questions && (
          <div className="overflow-hidden rounded-xl bg-white shadow">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-100 text-gray-600">
                <tr>
                  <th className="px-4 py-2 w-12">#</th>
                  <th className="px-4 py-2">Stem</th>
                  <th className="px-4 py-2">Choices</th>
                  <th className="px-4 py-2 w-24">Answer</th>
                </tr>
              </thead>
              <tbody>
                {questions.map((q) => (
                  <tr key={q.id} className="border-t align-top">
                    <td className="px-4 py-2 font-medium">{q.ordinal}</td>
                    <td className="px-4 py-2">{q.stem}</td>
                    <td className="px-4 py-2">
                      <ul className="list-disc pl-4">
                        {(q.choices ?? []).map((c, i) => (
                          <li key={i}>{c}</li>
                        ))}
                      </ul>
                    </td>
                    <td className="px-4 py-2">{q.correctAnswer ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}
