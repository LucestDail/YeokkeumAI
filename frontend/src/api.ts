// 엮음AI 백엔드 API 클라이언트. 토큰은 localStorage 보관, Bearer 헤더로 전송.

// context-path(prod=/yeokkeum/) 아래 서빙 시 API 경로도 base 를 붙인다.
const BASE = import.meta.env.BASE_URL // dev='/', prod='/yeokkeum/'
function u(path: string): string {
  return BASE.replace(/\/$/, '') + '/' + path.replace(/^\//, '')
}

const TOKEN_KEY = 'yk_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) ?? ''
}
export function setToken(t: string): void {
  localStorage.setItem(TOKEN_KEY, t)
}

function authHeader(): Record<string, string> {
  const t = getToken()
  return t ? { Authorization: 'Bearer ' + t } : {}
}

const STATUS_MSG: Record<number, string> = {
  400: '요청이 올바르지 않습니다.',
  401: '인증이 필요합니다. 상단에 API 토큰을 입력하세요.',
  403: '권한이 없습니다. 관리자 전용 기능일 수 있습니다.',
  404: '대상을 찾을 수 없습니다.',
  413: '파일이 허용 크기를 초과했습니다.',
  415: '지원하지 않는 형식입니다.',
  500: '서버 오류가 발생했습니다.',
  503: '기능을 일시적으로 사용할 수 없습니다.'
}

// 상태코드별 한국어 안내 + 서버가 준 상세(한국어)만 덧붙임. raw HTTP 문자열 노출 방지.
async function errText(res: Response): Promise<string> {
  let detail = ''
  const raw = await res.text().catch(() => '')
  try {
    const j = JSON.parse(raw) as { detail?: string; message?: string }
    detail = j.detail || j.message || ''
  } catch {
    detail = raw
  }
  const base = STATUS_MSG[res.status] || `오류가 발생했습니다 (HTTP ${res.status})`
  const skip = detail === 'unauthorized' || detail === 'forbidden' || !detail
  return skip ? base : `${base} (${detail.slice(0, 200)})`
}

export async function postJson<T>(path: string, body: unknown): Promise<T> {
  const res = await fetch(u(path), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify(body)
  })
  if (!res.ok) throw new Error(await errText(res))
  return (await res.json()) as T
}

export async function getJson<T>(path: string): Promise<T> {
  const res = await fetch(u(path), { headers: authHeader() })
  if (!res.ok) throw new Error(await errText(res))
  return (await res.json()) as T
}

export async function deleteJson<T>(path: string): Promise<T> {
  const res = await fetch(u(path), { method: 'DELETE', headers: authHeader() })
  if (!res.ok) throw new Error(await errText(res))
  return (await res.json()) as T
}

export async function uploadFile<T>(path: string, file: File): Promise<T> {
  const fd = new FormData()
  fd.append('file', file)
  const res = await fetch(u(path), { method: 'POST', headers: authHeader(), body: fd })
  if (!res.ok) throw new Error(await errText(res))
  return (await res.json()) as T
}

/** /api/chat SSE 스트리밍. data:{"t":tok} 프레임 파싱, data:[DONE] 종료. */
export function streamChat(
  message: string,
  onToken: (t: string) => void,
  onDone: () => void,
  onError: (e: string) => void
): void {
  fetch(u('/api/chat'), {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...authHeader() },
    body: JSON.stringify({ message })
  })
    .then(async (res) => {
      if (!res.ok || !res.body) {
        onError(await errText(res))
        return
      }
      const reader = res.body.getReader()
      const dec = new TextDecoder()
      let buf = ''
      for (;;) {
        const { value, done } = await reader.read()
        if (done) break
        buf += dec.decode(value, { stream: true })
        let idx: number
        while ((idx = buf.indexOf('\n\n')) >= 0) {
          const frame = buf.slice(0, idx)
          buf = buf.slice(idx + 2)
          const line = frame.split('\n').find((l) => l.startsWith('data:'))
          if (!line) continue
          const data = line.slice(5).trim()
          if (data === '[DONE]') {
            onDone()
            return
          }
          try {
            const o = JSON.parse(data) as { t?: string }
            if (o.t) onToken(o.t)
          } catch {
            /* keep-alive/비JSON 프레임 무시 */
          }
        }
      }
      onDone()
    })
    .catch((e: unknown) => onError(String(e)))
}

// ── 응답 타입 ──
export interface Citation {
  filename: string
  idx: number
  score: number
  snippet: string
}
export interface RagResult {
  answer: string
  citations: Citation[]
  grounded: boolean
  model: string
}
export interface IngestResult {
  docId: string
  filename: string
  nChunks: number
}
export interface DocItem {
  id: string
  filename: string
  chars: number
  nChunks?: number
  createdAt: number
}
export interface AuditItem {
  id: string
  ts: number
  actor: string
  role: string
  action: string
  detail: string
}
