<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { postJson } from '../api'

const { t } = useI18n()
const tab = ref<'summarize' | 'draft'>('summarize')

const text = ref('')
const summary = ref('')
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
    <h2 id="write-h">{{ t('write.h') }}</h2>
    <p class="desc">{{ t('write.desc') }}</p>

    <div class="krds-tab-area">
      <div class="tab line full">
        <ul role="tablist" :aria-label="t('write.h')">
          <li id="tab_sum" role="tab" :aria-selected="tab === 'summarize'" aria-controls="panel_sum" :class="{ active: tab === 'summarize' }">
            <button type="button" class="btn-tab" @click="tab = 'summarize'">{{ t('write.tabSum') }}</button>
          </li>
          <li id="tab_draft" role="tab" :aria-selected="tab === 'draft'" aria-controls="panel_draft" :class="{ active: tab === 'draft' }">
            <button type="button" class="btn-tab" @click="tab = 'draft'">{{ t('write.tabDraft') }}</button>
          </li>
        </ul>
      </div>

      <div class="tab-conts-wrap">
        <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

        <section v-show="tab === 'summarize'" id="panel_sum" role="tabpanel" aria-labelledby="tab_sum" class="tab-conts active">
          <div class="fieldset">
            <div class="form-group">
              <div class="form-tit"><label for="sum-text">{{ t('write.src') }}</label></div>
              <div class="form-conts"><textarea id="sum-text" class="krds-input" v-model="text" rows="5" :placeholder="t('write.srcph')"></textarea></div>
            </div>
          </div>
          <button type="button" class="krds-btn primary" :disabled="busy" @click="summarize">{{ busy ? t('write.summarizing') : t('write.sum3') }}</button>
          <div class="fieldset mt16">
            <div class="form-group">
              <div class="form-tit"><label for="sum-out">{{ t('write.sumOut') }}</label></div>
              <div class="output" id="sum-out" aria-live="polite">{{ summary || t('common.none') }}</div>
            </div>
          </div>
        </section>

        <section v-show="tab === 'draft'" id="panel_draft" role="tabpanel" aria-labelledby="tab_draft" class="tab-conts" :class="{ active: tab === 'draft' }">
          <div class="fieldset">
            <div class="form-group">
              <div class="form-tit"><label for="draft-kind">{{ t('write.kind') }}</label></div>
              <div class="form-conts" style="max-width: 28rem"><input id="draft-kind" type="text" class="krds-input" v-model="kind" :placeholder="t('write.kindph')" /></div>
            </div>
            <div class="form-group">
              <div class="form-tit"><label for="draft-brief">{{ t('write.brief') }}</label></div>
              <div class="form-conts"><textarea id="draft-brief" class="krds-input" v-model="brief" rows="5" :placeholder="t('write.briefph')"></textarea></div>
            </div>
          </div>
          <button type="button" class="krds-btn primary" :disabled="busy" @click="makeDraft">{{ busy ? t('write.generating') : t('write.gen') }}</button>
          <div class="fieldset mt16">
            <div class="form-group">
              <div class="form-tit"><label for="draft-out">{{ t('write.draftOut') }}</label></div>
              <div class="output" id="draft-out" aria-live="polite">{{ draft || t('common.none') }}</div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>
