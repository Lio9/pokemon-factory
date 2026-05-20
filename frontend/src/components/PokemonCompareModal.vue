<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
      >
        <div
          class="absolute inset-0 bg-black/50 backdrop-blur-sm"
          @click="$emit('close')"
        />
        <div class="relative bg-white rounded-3xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-y-auto">
          <button
            class="absolute top-4 right-4 w-10 h-10 rounded-full bg-gray-100 hover:bg-gray-200 flex items-center justify-center transition-colors"
            @click="$emit('close')"
          >
            <X class="w-5 h-5 text-gray-600" />
          </button>
          <div class="p-6">
            <h2 class="text-2xl font-bold text-gray-900 mb-6">
              宝可梦比较
            </h2>
            <CompareView
              :pokemon1="pokemon1"
              :pokemon2="pokemon2"
              @select-compare="$emit('select-compare')"
            />
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script>
import { X } from 'lucide-vue-next'
import { sprites } from '../services/api.js'

// 比较视图组件
const CompareView = {
  props: ['pokemon1', 'pokemon2'],
  emits: ['select-compare'],
  template: `
    <div class="grid md:grid-cols-2 gap-8">
      <div class="text-center">
        <img :src="pokemon1.spriteUrl || getSprite(pokemon1.id)" class="w-32 h-32 mx-auto mb-4">
        <h3 class="font-bold text-lg">{{ pokemon1.name }}</h3>
      </div>
      <div v-if="pokemon2" class="text-center">
        <img :src="pokemon2.spriteUrl || getSprite(pokemon2.id)" class="w-32 h-32 mx-auto mb-4">
        <h3 class="font-bold text-lg">{{ pokemon2.name }}</h3>
      </div>
      <div v-else class="flex items-center justify-center border-2 border-dashed border-gray-200 rounded-2xl">
        <button
          @click="$emit('select-compare')"
          class="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-xl font-medium transition-colors"
        >
          选择宝可梦
        </button>
      </div>
    </div>
  `,
  methods: {
    getSprite(id) {
      return sprites.pokemon(id)
    }
  }
}

export default {
  name: 'PokemonCompareModal',
  components: { X, CompareView },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    pokemon1: {
      type: Object,
      default: null
    },
    pokemon2: {
      type: Object,
      default: null
    }
  },
  emits: ['close', 'select-compare']
}
</script>

<style scoped>
/* 模态框动画 */
.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .relative,
.modal-leave-to .relative {
  transform: scale(0.95);
}
</style>
