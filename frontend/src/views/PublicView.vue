<template>
  <main class="public-page">
    <header class="public-nav">
      <router-link class="public-brand" to="/public">
        <img class="public-seal" :src="sealUrl" alt="宗脉印章" />
        <span class="public-brand-text">
          <strong>宗脉</strong>
          <small>电子族谱</small>
        </span>
      </router-link>
      <div class="workspace-actions">
        <el-button @click="router.push('/auth/login')">登录</el-button>
        <el-button type="primary" @click="router.push('/auth/register')"
          >注册账号</el-button
        >
      </div>
    </header>

    <section class="public-hero">
      <div class="hero-copy">
        <p class="public-eyebrow">
          <span class="eyebrow-rule"></span>
          家 族 记 忆 的 数 字 谱 牒
        </p>
        <h1>
          一脉相承<br />
          <em>让家族记忆有迹可循</em>
        </h1>
        <p class="public-lead">
          以世系为骨、以人物为肉、以大事记为魂，保存家族姓名、关系与故事，
          让后代随时可以回望来处。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="router.push('/auth/login')"
            >进入家族总览</el-button
          >
          <el-button size="large" @click="router.push('/auth/register')"
            >创建修谱账号</el-button
          >
        </div>
      </div>
      <div class="hero-art" aria-hidden="true">
        <span class="art-orbit orbit-a"></span>
        <span class="art-orbit orbit-b"></span>
        <span class="art-seal">宗</span>
        <svg class="art-lines" viewBox="0 0 420 260" fill="none">
          <path
            d="M210 24 V78 M210 78 H120 V140 M210 78 H300 V140 M120 140 H56 V212 M120 140 H184 V212 M300 140 H236 V212 M300 140 H364 V212"
            stroke="rgba(173,134,80,0.34)"
            stroke-width="1.2"
          />
          <circle cx="210" cy="24" r="5" fill="#a8473b" />
          <circle cx="120" cy="140" r="4" fill="rgba(173,134,80,0.7)" />
          <circle cx="300" cy="140" r="4" fill="rgba(173,134,80,0.7)" />
          <circle cx="56" cy="212" r="3.5" fill="rgba(173,134,80,0.5)" />
          <circle cx="184" cy="212" r="3.5" fill="rgba(173,134,80,0.5)" />
          <circle cx="236" cy="212" r="3.5" fill="rgba(173,134,80,0.5)" />
          <circle cx="364" cy="212" r="3.5" fill="rgba(173,134,80,0.5)" />
        </svg>
      </div>
    </section>

    <section class="public-families">
      <div class="public-section-head">
        <div>
          <p class="public-eyebrow">公 开 谱 牒</p>
          <h2>家族简介</h2>
        </div>
        <p class="section-note">由家族管理员授权公开，具体人物档案仍需家族授权访问。</p>
      </div>

      <el-skeleton v-if="loading" :rows="4" animated />
      <el-alert
        v-else-if="failed"
        title="公开简介加载失败，请刷新重试"
        type="error"
        :closable="false"
      />
      <el-empty v-else-if="!families.length" description="暂无公开的家族简介" />
      <div v-else class="public-family-grid">
        <article
          v-for="family in families"
          :key="family.id"
          class="public-family-card"
        >
          <span class="card-seal" aria-hidden="true">{{ family.surname || "宗" }}</span>
          <p class="card-eyebrow">世家简介</p>
          <h2>{{ family.name }}</h2>
          <p v-if="family.surname" class="card-surname">姓氏 · {{ family.surname }}</p>
          <p class="biography">{{ family.description || "暂无简介" }}</p>
        </article>
      </div>
    </section>

    <footer class="public-footer">
      <span>宗脉 · 电子族谱管理系统</span>
      <span>一脉相承 · 敬宗收族</span>
    </footer>
  </main>
