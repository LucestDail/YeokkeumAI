import { createI18n } from 'vue-i18n'

const LOCALE_KEY = 'yk_locale'

const ko = {
  brand: { name: '엮음AI', tagline: '공공기관 업무보조 AI · 통제·책임성·연계' },
  nav: { chat: '대화', write: '문서 작성·요약', review: '규정검토', docs: '문서·검색', agent: '에이전트·도구', audit: '감사로그' },
  token: { label: 'API 토큰', placeholder: 'Bearer 토큰 (secure-by-default: 미설정 시 API 차단)', note: '브라우저에만 저장됩니다' },
  lang: { label: '언어' },
  footer: { text: '엮음AI — 온프렘/내부망 벤더무관 AI 게이트웨이 · 공식 KRDS 디자인시스템 기반 · 웹접근성(KWCAG 2.2) 준수 지향' },
  skip: '본문 바로가기',
  common: { run: '실행', running: '처리 중…', result: '결과', none: '—' },
  chat: {
    h: '대화', desc: '벤더무관 게이트웨이를 통한 대화. 응답은 SSE로 실시간 스트리밍됩니다.',
    q: '질문', qph: '예: 공공 웹 접근성 준수 항목을 알려줘',
    send: '보내기 (Ctrl+Enter)', sending: '생성 중…', answer: '응답',
    genBusy: '응답 생성 중입니다', genDone: '응답 생성이 완료되었습니다'
  },
  write: {
    h: '문서 작성·요약', desc: '보고서·공문 초안 작성과 긴 문서 요약. 모든 요청은 감사로그에 기록됩니다.',
    tabSum: '요약', tabDraft: '초안 작성',
    src: '원문', srcph: '요약할 텍스트를 붙여넣으세요', sum3: '3문장 요약', summarizing: '요약 중…', sumOut: '요약',
    kind: '문서 종류', kindph: '보고서 / 공문 / 보도자료', brief: '작성 지시', briefph: '예: 노후 정수장 개선 사업 추진계획 보고서',
    gen: '초안 생성', generating: '작성 중…', draftOut: '초안'
  },
  review: {
    h: '규정검토',
    desc: '작성물을 등록된 규정·근거 문서에 비추어 위반·리스크를 항목별로 지적하고 수정문안을 제시합니다. 근거 없는 사항은 지어내지 않고 근거 범위 밖으로 명시합니다. (규정 문서는 「문서·검색」에서 먼저 등록)',
    lang: '답변 언어', target: '검토 대상(작성물)', targetph: '검토받을 공문·계획서·약관 등을 붙여넣으세요',
    run: '규정검토 실행', running: '검토 중…', grounded: '근거 기반', ungrounded: '근거 없음', model: '모델',
    resultLabel: '검토 결과', citations: '근거 인용'
  },
  docs: {
    h: '문서·검색 (RAG)', desc: '규정·지식 문서를 등록하고 근거 기반으로 검색합니다. 파일은 PDF·텍스트·HWP/HWPX·DOCX/XLSX를 지원합니다.',
    upload: '파일 업로드', uploadBtn: '업로드·색인', hint: 'PDF · 텍스트(.txt/.md) · HWP/HWPX · DOCX/XLSX 지원',
    textReg: '텍스트 직접 등록', docName: '문서명', docNameph: '예: 개인정보보호지침.txt', body: '본문', bodyph: '규정·지침 본문', textIndex: '텍스트 색인',
    listLabel: '등록 문서', colName: '문서명', colChars: '글자수', colChunks: '청크', colManage: '관리', delete: '삭제', noDocs: '등록된 문서가 없습니다.',
    search: '근거 검색', searchph: '예: 개인정보 보관기간 규정', searchBtn: '검색',
    grounded: '근거 기반', ungrounded: '근거 없음',
    confirmDelete: '문서를 삭제할까요? (근거에서 제외됩니다)'
  },
  agent: {
    h: '업무 에이전트 · 도구',
    desc: '도구를 실행합니다. 안전(읽기) 도구는 즉시 실행, 변경 도구는 관리자 승인(HITL) 후 실행됩니다. 모든 실행은 감사로그에 기록됩니다.',
    instr: '자연어 지시', instrph: '예: 웹접근성 기준을 규정에서 찾아줘', runAgent: '에이전트 실행',
    instrHint: 'LLM 이 적합한 도구를 골라 실행합니다(변경 도구는 승인 필요).',
    manual: '직접 도구 실행(수동)', tool: '도구', reqApprove: '실행 요청(승인 필요)',
    hitlH: '승인 대기 (HITL)', adminOnly: '관리자(admin) 토큰이어야 승인 목록을 볼 수 있습니다.', noPending: '대기 중인 승인 요청이 없습니다.',
    requester: '요청자', approve: '승인·실행', reject: '거부'
  },
  audit: {
    h: '감사로그', desc: '모든 요청의 주체·시각·행위·모델 기록(PII 마스킹). admin 권한이 필요합니다.',
    fAction: '행위', all: '전체', fActor: '주체(토큰 프리픽스)', fActorph: '예: token:646997', fLimit: '개수',
    query: '조회', querying: '조회 중…', export: 'CSV 내보내기',
    colTime: '시각', colActor: '주체', colRole: '역할', colAction: '행위', colDetail: '상세', noLogs: '감사로그가 없습니다.'
  }
}

