<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getToken, setToken } from './api'
import { setLocale } from './i18n'
import ChatView from './views/ChatView.vue'
import WriteView from './views/WriteView.vue'
import ReviewView from './views/ReviewView.vue'
import DocsView from './views/DocsView.vue'
import AgentView from './views/AgentView.vue'
import AuditView from './views/AuditView.vue'

type TabKey = 'chat' | 'write' | 'review' | 'docs' | 'agent' | 'audit'

const { t, locale } = useI18n()

const tabs: TabKey[] = ['chat', 'write', 'review', 'docs', 'agent', 'audit']
const views = { chat: ChatView, write: WriteView, review: ReviewView, docs: DocsView, agent: AgentView, audit: AuditView }

const active = ref<TabKey>('chat')
const current = computed(() => views[active.value])

const token = ref(getToken())
watch(token, (t) => setToken(t))
</script>

<template>
  <a href="#main" class="skip-link">{{ t('skip') }}</a>

  <header class="header">
    <div class="header-inner">
      <div class="brand">
        <span class="emblem" aria-hidden="true">엮</span>
        <strong>{{ t('brand.name') }}</strong>
        <span>{{ t('brand.tagline') }}</span>
      </div>
      <nav class="appnav" :aria-label="t('brand.name')">
        <button
          v-for="k in tabs"
          :key="k"
          type="button"
          :aria-current="active === k ? 'page' : undefined"
          @click="active = k"
        >{{ t('nav.' + k) }}</button>
      </nav>
    </div>
  </header>

  <div class="tokenbar">
    <div class="container">
      <label for="token" class="tokenbar-label">{{ t('token.label') }}</label>
      <input id="token" type="password" class="krds-input" v-model="token"
        :placeholder="t('token.placeholder')" autocomplete="off" />
      <span class="muted">{{ t('token.note') }}</span>
      <span style="margin-left:auto; display:flex; gap:var(--sp-4); align-items:center">
        <span class="sr-only">{{ t('lang.label') }}</span>
        <button type="button" class="krds-btn tertiary small" :aria-pressed="locale === 'ko'"
          :style="locale === 'ko' ? 'font-weight:700' : ''" @click="setLocale('ko')">KO</button>
        <button type="button" class="krds-btn tertiary small" :aria-pressed="locale === 'en'"
          :style="locale === 'en' ? 'font-weight:700' : ''" @click="setLocale('en')">EN</button>
      </span>
    </div>
  </div>

  <main id="main">
    <div class="container">
      <component :is="current" />
    </div>
  </main>

  <footer class="footer">
    <div class="container">
      {{ t('footer.text') }} ·
      <a href="https://www.krds.go.kr" target="_blank" rel="noopener">KRDS</a>
    </div>
  </footer>
</template>
