<script setup lang="ts">
import { ref } from 'vue'
import { postJson } from '../api'

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
    <h2 id="write-h">문서 작성·요약</h2>
    <p class="desc">보고서·공문 초안 작성과 긴 문서 요약. 모든 요청은 감사로그에 기록됩니다.</p>

    <!-- KRDS tab -->
    <div class="krds-tab-area">
      <div class="tab line full">
        <ul role="tablist" aria-label="작성 모드">
          <li id="tab_sum" role="tab" :aria-selected="tab === 'summarize'"
              aria-controls="panel_sum" :class="{ active: tab === 'summarize' }">
            <button type="button" class="btn-tab" @click="tab = 'summarize'">요약</button>
          </li>
          <li id="tab_draft" role="tab" :aria-selected="tab === 'draft'"
              aria-controls="panel_draft" :class="{ active: tab === 'draft' }">
            <button type="button" class="btn-tab" @click="tab = 'draft'">초안 작성</button>
          </li>
        </ul>
      </div>

      <div class="tab-conts-wrap">
        <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

        <section v-show="tab === 'summarize'" id="panel_sum" role="tabpanel" aria-labelledby="tab_sum" class="tab-conts active">
          <div class="fieldset">
            <div class="form-group">
              <div class="form-tit"><label for="sum-text">원문</label></div>
              <div class="form-conts">
                <textarea id="sum-text" class="krds-input" v-model="text" rows="5" placeholder="요약할 텍스트를 붙여넣으세요"></textarea>
              </div>
            </div>
          </div>
          <button type="button" class="krds-btn primary" :disabled="busy" @click="summarize">{{ busy ? '요약 중…' : '3문장 요약' }}</button>
          <div class="fieldset mt16">
            <div class="form-group">
              <div class="form-tit"><label for="sum-out">요약</label></div>
              <div class="output" id="sum-out" aria-live="polite">{{ summary || '—' }}</div>
            </div>
          </div>
        </section>

        <section v-show="tab === 'draft'" id="panel_draft" role="tabpanel" aria-labelledby="tab_draft" class="tab-conts" :class="{ active: tab === 'draft' }">
          <div class="fieldset">
            <div class="form-group">
              <div class="form-tit"><label for="draft-kind">문서 종류</label></div>
              <div class="form-conts" style="max-width: 28rem">
                <input id="draft-kind" type="text" class="krds-input" v-model="kind" placeholder="보고서 / 공문 / 보도자료" />
              </div>
            </div>
            <div class="form-group">
              <div class="form-tit"><label for="draft-brief">작성 지시</label></div>
              <div class="form-conts">
                <textarea id="draft-brief" class="krds-input" v-model="brief" rows="5" placeholder="예: 노후 정수장 개선 사업 추진계획 보고서"></textarea>
              </div>
            </div>
          </div>
          <button type="button" class="krds-btn primary" :disabled="busy" @click="makeDraft">{{ busy ? '작성 중…' : '초안 생성' }}</button>
          <div class="fieldset mt16">
            <div class="form-group">
              <div class="form-tit"><label for="draft-out">초안</label></div>
              <div class="output" id="draft-out" aria-live="polite">{{ draft || '—' }}</div>
            </div>
          </div>
        </section>
      </div>
    </div>
  </section>
</template>
