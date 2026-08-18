<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getJson, type AuditItem } from '../api'

const items = ref<AuditItem[]>([])
const error = ref('')
const busy = ref(false)

function fmt(ts: number): string {
  try { return new Date(ts).toLocaleString('ko-KR') } catch { return String(ts) }
}

async function load() {
  error.value = ''; busy.value = true
  try {
    const r = await getJson<{ items: AuditItem[] }>('/api/audit')
    items.value = r.items
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

onMounted(load)
</script>

<template>
  <section class="card" aria-labelledby="audit-h">
    <h2 id="audit-h">감사로그</h2>
    <p class="desc">모든 요청의 <strong>주체·시각·행위·모델</strong> 기록. <strong>admin</strong> 권한이 필요합니다.</p>
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>
    <button type="button" class="krds-btn secondary" :disabled="busy" @click="load">{{ busy ? '조회 중…' : '새로고침' }}</button>

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
