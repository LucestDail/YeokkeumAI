<script setup lang="ts">
import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { streamChat } from '../api'

const { t } = useI18n()
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
    (tok) => { output.value += tok },
    () => { busy.value = false },
    (e) => { error.value = e; busy.value = false }
  )
}
</script>

<template>
  <section class="card" aria-labelledby="chat-h">
    <h2 id="chat-h">{{ t('chat.h') }}</h2>
    <p class="desc">{{ t('chat.desc') }}</p>

    <div v-if="error" class="app-alert danger" role="alert">{{ error }}</div>

    <div class="fieldset">
      <div class="form-group">
        <div class="form-tit"><label for="chat-msg">{{ t('chat.q') }}</label></div>
        <div class="form-conts">
          <textarea id="chat-msg" class="krds-input" v-model="message" rows="4"
            :placeholder="t('chat.qph')" @keydown.ctrl.enter="send"></textarea>
        </div>
      </div>
    </div>

    <button type="button" class="krds-btn primary" :disabled="busy" @click="send">
      {{ busy ? t('chat.sending') : t('chat.send') }}
    </button>

    <div class="fieldset mt16">
      <div class="form-group">
        <div class="form-tit"><label for="chat-out">{{ t('chat.answer') }}</label></div>
        <div class="output" id="chat-out" aria-live="off">{{ output || t('common.none') }}</div>
        <p class="sr-only" role="status" aria-live="polite">{{ busy ? t('chat.genBusy') : (output ? t('chat.genDone') : '') }}</p>
      </div>
    </div>
  </section>
</template>
