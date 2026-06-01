import { api } from './client'

export interface ExamResponse {
  id: string
  name: string
  sourceType: string
  createdAt: string
}

export interface QuestionResponse {
  id: string
  ordinal: number
  stem: string
  choices: string[] | null
  correctAnswer: string | null
}

export interface ExtractionResponse {
  examId: string
  questionCount: number
  questions: QuestionResponse[]
}

export async function uploadIllustrative(file: File, name?: string): Promise<ExamResponse> {
  const form = new FormData()
  form.append('file', file)
  if (name) form.append('name', name)
  const { data } = await api.post<{ data: ExamResponse }>('/exams/illustrative', form)
  return data.data
}

export async function extractExam(examId: string): Promise<ExtractionResponse> {
  const { data } = await api.post<{ data: ExtractionResponse }>(`/exams/${examId}/extract`)
  return data.data
}
