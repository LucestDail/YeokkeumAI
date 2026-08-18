<script setup lang="ts">
import { ref } from 'vue'
import { postJson, type RagResult } from '../api'

const text = ref('')
const lang = ref('ko')
const result = ref<RagResult | null>(null)
const busy = ref(false)
const error = ref('')

const langs = [
  { v: 'ko', label: '한국어' },
  { v: 'en', label: 'English' },
  { v: 'vi', label: 'Tiếng Việt' },
  { v: 'zh', label: '中文' }
]

async function review() {
  if (!text.value.trim() || busy.value) return
  error.value = ''; result.value = null; busy.value = true
  try {
    result.value = await postJson<RagResult>('/api/review', { text: text.value, lang: lang.value })
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}
</script>

<template>
  <section class="card" aria-labelledby="review-h">
    <h2 id="review-h">규정검토</h2>
    <p class="desc">
      작성물을 <strong>등록된 규정·근거 문서</strong>에 비추어 위반·리스크를 항목별로 지적하고 수정문안을 제시합니다.
      근거 없는 사항은 지어내지 않고 <em>근거 범위 밖</em>으로 명시합니다. (규정 문서는 「문서·검색」에서 먼저 등록)
    </p>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>

    <div class="row">
      <div class="field" style="max-width: 200px; flex: 0 0 200px">
        <label for="rev-lang">답변 언어</label>
        <select id="rev-lang" v-model="lang">
          <option v-for="l in langs" :key="l.v" :value="l.v">{{ l.label }}</option>
        </select>
      </div>
    </div>
    <div class="field">
      <label for="rev-text">검토 대상(작성물)</label>
      <textarea id="rev-text" v-model="text" placeholder="검토받을 공문·계획서·약관 등을 붙여넣으세요"></textarea>
    </div>
    <button class="btn btn-primary" :disabled="busy" @click="review">{{ busy ? '검토 중…' : '규정검토 실행' }}</button>

    <div v-if="result" style="margin-top: var(--sp-24)" aria-live="polite">
      <p>
        <span v-if="result.grounded" class="tag tag-success">근거 기반</span>
        <span v-else class="tag tag-danger">근거 없음</span>
        <span class="muted"> · 모델 {{ result.model }}</span>
      </p>
      <div class="field">
        <label>검토 결과</label>
        <div class="output">{{ result.answer }}</div>
      </div>
      <div v-if="result.citations.length">
        <label>근거 인용 ({{ result.citations.length }})</label>
        <div v-for="(c, i) in result.citations" :key="i" class="cite">
          <div class="meta">[근거 {{ i + 1 }}] {{ c.filename }} #{{ c.idx }} · score {{ c.score }}</div>
          <div>{{ c.snippet }}</div>
        </div>
      </div>
    </div>
  </section>
</template>
