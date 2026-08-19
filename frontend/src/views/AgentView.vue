<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { getJson, postJson } from '../api'

interface ToolItem { name: string; description: string; risky: boolean }
interface Approval { id: string; tool: string; args: Record<string, unknown>; requestedBy: string; createdAt: number }

const { t } = useI18n()
const instruction = ref('')
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

const current = computed(() => tools.value.find((t2) => t2.name === selected.value))

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
  } catch {
    approvalsDenied.value = true
  }
}

function buildArgs(): Record<string, unknown> {
  if (selected.value === 'doc_search') return { query: query.value }
  if (selected.value === 'doc_delete') return { docId: docId.value }
  try { return JSON.parse(argsJson.value || '{}') } catch { return {} }
}

async function runAgent() {
  if (!instruction.value.trim() || busy.value) return
  error.value = ''; result.value = ''; busy.value = true
  try {
    const r = await postJson<{ status: string; result?: string; message?: string; chosenTool?: string; approvalId?: string }>(
      '/api/agent', { instruction: instruction.value })
    if (r.status === 'no_tool') result.value = 'ℹ ' + (r.message || '')
    else if (r.status === 'pending_approval') { result.value = `⏸ [${r.chosenTool}] ${r.message} (${r.approvalId})`; await loadApprovals() }
    else result.value = `[${r.chosenTool}] ` + (r.result || t('common.none'))
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

async function invoke() {
  if (!selected.value || busy.value) return
  error.value = ''; result.value = ''; busy.value = true
  try {
    const r = await postJson<{ status: string; result?: string; message?: string; approvalId?: string }>(
      '/api/tools/' + encodeURIComponent(selected.value), buildArgs())
    if (r.status === 'pending_approval') { result.value = `⏸ ${r.message} (${r.approvalId})`; await loadApprovals() }
    else result.value = r.result || t('common.none')
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

async function decide(id: string, decision: 'approve' | 'reject') {
  busy.value = true; error.value = ''
  try {
    const r = await postJson<{ status: string; result?: string }>('/api/approvals/' + encodeURIComponent(id), { decision })
    result.value = decision === 'approve' ? `✔ ${r.result || ''}` : '✖'
    await loadApprovals()
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

onMounted(() => { loadTools(); loadApprovals() })
</script>

<template>
  <section class="card" aria-labelledby="agent-h">
    <h2 id="agent-h">{{ t('agent.h') }}</h2>
    <p class="desc">{{ t('agent.desc') }}</p>
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="agent-inst">{{ t('agent.instr') }}</label></div>
        <div class="form-conts row">
          <input id="agent-inst" type="text" class="krds-input" v-model="instruction" :placeholder="t('agent.instrph')" @keyup.enter="runAgent" />
          <div style="flex:0 0 auto"><button type="button" class="krds-btn primary" :disabled="busy" @click="runAgent">{{ t('agent.runAgent') }}</button></div>
        </div>
        <p class="form-hint">{{ t('agent.instrHint') }}</p>
      </div>
    </div>

    <details style="margin-bottom: var(--sp-16)">
      <summary style="cursor:pointer; font-weight:700">{{ t('agent.manual') }}</summary>
      <div class="fieldset mt16">
        <div class="form-group">
          <div class="form-tit"><label for="tool-sel">{{ t('agent.tool') }}</label></div>
          <div class="form-conts" style="max-width: 28rem">
            <select id="tool-sel" class="krds-form-select" v-model="selected">
              <option v-for="tl in tools" :key="tl.name" :value="tl.name">{{ tl.name }}{{ tl.risky ? ' ⚠︎' : '' }}</option>
            </select>
          </div>
          <p class="form-hint" v-if="current">{{ current.description }}</p>
        </div>
        <div class="form-group" v-if="selected === 'doc_search'">
          <div class="form-tit"><label for="a-query">query</label></div>
          <div class="form-conts"><input id="a-query" type="text" class="krds-input" v-model="query" /></div>
        </div>
        <div class="form-group" v-else-if="selected === 'doc_delete'">
          <div class="form-tit"><label for="a-docid">docId</label></div>
          <div class="form-conts"><input id="a-docid" type="text" class="krds-input" v-model="docId" /></div>
        </div>
        <div class="form-group" v-else>
          <div class="form-tit"><label for="a-json">args (JSON)</label></div>
          <div class="form-conts"><textarea id="a-json" class="krds-input" v-model="argsJson" rows="3"></textarea></div>
        </div>
      </div>
      <button type="button" class="krds-btn secondary" :disabled="busy" @click="invoke">
        {{ current?.risky ? t('agent.reqApprove') : t('common.run') }}
      </button>
    </details>

    <div v-if="result" class="output mt16" aria-live="polite">{{ result }}</div>

    <hr style="border:none; border-top:1px solid var(--krds-gray-20); margin: var(--sp-24) 0" />
    <h3 style="font-size: var(--fs-h4); font-weight:700; margin-bottom: var(--sp-8)">{{ t('agent.hitlH') }}</h3>
    <p v-if="approvalsDenied" class="muted">{{ t('agent.adminOnly') }}</p>
    <template v-else>
      <p v-if="!approvals.length" class="muted">{{ t('agent.noPending') }}</p>
      <div v-for="a in approvals" :key="a.id" class="cite">
        <div class="meta">{{ a.tool }} · {{ t('agent.requester') }} {{ a.requestedBy }}</div>
        <div class="muted">{{ JSON.stringify(a.args) }}</div>
        <div style="margin-top: var(--sp-8)">
          <button type="button" class="krds-btn primary small" :disabled="busy" @click="decide(a.id, 'approve')">{{ t('agent.approve') }}</button>
          <button type="button" class="krds-btn tertiary small" :disabled="busy" @click="decide(a.id, 'reject')" style="margin-left: var(--sp-8)">{{ t('agent.reject') }}</button>
        </div>
      </div>
    </template>
  </section>
</template>
