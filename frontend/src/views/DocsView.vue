<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { postJson, getJson, uploadFile, deleteJson, type RagResult, type IngestResult, type DocItem } from '../api'
import { renderMarkdown } from '../md'

const { t } = useI18n()
const docs = ref<DocItem[]>([])
const error = ref('')
const notice = ref('')

const filename = ref('')
const text = ref('')
const fileInput = ref<HTMLInputElement | null>(null)
const query = ref('')
const result = ref<RagResult | null>(null)
const busy = ref(false)

async function loadDocs() {
  try {
    const r = await getJson<{ items: DocItem[] }>('/api/docs')
    docs.value = r.items
  } catch (e) { error.value = String(e) }
}

async function ingestText() {
  if (!filename.value.trim() || !text.value.trim() || busy.value) return
  error.value = ''; notice.value = ''; busy.value = true
  try {
    const r = await postJson<IngestResult>('/api/docs', { filename: filename.value, text: text.value })
    notice.value = `${r.filename} (${r.nChunks})`
    filename.value = ''; text.value = ''
    await loadDocs()
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

async function uploadSelected() {
  const f = fileInput.value?.files?.[0]
  if (!f || busy.value) return
  error.value = ''; notice.value = ''; busy.value = true
  try {
    const r = await uploadFile<IngestResult>('/api/docs/upload', f)
    notice.value = `${r.filename} (${r.nChunks})`
    if (fileInput.value) fileInput.value.value = ''
    await loadDocs()
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

async function removeDoc(d: DocItem) {
  if (busy.value) return
  if (!window.confirm(t('docs.confirmDelete'))) return
  error.value = ''; notice.value = ''; busy.value = true
  try {
    await deleteJson<{ deleted: string }>('/api/docs/' + encodeURIComponent(d.id))
    notice.value = d.filename
    await loadDocs()
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

async function search() {
  if (!query.value.trim() || busy.value) return
  error.value = ''; result.value = null; busy.value = true
  try {
    result.value = await postJson<RagResult>('/api/rag/query', { query: query.value })
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

onMounted(loadDocs)
</script>

<template>
  <section class="card">
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>
    <div v-if="notice" class="app-alert info" role="status">{{ notice }}</div>

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="up-file">{{ t('docs.upload') }}</label></div>
        <div class="form-conts row">
          <input id="up-file" ref="fileInput" type="file" class="krds-input" accept=".pdf,.txt,.md,.hwp,.hwpx,.docx,.xlsx" />
          <div style="flex:0 0 auto"><button type="button" class="krds-btn secondary" :disabled="busy" @click="uploadSelected">{{ t('docs.uploadBtn') }}</button></div>
        </div>
        <p class="form-hint">{{ t('docs.hint') }}</p>
      </div>
    </div>

    <details style="margin: var(--sp-16) 0">
      <summary style="cursor:pointer; font-weight:700">{{ t('docs.textReg') }}</summary>
      <div class="fieldset mt16">
        <div class="form-group">
          <div class="form-tit"><label for="doc-name">{{ t('docs.docName') }}</label></div>
          <div class="form-conts"><input id="doc-name" type="text" class="krds-input" v-model="filename" :placeholder="t('docs.docNameph')" /></div>
        </div>
        <div class="form-group">
          <div class="form-tit"><label for="doc-text">{{ t('docs.body') }}</label></div>
          <div class="form-conts"><textarea id="doc-text" class="krds-input" v-model="text" rows="5" :placeholder="t('docs.bodyph')"></textarea></div>
        </div>
      </div>
      <button type="button" class="krds-btn secondary" :disabled="busy" @click="ingestText">{{ t('docs.textIndex') }}</button>
    </details>

    <p class="form-tit"><label>{{ t('docs.listLabel') }} ({{ docs.length }})</label></p>
    <div v-if="docs.length" class="krds-table-wrap">
      <table class="tbl col data">
        <caption class="sr-only">{{ t('docs.listLabel') }}</caption>
        <colgroup><col style="width:55%"><col><col><col style="width:80px"></colgroup>
        <thead>
          <tr><th scope="col">{{ t('docs.colName') }}</th><th scope="col">{{ t('docs.colChars') }}</th><th scope="col">{{ t('docs.colChunks') }}</th><th scope="col">{{ t('docs.colManage') }}</th></tr>
        </thead>
        <tbody>
          <tr v-for="d in docs" :key="d.id">
            <td>{{ d.filename }}</td><td>{{ d.chars }}</td><td>{{ d.nChunks ?? '—' }}</td>
            <td><button type="button" class="krds-btn tertiary small" :disabled="busy" @click="removeDoc(d)">{{ t('docs.delete') }}</button></td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else class="muted">{{ t('docs.noDocs') }}</p>

    <hr style="border:none; border-top:1px solid var(--krds-gray-20); margin: var(--sp-24) 0" />

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="rag-q">{{ t('docs.search') }}</label></div>
        <div class="form-conts row">
          <input id="rag-q" type="text" class="krds-input" v-model="query" :placeholder="t('docs.searchph')" @keyup.enter="search" />
          <div style="flex:0 0 auto"><button type="button" class="krds-btn primary" :disabled="busy" @click="search">{{ t('docs.searchBtn') }}</button></div>
        </div>
      </div>
    </div>
    <div v-if="result" aria-live="polite">
      <p>
        <span v-if="result.grounded" class="krds-badge bg-light-success">{{ t('docs.grounded') }}</span>
        <span v-else class="krds-badge bg-light-danger">{{ t('docs.ungrounded') }}</span>
      </p>
      <div class="output md" v-html="renderMarkdown(result.answer)"></div>
      <div v-for="(c, i) in result.citations" :key="i" class="cite">
        <div class="meta">[{{ i + 1 }}] {{ c.filename }} #{{ c.idx }} · score {{ c.score }}</div>
        <div>{{ c.snippet }}</div>
      </div>
    </div>
  </section>
</template>
