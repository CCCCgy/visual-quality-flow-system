<template>
  <router-view />
</template>

<script setup>
/**
 * 页面或模块职责：
 * Vue 根组件，仅承载顶层 router-view。
 *
 * 路由入口：
 * main.js 挂载 App 后，router/index.js 根据当前 URL 渲染 MainLayout 及其子页面。
 *
 * 调用的前端 API：
 * 无。具体 API 调用由各 views/*.vue 页面完成。
 *
 * 对应后端接口：
 * 无直接对应接口。
 *
 * 主要交互流程：
 * 浏览器 URL -> Vue Router -> 当前页面组件 -> api/*.js -> 后端 Controller。
 */
</script>
