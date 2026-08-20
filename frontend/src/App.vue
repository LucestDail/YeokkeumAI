<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { getToken, setToken, login } from './api'
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
watch(token, (v) => setToken(v))

// 로그인 상태
const role = ref(localStorage.getItem('yk_role') || '')
const showLogin = ref(false)
const loginUser = ref('')
const loginPass = ref('')
const loginError = ref('')
const showTokenField = ref(false)

function persistRole(r: string) {
  role.value = r
  try { r ? localStorage.setItem('yk_role', r) : localStorage.removeItem('yk_role') } catch { /* ignore */ }
}
async function guestLogin() {
  loginError.value = ''
  try { const r = await login({ guest: true }); token.value = r.token; persistRole(r.role) }
  catch (e) { loginError.value = String(e) }
}
async function doLogin() {
  loginError.value = ''
  try {
    const r = await login({ username: loginUser.value, password: loginPass.value })
    token.value = r.token; persistRole(r.role); showLogin.value = false; loginPass.value = ''
  } catch { loginError.value = t('login.fail') }
}
function doLogout() { token.value = ''; persistRole(''); showLogin.value = false }

// 글자 크기(KRDS 화면 설정) — html font-size(62.5% 기준) 조절
const SCALES = [56.25, 62.5, 70]
const fontLevel = ref(1)
function applyFont(level: number) {
  fontLevel.value = level
  document.documentElement.style.fontSize = SCALES[level] + '%'
  try { localStorage.setItem('yk_fontscale', String(level)) } catch { /* ignore */ }
}
onMounted(() => {
  const saved = Number(localStorage.getItem('yk_fontscale'))
  applyFont(Number.isInteger(saved) && saved >= 0 && saved <= 2 ? saved : 1)
  document.documentElement.lang = locale.value
})
</script>

<template>
  <a href="#main" class="skip-link">{{ t('skip') }}</a>

  <!-- 1. 마스트헤드 (전자정부 식별 패턴 준용 · 정직 문구) -->
  <div class="masthead">
    <div class="container">
      <span class="flag" aria-hidden="true">🇰🇷</span>
      <span class="m-txt">{{ t('chrome.masthead') }}</span>
    </div>
  </div>

  <!-- 2. 헤더 (로고 + 유틸리티) -->
  <header class="site-header">
    <div class="container">
      <a href="#main" class="brand">
        <span class="emblem" aria-hidden="true">엮</span>
        <strong>{{ t('brand.name') }}</strong>
        <span class="tagline">{{ t('brand.tagline') }}</span>
      </a>
      <div class="header-util">
        <div class="u-group" role="group" :aria-label="t('chrome.fontSize')">
          <span class="u-label">{{ t('chrome.fontSize') }}</span>
          <button type="button" class="u-btn" :aria-pressed="fontLevel === 0" :aria-label="t('chrome.smaller')" @click="applyFont(0)">A−</button>
          <button type="button" class="u-btn" :aria-pressed="fontLevel === 1" :aria-label="t('chrome.normal')" @click="applyFont(1)">A</button>
          <button type="button" class="u-btn" :aria-pressed="fontLevel === 2" :aria-label="t('chrome.larger')" @click="applyFont(2)">A+</button>
        </div>
        <span class="u-sep" aria-hidden="true"></span>
        <div class="u-group" role="group" :aria-label="t('lang.label')">
          <button type="button" class="u-btn" :aria-pressed="locale === 'ko'" @click="setLocale('ko')">KO</button>
          <button type="button" class="u-btn" :aria-pressed="locale === 'en'" @click="setLocale('en')">EN</button>
        </div>
      </div>
    </div>
  </header>

  <!-- 3. GNB (주메뉴) -->
  <nav class="gnb" :aria-label="t('brand.name')">
    <div class="container">
      <button
        v-for="k in tabs"
        :key="k"
        type="button"
        :aria-current="active === k ? 'page' : undefined"
        @click="active = k"
      >{{ t('nav.' + k) }}</button>
    </div>
  </nav>

  <!-- 인증(로그인) 유틸 -->
  <div class="authbar">
    <div class="container">
      <template v-if="token">
        <span class="muted">{{ t('login.loggedIn') }} · {{ role === 'admin' ? '관리자' : (role || '사용자') }}</span>
        <button type="button" class="krds-btn small tertiary" @click="doLogout">{{ t('login.logout') }}</button>
      </template>
      <template v-else>
        <button type="button" class="krds-btn small primary" @click="guestLogin">{{ t('login.guest') }}</button>
        <button type="button" class="krds-btn small tertiary" @click="showLogin = !showLogin">{{ t('login.admin') }}</button>
        <span v-if="showLogin" class="login-inline">
          <input type="text" class="krds-input" v-model="loginUser" :placeholder="t('login.id')" autocomplete="username" />
          <input type="password" class="krds-input" v-model="loginPass" :placeholder="t('login.pw')" autocomplete="current-password" @keyup.enter="doLogin" />
          <button type="button" class="krds-btn small secondary" @click="doLogin">{{ t('login.submit') }}</button>
        </span>
        <button type="button" class="u-btn" @click="showTokenField = !showTokenField">{{ t('login.advanced') }}</button>
        <input v-if="showTokenField" id="token" type="password" class="krds-input" v-model="token" :placeholder="t('token.placeholder')" autocomplete="off" style="max-width:26rem" />
      </template>
      <span v-if="loginError" class="muted" style="color:var(--krds-danger)">{{ loginError }}</span>
    </div>
  </div>

  <main id="main">
    <div class="container">
      <!-- 4. 브레드크럼 -->
      <nav class="krds-breadcrumb-wrap" :aria-label="t('chrome.home')">
        <ol class="breadcrumb">
          <li class="home"><a href="#main" class="txt">{{ t('chrome.home') }}</a></li>
          <li><span class="txt">{{ t('nav.' + active) }}</span></li>
        </ol>
      </nav>

      <!-- 5. 페이지 타이틀 영역 -->
      <div class="page-title">
        <h1>{{ t(active + '.h') }}</h1>
        <p>{{ t(active + '.desc') }}</p>
      </div>

      <!-- 6. 본문 -->
      <component :is="current" />
    </div>
  </main>

  <!-- 7. 푸터 + 운영기관 식별자 -->
  <footer class="site-footer">
    <div class="container">
      <div class="f-brand">
        <span class="emblem" aria-hidden="true">엮</span>
        <strong>{{ t('brand.name') }}</strong>
      </div>
      <p>{{ t('chrome.footerNote') }}</p>
      <div class="f-menu">
        <a href="#" class="point">{{ t('chrome.privacy') }}</a>
        <a href="#">{{ t('chrome.terms') }}</a>
        <a href="#">{{ t('chrome.a11y') }}</a>
        <a href="https://www.krds.go.kr" target="_blank" rel="noopener">KRDS</a>
      </div>
      <p class="f-copy">© {{ t('chrome.copyright') }}</p>
    </div>
  </footer>
</template>
