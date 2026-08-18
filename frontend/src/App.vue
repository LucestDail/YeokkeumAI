<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { getToken, setToken } from './api'
import ChatView from './views/ChatView.vue'
import WriteView from './views/WriteView.vue'
import ReviewView from './views/ReviewView.vue'
import DocsView from './views/DocsView.vue'
import AuditView from './views/AuditView.vue'

type TabKey = 'chat' | 'write' | 'review' | 'docs' | 'audit'

const tabs: { key: TabKey; label: string }[] = [
  { key: 'chat', label: '대화' },
  { key: 'write', label: '문서 작성·요약' },
  { key: 'review', label: '규정검토' },
  { key: 'docs', label: '문서·검색' },
  { key: 'audit', label: '감사로그' }
]

const views = { chat: ChatView, write: WriteView, review: ReviewView, docs: DocsView, audit: AuditView }

const active = ref<TabKey>('chat')
const current = computed(() => views[active.value])

const token = ref(getToken())
watch(token, (t) => setToken(t))
</script>

<template>
  <a href="#main" class="skip-link">본문 바로가기</a>

  <header class="header">
    <div class="header-inner">
      <div class="brand">
        <strong>엮음AI</strong>
        <span>공공기관 업무보조 AI · 통제·책임성·연계</span>
      </div>
      <nav class="appnav" aria-label="주요 기능">
        <button
          v-for="t in tabs"
          :key="t.key"
          type="button"
          :aria-current="active === t.key ? 'page' : undefined"
          @click="active = t.key"
        >{{ t.label }}</button>
      </nav>
    </div>
  </header>

  <div class="tokenbar">
    <div class="container">
      <label for="token" class="tokenbar-label">API 토큰</label>
      <input id="token" type="password" class="krds-input" v-model="token"
        placeholder="Bearer 토큰 (secure-by-default: 미설정 시 API 차단)" autocomplete="off" />
      <span class="muted">브라우저에만 저장됩니다</span>
    </div>
  </div>

  <main id="main">
    <div class="container">
      <component :is="current" />
    </div>
  </main>

  <footer class="footer">
    <div class="container">
      엮음AI — 온프렘/내부망 벤더무관 AI 게이트웨이 · 공식 KRDS 디자인시스템 기반 ·
      <a href="https://www.krds.go.kr" target="_blank" rel="noopener">KRDS</a> ·
      웹접근성(KWCAG 2.2) 준수 지향
    </div>
  </footer>
</template>
