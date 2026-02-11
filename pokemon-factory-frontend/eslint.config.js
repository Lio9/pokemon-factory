const vue = require('eslint-plugin-vue')

module.exports = [
  ...vue.configs['flat/recommended'],
  {
    rules: {
      'vue/no-unused-components': 'off',
      'vue/no-reserved-component-names': 'off',
      'vue/no-unused-vars': 'off'
    }
  }
]