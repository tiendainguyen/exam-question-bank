import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import {
  uploadIllustrative,
  extractExam,
  type QuestionResponse,
  type ExtractionMethod,
} from '../api/examApi'

export default function UploadExamPage() {
  const [file, setFile] = useState<File | null>(null)
  const [name, setName] = useState('')
  const [method, setMethod] = useState<ExtractionMethod>('TESSERACT')
  const [examId, setExamId] = useState<string | null>(null)
  const [status, setStatus] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [canRetryWithAi, setCanRetryWithAi] = useState(false)
  const [busy, setBusy] = useState(false)
  const [questions, setQuestions] = useState<QuestionResponse[] | null>(null)

  async function runExtraction(id: string, m: ExtractionMethod) {
    setError(null)
    setCanRetryWithAi(false)
    setQuestions(null)
    setBusy(true)
    try {
      setStatus(m === 'AI_VISION' ? 'Trích xuất bằng AI Vision…' : 'OCR bằng Tesseract…')
      const result = await extractExam(id, m)
      setQuestions(result.questions)
      setStatus(`Đã trích ${result.questionCount} câu (${m === 'AI_VISION' ? 'AI Vision' : 'Tesseract'}).`)
    } catch (err: unknown) {
      const res = (err as { response?: { status?: number; data?: { detail?: string; retryWithAi?: boolean } } })?.response
      setStatus(null)
      if (res?.status === 422 && res.data?.retryWithAi) {
        setError('OCR Tesseract không tách được câu hỏi. Bạn có thể thử lại bằng AI Vision.')
        setCanRetryWithAi(true)
      } else {
        setError(res?.data?.detail ?? 'Trích xuất thất bại.')
      }
    } finally {
      setBusy(false)
    }
  }

  async function onSubmit(e: FormEvent) {
    e.preventDefault()
    if (!file) return
    setBusy(true)
    setError(null)
    setQuestions(null)
    try {
      setStatus('Đang tải lên…')
      const exam = await uploadIllustrative(file, name || undefined)
      setExamId(exam.id)
      await runExtraction(exam.id, method)
    } catch {
      setStatus(null)
      setError('Tải lên thất bại.')
      setBusy(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50 p-8">
      <div className="mx-auto max-w-3xl space-y-6">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-semibold text-gray-900">Upload đề minh họa</h1>
          <Link to="/" className="text-sm text-indigo-600 hover:underline">
            ← Dashboard
          </Link>
        </div>

        <form onSubmit={onSubmit} className="space-y-4 rounded-xl bg-white p-6 shadow">
          <label className="block">
            <span className="mb-1 block text-sm font-medium text-gray-700">Tên đề (tuỳ chọn)</span>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full rounded-md border border-gray-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
            />
          </label>

          <label className="block">
            <span className="mb-1 block text-sm font-medium text-gray-700">File PDF</span>
            <input
              type="file"
              accept="application/pdf"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              required
              className="block w-full text-sm"
            />
          </label>

          <fieldset className="space-y-2">
            <legend className="mb-1 text-sm font-medium text-gray-700">Cách trích xuất (OCR)</legend>
            <label className="flex items-start gap-2">
              <input
                type="radio"
                name="method"
                checked={method === 'TESSERACT'}
                onChange={() => setMethod('TESSERACT')}
                className="mt-1"
              />
              <span className="text-sm">
                <span className="font-medium">Tesseract (miễn phí)</span> — OCR bằng thư viện, chạy offline.
                Ưu tiên lớp text có sẵn, tự OCR khi là đề scan.
              </span>
            </label>
            <label className="flex items-start gap-2">
              <input
                type="radio"
                name="method"
                checked={method === 'AI_VISION'}
                onChange={() => setMethod('AI_VISION')}
                className="mt-1"
              />
              <span className="text-sm">
                <span className="font-medium">AI Vision</span> — mô hình thị giác đọc ảnh trang, chịu được
                layout phức tạp (cần model có vision).
              </span>
            </label>
          </fieldset>

          {status && <p className="text-sm text-gray-600">{status}</p>}
          {error && <p className="text-sm text-red-600">{error}</p>}
          {canRetryWithAi && examId && (
            <button
              type="button"
              disabled={busy}
              onClick={() => {
                setMethod('AI_VISION')
                runExtraction(examId, 'AI_VISION')
              }}
              className="rounded-md bg-amber-600 px-4 py-2 text-white hover:bg-amber-700 disabled:opacity-50"
            >
              Thử lại bằng AI Vision
            </button>
          )}

          <div>
            <button
              type="submit"
              disabled={busy || !file}
              className="rounded-md bg-indigo-600 px-4 py-2 text-white hover:bg-indigo-700 disabled:opacity-50"
            >
              {busy ? 'Đang xử lý…' : 'Tải lên & trích xuất'}
            </button>
          </div>
        </form>

        {questions && (
          <div className="overflow-hidden rounded-xl bg-white shadow">
            <table className="w-full text-left text-sm">
              <thead className="bg-gray-100 text-gray-600">
                <tr>
                  <th className="w-12 px-4 py-2">#</th>
                  <th className="px-4 py-2">Câu hỏi</th>
                  <th className="px-4 py-2">Lựa chọn</th>
                  <th className="w-24 px-4 py-2">Đáp án</th>
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