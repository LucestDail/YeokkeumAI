<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { streamChat, streamSSE } from '../api'
import { renderMarkdown } from '../md'

interface Msg { role: 'user' | 'ai'; text: string }

const { t } = useI18n()
const message = ref('')
const mode = ref<'normal' | 'gov24'>('normal')
const messages = ref<Msg[]>([])
const busy = ref(false)
const error = ref('')
const threadEl = ref<HTMLElement | null>(null)

const examples = [
  '공공 웹 접근성 준수 항목을 알려줘',
  '개인정보 수집 시 유의사항은?',
  '전자정부법의 목적을 한 문장으로'
]

async function scrollBottom() {
  await nextTick()
  if (threadEl.value) threadEl.value.scrollTop = threadEl.value.scrollHeight
}

async function send(text?: string) {
  const q = (text ?? message.value).trim()
  if (!q || busy.value) return
  error.value = ''
  messages.value.push({ role: 'user', text: q })
  messages.value.push({ role: 'ai', text: mode.value === 'gov24' ? t('chat.gov24Searching') : '' })
  const aiIdx = messages.value.length - 1
  message.value = ''
  busy.value = true
  scrollBottom()
  let started = false
  const onTok = (tok: string) => {
    if (!started) { messages.value[aiIdx].text = ''; started = true } // 진행상태 placeholder 제거
    messages.value[aiIdx].text += tok; scrollBottom()
  }
  const onDone = () => { busy.value = false; scrollBottom() }
  const onErr = (e: string) => { error.value = e; messages.value.splice(aiIdx, 1); busy.value = false }
  if (mode.value === 'gov24') {
    streamSSE('/api/gov24/chat', { query: q }, onTok, onDone, onErr,
      (p) => { if (!started) messages.value[aiIdx].text = p + ' …' })
    return
  }
  streamChat(q, onTok, onDone, onErr)
}
</script>

<template>
  <section class="card">
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="chat-modes" role="group" aria-label="mode">
      <button type="button" :aria-pressed="mode === 'normal'" @click="mode = 'normal'">{{ t('chat.modeNormal') }}</button>
      <button type="button" :aria-pressed="mode === 'gov24'" @click="mode = 'gov24'">{{ t('chat.modeGov24') }}</button>
    </div>

    <div class="chat-thread" ref="threadEl">
      <div v-if="!messages.length" class="chat-empty">
        <p class="lead">{{ t('chat.qph') }}</p>
        <div class="chips">
          <button v-for="ex in examples" :key="ex" type="button" class="chip" @click="send(ex)">{{ ex }}</button>
        </div>
      </div>
      <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role === 'user' ? 'msg-user' : 'msg-ai'">
        <span class="msg-role">{{ m.role === 'user' ? '나' : t('brand.name') }}</span>
        <div v-if="m.role === 'ai'" class="msg-body md" v-html="renderMarkdown(m.text) || '…'"></div>
        <div v-else class="msg-body">{{ m.text }}</div>
      </div>
    </div>
    <p class="sr-only" role="status" aria-live="polite">{{ busy ? t('chat.genBusy') : '' }}</p>

    <div class="chat-inputbar">
      <label for="chat-msg" class="sr-only">{{ t('chat.q') }}</label>
      <textarea id="chat-msg" class="krds-input" v-model="message" rows="2"
        :placeholder="t('chat.qph')" @keydown.ctrl.enter="send()" @keydown.meta.enter="send()"></textarea>
      <button type="button" class="krds-btn primary" :disabled="busy" @click="send()">
        {{ busy ? t('chat.sending') : t('chat.send') }}
      </button>
    </div>
  </section>
</template>
