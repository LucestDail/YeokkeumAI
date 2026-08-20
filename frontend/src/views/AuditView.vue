<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getJson, getBlob, type AuditItem } from '../api'

const { t } = useI18n()
const items = ref<AuditItem[]>([])
const error = ref('')
const busy = ref(false)

const fAction = ref('')
const fActor = ref('')
const fLimit = ref(200)

const ACTIONS = ['', 'chat', 'summarize', 'draft', 'ingest', 'upload', 'rag_query', 'review', 'delete', 'tool_invoke', 'tool_exec', 'approval_request', 'approval_approve', 'approval_reject', 'agent']

function fmt(ts: number): string {
  try { return new Date(ts).toLocaleString() } catch { return String(ts) }
}

function queryString(): string {
  const p = new URLSearchParams()
  if (fAction.value) p.set('action', fAction.value)
  if (fActor.value.trim()) p.set('actor', fActor.value.trim())
  p.set('limit', String(fLimit.value || 200))
  return p.toString()
}

async function load() {
  error.value = ''; busy.value = true
  try {
    const r = await getJson<{ items: AuditItem[] }>('/api/audit?' + queryString())
    items.value = r.items
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

async function exportCsv() {
  error.value = ''; busy.value = true
  try {
    const blob = await getBlob('/api/audit/export?' + queryString())
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url; a.download = 'audit.csv'; a.click()
    URL.revokeObjectURL(url)
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <section class="card">
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="fieldset">
      <div class="row">
        <div class="form-group">
          <div class="form-tit"><label for="f-action">{{ t('audit.fAction') }}</label></div>
          <div class="form-conts">
            <select id="f-action" class="krds-form-select" v-model="fAction">
              <option v-for="a in ACTIONS" :key="a" :value="a">{{ a === '' ? t('audit.all') : a }}</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <div class="form-tit"><label for="f-actor">{{ t('audit.fActor') }}</label></div>
          <div class="form-conts"><input id="f-actor" type="text" class="krds-input" v-model="fActor" :placeholder="t('audit.fActorph')" /></div>
        </div>
        <div class="form-group" style="flex:0 0 120px">
          <div class="form-tit"><label for="f-limit">{{ t('audit.fLimit') }}</label></div>
          <div class="form-conts"><input id="f-limit" type="number" class="krds-input" v-model.number="fLimit" min="1" max="1000" /></div>
        </div>
      </div>
    </div>
    <button type="button" class="krds-btn primary" :disabled="busy" @click="load">{{ busy ? t('audit.querying') : t('audit.query') }}</button>
    <button type="button" class="krds-btn secondary" :disabled="busy" @click="exportCsv" style="margin-left:var(--sp-8)">{{ t('audit.export') }}</button>

    <div class="mt16" style="overflow-x:auto">
      <div v-if="items.length" class="krds-table-wrap">
        <table class="tbl col data">
          <caption class="sr-only">{{ t('audit.h') }}</caption>
          <thead>
            <tr><th scope="col">{{ t('audit.colTime') }}</th><th scope="col">{{ t('audit.colActor') }}</th><th scope="col">{{ t('audit.colRole') }}</th><th scope="col">{{ t('audit.colAction') }}</th><th scope="col">{{ t('audit.colDetail') }}</th></tr>
          </thead>
          <tbody>
            <tr v-for="a in items" :key="a.id">
              <td>{{ fmt(a.ts) }}</td><td>{{ a.actor }}</td><td>{{ a.role }}</td>
              <td><span class="krds-badge bg-light-information">{{ a.action }}</span></td><td>{{ a.detail }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="muted">{{ t('audit.noLogs') }}</p>
    </div>
  </section>
</template>
