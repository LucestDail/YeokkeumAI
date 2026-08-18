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

    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="chat-msg">질문</label></div>
        <div class="form-conts">
          <textarea id="chat-msg" class="krds-input" v-model="message" rows="4"
            placeholder="예: 공공 웹 접근성 준수 항목을 알려줘" @keydown.ctrl.enter="send"></textarea>
        </div>
      </div>
    </div>

    <button type="button" class="krds-btn primary" :disabled="busy" @click="send">
      {{ busy ? '생성 중…' : '보내기 (Ctrl+Enter)' }}
    </button>

    <div class="fieldset mt16">
      <div class="form-group">
        <div class="form-tit"><label for="chat-out">응답</label></div>
        <div class="output" id="chat-out" aria-live="polite">{{ output || '—' }}</div>
      </div>
    </div>
  </section>
</template>
