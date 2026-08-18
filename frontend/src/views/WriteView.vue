<script setup lang="ts">
import { ref } from 'vue'
import { postJson } from '../api'

const tab = ref<'summarize' | 'draft'>('summarize')

// 요약
const text = ref('')
const summary = ref('')
// 초안
const kind = ref('보고서')
const brief = ref('')
const draft = ref('')

const busy = ref(false)
const error = ref('')

async function summarize() {
  if (!text.value.trim() || busy.value) return
  error.value = ''; summary.value = ''; busy.value = true
  try {
    const r = await postJson<{ summary: string }>('/api/summarize', { text: text.value })
    summary.value = r.summary
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}
async function makeDraft() {
  if (!brief.value.trim() || busy.value) return
  error.value = ''; draft.value = ''; busy.value = true
  try {
    const r = await postJson<{ draft: string }>('/api/draft', { kind: kind.value, brief: brief.value })
    draft.value = r.draft
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}
</script>

<template>
  <section class="card" aria-labelledby="write-h">
    <h2 id="write-h">문서 작성·요약</h2>
    <p class="desc">보고서·공문 초안 작성과 긴 문서 요약. 모든 요청은 감사로그에 기록됩니다.</p>

    <div class="nav" role="tablist" aria-label="작성 모드" style="border-bottom:1px solid var(--krds-gray-20); margin-bottom: var(--sp-16)">
      <button role="tab" :aria-current="tab==='summarize' ? 'page' : undefined" @click="tab='summarize'">요약</button>
      <button role="tab" :aria-current="tab==='draft' ? 'page' : undefined" @click="tab='draft'">초안 작성</button>
    </div>

    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>

    <div v-if="tab==='summarize'">
      <div class="field">
        <label for="sum-text">원문</label>
        <textarea id="sum-text" v-model="text" placeholder="요약할 텍스트를 붙여넣으세요"></textarea>
      </div>
      <button class="btn btn-primary" :disabled="busy" @click="summarize">{{ busy ? '요약 중…' : '3문장 요약' }}</button>
      <div class="field" style="margin-top: var(--sp-16)">
        <label for="sum-out">요약</label>
        <div id="sum-out" class="output" aria-live="polite">{{ summary || '—' }}</div>
      </div>
    </div>

    <div v-else>
      <div class="row">
        <div class="field">
          <label for="draft-kind">문서 종류</label>
          <input id="draft-kind" type="text" v-model="kind" placeholder="보고서 / 공문 / 보도자료" />
        </div>
      </div>
      <div class="field">
        <label for="draft-brief">작성 지시</label>
        <textarea id="draft-brief" v-model="brief" placeholder="예: 노후 정수장 개선 사업 추진계획 보고서"></textarea>
      </div>
      <button class="btn btn-primary" :disabled="busy" @click="makeDraft">{{ busy ? '작성 중…' : '초안 생성' }}</button>
      <div class="field" style="margin-top: var(--sp-16)">
        <label for="draft-out">초안</label>
        <div id="draft-out" class="output" aria-live="polite">{{ draft || '—' }}</div>
      </div>
    </div>
  </section>
</template>
