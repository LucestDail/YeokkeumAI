<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getJson, getBlob, type AuditItem } from '../api'

const items = ref<AuditItem[]>([])
const error = ref('')
const busy = ref(false)

const fAction = ref('')
const fActor = ref('')
const fLimit = ref(200)

const ACTIONS = ['', 'chat', 'summarize', 'draft', 'ingest', 'upload', 'rag_query', 'review', 'delete']

function fmt(ts: number): string {
  try { return new Date(ts).toLocaleString('ko-KR') } catch { return String(ts) }
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
  <section class="card" aria-labelledby="audit-h">
    <h2 id="audit-h">감사로그</h2>
    <p class="desc">모든 요청의 <strong>주체·시각·행위·모델</strong> 기록(PII 마스킹). <strong>admin</strong> 권한이 필요합니다.</p>
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="fieldset">
      <div class="row">
        <div class="form-group">
          <div class="form-tit"><label for="f-action">행위</label></div>
          <div class="form-conts">
            <select id="f-action" class="krds-form-select" v-model="fAction">
              <option v-for="a in ACTIONS" :key="a" :value="a">{{ a === '' ? '전체' : a }}</option>
            </select>
          </div>
        </div>
        <div class="form-group">
          <div class="form-tit"><label for="f-actor">주체(토큰 프리픽스)</label></div>
          <div class="form-conts"><input id="f-actor" type="text" class="krds-input" v-model="fActor" placeholder="예: token:646997" /></div>
        </div>
        <div class="form-group" style="flex:0 0 120px">
          <div class="form-tit"><label for="f-limit">개수</label></div>
          <div class="form-conts"><input id="f-limit" type="number" class="krds-input" v-model.number="fLimit" min="1" max="1000" /></div>
        </div>
      </div>
    </div>
    <button type="button" class="krds-btn primary" :disabled="busy" @click="load">{{ busy ? '조회 중…' : '조회' }}</button>
    <button type="button" class="krds-btn secondary" :disabled="busy" @click="exportCsv" style="margin-left:var(--sp-8)">CSV 내보내기</button>

    <div class="mt16" style="overflow-x:auto">
      <div v-if="items.length" class="krds-table-wrap">
        <table class="tbl col data">
          <caption class="sr-only">감사로그: 시각, 주체, 역할, 행위, 상세</caption>
          <thead>
            <tr><th scope="col">시각</th><th scope="col">주체</th><th scope="col">역할</th><th scope="col">행위</th><th scope="col">상세</th></tr>
          </thead>
          <tbody>
            <tr v-for="a in items" :key="a.id">
              <td>{{ fmt(a.ts) }}</td><td>{{ a.actor }}</td><td>{{ a.role }}</td>
              <td><span class="krds-badge bg-light-information">{{ a.action }}</span></td><td>{{ a.detail }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <p v-else class="muted">감사로그가 없습니다.</p>
    </div>
  </section>
</template>
