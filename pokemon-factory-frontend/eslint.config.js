const vue = require('eslint-plugin-vue')

module.exports = [
  {
    ignores: ['dist/**', 'node_modules/**']
  },
  ...vue.configs['flat/recommended'],
  {
    rules: {
      'vue/multi-word-component-names': 'off',
      'vue/require-default-prop': 'off',
      'vue/no-unused-components': 'off',
      'vue/no-reserved-component-names': 'off',
      'vue/no-unused-vars': 'off'
    }
  }
]