const en: typeof ko = {
  brand: { name: 'YeokkeumAI', tagline: 'Public-sector AI assistant · Control · Accountability · Interoperability' },
  nav: { chat: 'Chat', write: 'Draft·Summarize', review: 'Compliance', docs: 'Docs·Search', agent: 'Agent·Tools', audit: 'Audit Log' },
  token: { label: 'API Token', placeholder: 'Bearer token (secure-by-default: API closed if unset)', note: 'Stored only in your browser' },
  lang: { label: 'Language' },
  footer: { text: 'YeokkeumAI — on-prem vendor-neutral AI gateway · built on the official KRDS design system · web accessibility (KWCAG 2.2) oriented' },
  skip: 'Skip to content',
  common: { run: 'Run', running: 'Working…', result: 'Result', none: '—' },
  chat: {
    h: 'Chat', desc: 'Chat via the vendor-neutral gateway. Responses stream in real time over SSE.',
    q: 'Question', qph: 'e.g., Tell me the public web accessibility requirements',
    send: 'Send (Ctrl+Enter)', sending: 'Generating…', answer: 'Response',
    genBusy: 'Generating response', genDone: 'Response complete'
  },
  write: {
    h: 'Draft · Summarize', desc: 'Draft reports/official letters and summarize long documents. All requests are audited.',
    tabSum: 'Summarize', tabDraft: 'Draft',
    src: 'Source text', srcph: 'Paste text to summarize', sum3: 'Summarize (3 sentences)', summarizing: 'Summarizing…', sumOut: 'Summary',
    kind: 'Document type', kindph: 'Report / Official letter / Press release', brief: 'Instructions', briefph: 'e.g., Report on the water plant improvement plan',
    gen: 'Generate draft', generating: 'Drafting…', draftOut: 'Draft'
  },
  review: {
    h: 'Compliance Review',
    desc: 'Reviews your text against registered regulations/evidence, flags violations/risks item by item and suggests revised wording. It does not fabricate; anything unsupported is marked out of scope. (Register regulation docs under “Docs·Search” first)',
    lang: 'Answer language', target: 'Text to review', targetph: 'Paste the letter/plan/terms to review',
    run: 'Run review', running: 'Reviewing…', grounded: 'Grounded', ungrounded: 'No evidence', model: 'model',
    resultLabel: 'Review result', citations: 'Citations'
  },
  docs: {
    h: 'Docs · Search (RAG)', desc: 'Register regulation/knowledge docs and search with citations. Files: PDF · text · HWP/HWPX · DOCX/XLSX.',
    upload: 'Upload file', uploadBtn: 'Upload·Index', hint: 'PDF · text(.txt/.md) · HWP/HWPX · DOCX/XLSX',
    textReg: 'Register text directly', docName: 'Document name', docNameph: 'e.g., privacy-policy.txt', body: 'Body', bodyph: 'Regulation/guideline body', textIndex: 'Index text',
    listLabel: 'Registered docs', colName: 'Name', colChars: 'Chars', colChunks: 'Chunks', colManage: 'Manage', delete: 'Delete', noDocs: 'No documents registered.',
    search: 'Evidence search', searchph: 'e.g., personal data retention rules', searchBtn: 'Search',
    grounded: 'Grounded', ungrounded: 'No evidence',
    confirmDelete: 'Delete this document? (removed from evidence)'
  },
  agent: {
    h: 'Agent · Tools',
    desc: 'Run tools. Safe (read) tools run immediately; change tools run after admin approval (HITL). All runs are audited.',
    instr: 'Natural-language instruction', instrph: 'e.g., Find the web accessibility rules in the regulations', runAgent: 'Run agent',
    instrHint: 'The LLM picks a suitable tool and runs it (change tools need approval).',
    manual: 'Run a tool manually', tool: 'Tool', reqApprove: 'Request (needs approval)',
    hitlH: 'Pending approvals (HITL)', adminOnly: 'An admin token is required to view approvals.', noPending: 'No pending approvals.',
    requester: 'requested by', approve: 'Approve·Run', reject: 'Reject'
  },
  audit: {
    h: 'Audit Log', desc: 'Records who·when·what·which model for every request (PII masked). Admin only.',
    fAction: 'Action', all: 'All', fActor: 'Actor (token prefix)', fActorph: 'e.g., token:646997', fLimit: 'Limit',
    query: 'Query', querying: 'Querying…', export: 'Export CSV',
    colTime: 'Time', colActor: 'Actor', colRole: 'Role', colAction: 'Action', colDetail: 'Detail', noLogs: 'No audit logs.'
  }
}

const saved = (typeof localStorage !== 'undefined' && localStorage.getItem(LOCALE_KEY)) || 'ko'

export const i18n = createI18n({
  legacy: false,
  locale: saved,
  fallbackLocale: 'ko',
  messages: { ko, en }
})

export function setLocale(loc: string): void {
  i18n.global.locale.value = loc as 'ko' | 'en'
  try { localStorage.setItem(LOCALE_KEY, loc) } catch { /* ignore */ }
  document.documentElement.lang = loc
}
