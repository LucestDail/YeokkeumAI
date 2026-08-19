<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { getJson, postJson } from '../api'

interface ToolItem { name: string; description: string; risky: boolean }
interface Approval { id: string; tool: string; args: Record<string, unknown>; requestedBy: string; createdAt: number }

const tools = ref<ToolItem[]>([])
const selected = ref('')
const query = ref('')
const docId = ref('')
const argsJson = ref('{}')
const result = ref('')
const error = ref('')
const busy = ref(false)

const approvals = ref<Approval[]>([])
const approvalsDenied = ref(false)

const current = computed(() => tools.value.find((t) => t.name === selected.value))

async function loadTools() {
  try {
    const r = await getJson<{ items: ToolItem[] }>('/api/tools')
    tools.value = r.items
    if (!selected.value && r.items.length) selected.value = r.items[0].name
  } catch (e) { error.value = String(e) }
}

async function loadApprovals() {
  try {
    const r = await getJson<{ items: Approval[] }>('/api/approvals')
    approvals.value = r.items
    approvalsDenied.value = false
  } catch (e) {
    approvalsDenied.value = true // user 권한이면 403
  }
}

function buildArgs(): Record<string, unknown> {
  if (selected.value === 'doc_search') return { query: query.value }
  if (selected.value === 'doc_delete') return { docId: docId.value }
  try { return JSON.parse(argsJson.value || '{}') } catch { return {} }
}

async function invoke() {
  if (!selected.value || busy.value) return
  error.value = ''; result.value = ''; busy.value = true
  try {
    const r = await postJson<{ status: string; result?: string; message?: string; approvalId?: string }>(
      '/api/tools/' + encodeURIComponent(selected.value), buildArgs())
    if (r.status === 'pending_approval') {
      result.value = `⏸ ${r.message} (승인ID: ${r.approvalId})`
      await loadApprovals()
    } else {
      result.value = r.result || '(결과 없음)'
    }
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

async function decide(id: string, decision: 'approve' | 'reject') {
  busy.value = true; error.value = ''
  try {
    const r = await postJson<{ status: string; result?: string }>(
      '/api/approvals/' + encodeURIComponent(id), { decision })
    result.value = decision === 'approve' ? `✔ 승인·실행: ${r.result || ''}` : '✖ 거부됨'
    await loadApprovals()
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

onMounted(() => { loadTools(); loadApprovals() })
</script>

<template>
  <section class="card" aria-labelledby="agent-h">
    <h2 id="agent-h">업무 에이전트 · 도구</h2>
    <p class="desc">
      도구를 실행합니다. <strong>안전(읽기) 도구는 즉시 실행</strong>, <strong>변경 도구는 관리자 승인(HITL) 후 실행</strong>됩니다.
      모든 실행은 감사로그에 기록됩니다.
    </p>
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="tool-sel">도구</label></div>
        <div class="form-conts" style="max-width: 28rem">
          <select id="tool-sel" class="krds-form-select" v-model="selected">
            <option v-for="t in tools" :key="t.name" :value="t.name">
              {{ t.name }}{{ t.risky ? ' ⚠︎변경' : '' }}
            </option>
          </select>
        </div>
        <p class="form-hint" v-if="current">{{ current.description }}</p>
      </div>

      <div class="form-group" v-if="selected === 'doc_search'">
        <div class="form-tit"><label for="a-query">query</label></div>
        <div class="form-conts"><input id="a-query" type="text" class="krds-input" v-model="query" placeholder="검색 질의" /></div>
      </div>
      <div class="form-group" v-else-if="selected === 'doc_delete'">
        <div class="form-tit"><label for="a-docid">docId</label></div>
        <div class="form-conts"><input id="a-docid" type="text" class="krds-input" v-model="docId" placeholder="삭제할 문서 ID" /></div>
      </div>
      <div class="form-group" v-else>
        <div class="form-tit"><label for="a-json">args (JSON)</label></div>
        <div class="form-conts"><textarea id="a-json" class="krds-input" v-model="argsJson" rows="3"></textarea></div>
      </div>
    </div>
    <button type="button" class="krds-btn primary" :disabled="busy" @click="invoke">
      {{ current?.risky ? '실행 요청(승인 필요)' : '실행' }}
    </button>

    <div v-if="result" class="output mt16" aria-live="polite">{{ result }}</div>

    <hr style="border:none; border-top:1px solid var(--krds-gray-20); margin: var(--sp-24) 0" />
    <h3 style="font-size: var(--fs-h4); font-weight:700; margin-bottom: var(--sp-8)">승인 대기 (HITL)</h3>
    <p v-if="approvalsDenied" class="muted">관리자(admin) 토큰이어야 승인 목록을 볼 수 있습니다.</p>
    <template v-else>
      <p v-if="!approvals.length" class="muted">대기 중인 승인 요청이 없습니다.</p>
      <div v-for="a in approvals" :key="a.id" class="cite">
        <div class="meta">{{ a.tool }} · 요청자 {{ a.requestedBy }}</div>
        <div class="muted">{{ JSON.stringify(a.args) }}</div>
        <div style="margin-top: var(--sp-8)">
          <button type="button" class="krds-btn primary small" :disabled="busy" @click="decide(a.id, 'approve')">승인·실행</button>
          <button type="button" class="krds-btn tertiary small" :disabled="busy" @click="decide(a.id, 'reject')" style="margin-left: var(--sp-8)">거부</button>
        </div>
      </div>
    </template>
  </section>
</template>