</template>
<script setup>
import { onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import service from "@/utils/request";
const router = useRouter();
const sealUrl = new URL("@/assets/brand-seal.svg", import.meta.url).href;
const families = ref([]),
  loading = ref(true),
  failed = ref(false);
onMounted(async () => {
  try {
    families.value = await service.get("/public/families");
  } catch {
    failed.value = true;
  } finally {
    loading.value = false;
  }
});
</script>
<style scoped>
.public-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 26px 32px 46px;
}
.public-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 16px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--line);
}
.public-brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  color: var(--ink-900);
}
.public-seal {
  width: 42px;
  height: 42px;
  border-radius: 11px;
  box-shadow: 0 8px 20px rgba(168, 71, 59, 0.2);
}
.public-brand-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.public-brand-text strong {
  font-family: var(--serif);
  font-size: 22px;
  letter-spacing: 0.2em;
}
.public-brand-text small {
  font-size: 10.5px;
  letter-spacing: 0.18em;
  color: var(--ink-400);
}
.public-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 0.9fr);
  gap: 50px;
  align-items: center;
  padding: 84px 0 76px;
}
.public-eyebrow {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  font-size: 10.5px;
  font-weight: 600;
  letter-spacing: 0.3em;
  color: var(--bronze);
}
.eyebrow-rule {
  width: 26px;
  height: 1px;
  background: currentColor;
  opacity: 0.7;
}
.hero-copy h1 {
  margin: 0;
  font-size: clamp(38px, 4.6vw, 60px);
  line-height: 1.24;
  letter-spacing: 0.08em;
  color: var(--ink-900);
}
.hero-copy h1 em {
  font-style: normal;
  color: var(--jade-700);
}
.public-lead {
  max-width: 48ch;
  margin: 24px 0 0;
  font-size: 14px;
  line-height: 2;
  color: var(--ink-500);
}
.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 32px;
}
.hero-art {
  position: relative;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.art-orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(173, 134, 80, 0.18);
}
.orbit-a {
  width: 340px;
  height: 340px;
}
.orbit-b {
  width: 250px;
  height: 250px;
  border-color: rgba(50, 107, 96, 0.16);
}
.art-seal {
  position: relative;
  z-index: 1;
  font-family: var(--serif);
  font-size: 150px;
  line-height: 1;
  color: rgba(168, 71, 59, 0.14);
  user-select: none;
}
.art-lines {
  position: absolute;
  width: 100%;
  bottom: -6px;
  opacity: 0.9;
}
.public-families {
  padding-top: 26px;
  border-top: 1px solid var(--line);
}
.public-section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 24px;
}
.public-section-head h2 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 0.08em;
}
.section-note {
  max-width: 40ch;
  margin: 0;
  font-size: 12px;
  color: var(--ink-400);
}
.public-family-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}
.public-family-card {
  position: relative;
  padding: 26px 26px 24px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: rgba(255, 253, 248, 0.9);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
  transition:
    transform 0.22s ease,
    box-shadow 0.22s ease;
}
.public-family-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow-md);
}
.card-seal {
  position: absolute;
  right: 18px;
  top: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border: 1px solid rgba(168, 71, 59, 0.4);
  border-radius: 6px;
  font-family: var(--serif);
  font-size: 20px;
  color: rgba(168, 71, 59, 0.72);
  transform: rotate(-4deg);
}
.card-eyebrow {
  margin: 0 0 8px;
  font-size: 10px;
  letter-spacing: 0.24em;
  color: var(--bronze);
}
.public-family-card h2 {
  margin: 0;
  font-size: 23px;
  letter-spacing: 0.06em;
}
.card-surname {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--ink-400);
}
.public-family-card .biography {
  margin: 16px 0 0;
  font-size: 13px;
  color: var(--ink-500);
}
.public-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 10px;
  margin-top: 64px;
  padding-top: 22px;
  border-top: 1px solid var(--line);
  font-size: 11px;
  letter-spacing: 0.16em;
  color: var(--ink-400);
}
@media (max-width: 900px) {
  .public-page {
    padding: 20px 18px 36px;
  }
  .public-hero {
    grid-template-columns: 1fr;
    gap: 20px;
    padding: 48px 0 40px;
  }
  .hero-art {
    min-height: 220px;
  }
}
</style>
