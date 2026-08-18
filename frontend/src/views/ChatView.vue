<script setup lang="ts">
import { ref } from 'vue'
import { streamChat } from '../api'

const message = ref('')
const output = ref('')
const busy = ref(false)
const error = ref('')

function send() {
  if (!message.value.trim() || busy.value) return
  error.value = ''
  output.value = ''
  busy.value = true
  streamChat(
    message.value,
    (t) => { output.value += t },
    () => { busy.value = false },
    (e) => { error.value = e; busy.value = false }
  )
}
</script>

<template>
  <section class="card" aria-labelledby="chat-h">
    <h2 id="chat-h">대화</h2>
    <p class="desc">벤더무관 게이트웨이를 통한 대화. 응답은 SSE로 실시간 스트리밍됩니다.</p>
    <div v-if="error" class="alert alert-danger" role="alert">{{ error }}</div>
    <div class="field">
      <label for="chat-msg">질문</label>
      <textarea id="chat-msg" v-model="message" placeholder="예: 공공 웹 접근성 준수 항목을 알려줘"
        @keydown.ctrl.enter="send"></textarea>
    </div>
    <button class="btn btn-primary" :disabled="busy" @click="send">
      {{ busy ? '생성 중…' : '보내기 (Ctrl+Enter)' }}
    </button>
    <div class="field" style="margin-top: var(--sp-16)">
      <label for="chat-out">응답</label>
      <div id="chat-out" class="output" aria-live="polite">{{ output || '—' }}</div>
    </div>
  </section>
</template>
