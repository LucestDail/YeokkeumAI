<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { streamChat } from '../api'

interface Msg { role: 'user' | 'ai'; text: string }

const { t } = useI18n()
const message = ref('')
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

function send(text?: string) {
  const q = (text ?? message.value).trim()
  if (!q || busy.value) return
  error.value = ''
  messages.value.push({ role: 'user', text: q })
  messages.value.push({ role: 'ai', text: '' })
  const aiIdx = messages.value.length - 1
  message.value = ''
  busy.value = true
  scrollBottom()
  streamChat(
    q,
    (tok) => { messages.value[aiIdx].text += tok; scrollBottom() },
    () => { busy.value = false; scrollBottom() },
    (e) => { error.value = e; messages.value.splice(aiIdx, 1); busy.value = false }
  )
}
</script>

<template>
  <section class="card">
    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="chat-thread" ref="threadEl">
      <div v-if="!messages.length" class="chat-empty">
        <p class="lead">{{ t('chat.qph') }}</p>
        <div class="chips">
          <button v-for="ex in examples" :key="ex" type="button" class="chip" @click="send(ex)">{{ ex }}</button>
        </div>
      </div>
      <div v-for="(m, i) in messages" :key="i" class="msg" :class="m.role === 'user' ? 'msg-user' : 'msg-ai'">
        <span class="msg-role">{{ m.role === 'user' ? '나' : t('brand.name') }}</span>
        <div class="msg-body">{{ m.text || '…' }}</div>
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
