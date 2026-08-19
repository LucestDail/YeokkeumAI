<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { postJson, getJson, uploadFile, deleteJson, type RagResult, type IngestResult, type DocItem } from '../api'

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
    notice.value = `색인 완료: ${r.filename} (${r.nChunks}청크)`
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
    notice.value = `업로드 색인 완료: ${r.filename} (${r.nChunks}청크)`
    if (fileInput.value) fileInput.value.value = ''
    await loadDocs()
  } catch (e) { error.value = String(e) } finally { busy.value = false }
}

async function removeDoc(d: DocItem) {
  if (busy.value) return
  if (!window.confirm(`'${d.filename}' 문서를 삭제할까요? (근거에서 제외됩니다)`)) return
  error.value = ''; notice.value = ''; busy.value = true
  try {
    await deleteJson<{ deleted: string }>('/api/docs/' + encodeURIComponent(d.id))
    notice.value = `삭제 완료: ${d.filename}`
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
  <section class="card" aria-labelledby="docs-h">
    <h2 id="docs-h">문서·검색 (RAG)</h2>
    <p class="desc">규정·지식 문서를 등록하고 근거 기반으로 검색합니다. 파일은 PDF·텍스트·HWP/HWPX를 지원합니다.</p>
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>
    <div v-if="notice" class="app-alert info" role="status">{{ notice }}</div>

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="up-file">파일 업로드</label></div>
        <div class="form-conts row">
          <input id="up-file" ref="fileInput" type="file" class="krds-input" accept=".pdf,.txt,.md,.hwp,.hwpx" />
          <div style="flex:0 0 auto">
            <button type="button" class="krds-btn secondary" :disabled="busy" @click="uploadSelected">업로드·색인</button>
          </div>
        </div>
        <p class="form-hint">PDF · 텍스트(.txt/.md) · HWP/HWPX 지원</p>
      </div>
    </div>

    <details style="margin: var(--sp-16) 0">
      <summary style="cursor:pointer; font-weight:700">텍스트 직접 등록</summary>
      <div class="fieldset mt16">
        <div class="form-group">
          <div class="form-tit"><label for="doc-name">문서명</label></div>
          <div class="form-conts"><input id="doc-name" type="text" class="krds-input" v-model="filename" placeholder="예: 개인정보보호지침.txt" /></div>
        </div>
        <div class="form-group">
          <div class="form-tit"><label for="doc-text">본문</label></div>
          <div class="form-conts"><textarea id="doc-text" class="krds-input" v-model="text" rows="5" placeholder="규정·지침 본문"></textarea></div>
        </div>
      </div>
      <button type="button" class="krds-btn secondary" :disabled="busy" @click="ingestText">텍스트 색인</button>
    </details>

    <p class="form-tit"><label>등록 문서 ({{ docs.length }})</label></p>
    <div v-if="docs.length" class="krds-table-wrap">
      <table class="tbl col data">
        <caption class="sr-only">등록된 문서 목록: 문서명, 글자수, 청크수</caption>
        <colgroup><col style="width:55%"><col><col><col style="width:80px"></colgroup>
        <thead>
          <tr><th scope="col">문서명</th><th scope="col">글자수</th><th scope="col">청크</th><th scope="col">관리</th></tr>
        </thead>
        <tbody>
          <tr v-for="d in docs" :key="d.id">
            <td>{{ d.filename }}</td><td>{{ d.chars }}</td><td>{{ d.nChunks ?? '—' }}</td>
            <td><button type="button" class="krds-btn tertiary small" :disabled="busy" @click="removeDoc(d)">삭제</button></td>
          </tr>
        </tbody>
      </table>
    </div>
    <p v-else class="muted">등록된 문서가 없습니다.</p>

    <hr style="border:none; border-top:1px solid var(--krds-gray-20); margin: var(--sp-24) 0" />

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="rag-q">근거 검색</label></div>
        <div class="form-conts row">
          <input id="rag-q" type="text" class="krds-input" v-model="query" placeholder="예: 개인정보 보관기간 규정" @keyup.enter="search" />
          <div style="flex:0 0 auto"><button type="button" class="krds-btn primary" :disabled="busy" @click="search">검색</button></div>
        </div>
      </div>
    </div>
    <div v-if="result" aria-live="polite">
      <p>
        <span v-if="result.grounded" class="krds-badge bg-light-success">근거 기반</span>
        <span v-else class="krds-badge bg-light-danger">근거 없음</span>
      </p>
      <div class="output">{{ result.answer }}</div>
      <div v-for="(c, i) in result.citations" :key="i" class="cite">
        <div class="meta">[근거 {{ i + 1 }}] {{ c.filename }} #{{ c.idx }} · score {{ c.score }}</div>
        <div>{{ c.snippet }}</div>
      </div>
    </div>
  </section>
</template>
