<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { postJson, type RagResult } from '../api'
import { renderMarkdown } from '../md'

const { t } = useI18n()
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
  <section class="card">
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="rev-lang">{{ t('review.lang') }}</label></div>
        <div class="form-conts" style="max-width: 20rem">
          <select id="rev-lang" class="krds-form-select" v-model="lang">
            <option v-for="l in langs" :key="l.v" :value="l.v">{{ l.label }}</option>
          </select>
        </div>
      </div>
      <div class="form-group">
        <div class="form-tit"><label for="rev-text">{{ t('review.target') }}</label></div>
        <div class="form-conts"><textarea id="rev-text" class="krds-input" v-model="text" rows="6" :placeholder="t('review.targetph')"></textarea></div>
      </div>
    </div>
    <button type="button" class="krds-btn primary" :disabled="busy" @click="review">{{ busy ? t('review.running') : t('review.run') }}</button>

    <div v-if="result" class="mt16" aria-live="polite">
      <p>
        <span v-if="result.grounded" class="krds-badge bg-light-success">{{ t('review.grounded') }}</span>
        <span v-else class="krds-badge bg-light-danger">{{ t('review.ungrounded') }}</span>
        <span class="muted"> · {{ t('review.model') }} {{ result.model }}</span>
      </p>
      <div class="fieldset">
        <div class="form-group">
          <div class="form-tit"><label>{{ t('review.resultLabel') }}</label></div>
          <div class="output md" v-html="renderMarkdown(result.answer)"></div>
        </div>
      </div>
      <div v-if="result.citations.length">
        <p class="form-tit"><label>{{ t('review.citations') }} ({{ result.citations.length }})</label></p>
        <div v-for="(c, i) in result.citations" :key="i" class="cite">
          <div class="meta">[{{ i + 1 }}] {{ c.filename }} #{{ c.idx }} · score {{ c.score }}</div>
          <div>{{ c.snippet }}</div>
        </div>
      </div>
    </div>
  </section>
</template>
