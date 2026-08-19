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

async function errText(res: Response): Promise<string> {
  const body = await res.text().catch(() => '')
  return `HTTP ${res.status}${body ? ' — ' + body.slice(0, 300) : ''}`
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
