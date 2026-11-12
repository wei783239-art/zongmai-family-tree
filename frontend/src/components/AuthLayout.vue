<template>
  <div class="authlayout">
    <section class="auth-visual">
      <span class="visual-orbit orbit-one" aria-hidden="true"></span>
      <span class="visual-orbit orbit-two" aria-hidden="true"></span>
      <div class="visual-inner">
        <img class="visual-seal" :src="sealUrl" alt="宗脉印章" />
        <p class="visual-eyebrow">ZONGMAI · FAMILY ARCHIVE</p>
        <h1 class="visual-title">宗脉</h1>
        <p class="visual-sub">一脉相承 · 慎终追远</p>
        <p class="visual-note">
          修谱续牒，敬宗收族。<br />让每一段家族记忆，都有迹可循。
        </p>
      </div>
      <svg class="lineage-art" viewBox="0 0 620 300" fill="none" aria-hidden="true">
        <path
          d="M310 26 V86 M310 86 H160 V152 M310 86 H460 V152 M160 152 H84 V226 M160 152 H236 V226 M460 152 H384 V226 M460 152 H536 V226"
          stroke="rgba(216,189,139,0.26)"
          stroke-width="1.2"
        />
        <circle cx="310" cy="26" r="5" fill="rgba(216,189,139,0.55)" />
        <circle cx="160" cy="152" r="4.5" fill="rgba(216,189,139,0.42)" />
        <circle cx="460" cy="152" r="4.5" fill="rgba(216,189,139,0.42)" />
        <circle cx="84" cy="226" r="4" fill="rgba(216,189,139,0.32)" />
        <circle cx="236" cy="226" r="4" fill="rgba(168,71,59,0.5)" />
        <circle cx="384" cy="226" r="4" fill="rgba(216,189,139,0.32)" />
        <circle cx="536" cy="226" r="4" fill="rgba(216,189,139,0.32)" />
      </svg>
      <p class="visual-footer">宗脉 · 电子族谱管理系统</p>
    </section>
    <section class="auth-panel">
      <span class="panel-halo" aria-hidden="true"></span>
      <div class="auth-card">
        <router-view />
      </div>
      <p class="auth-copyright">以谱为脉 · 以字传家</p>
    </section>
  </div>
</template>
<script setup>
const sealUrl = new URL('@/assets/brand-seal.svg', import.meta.url).href
</script>
<style scoped lang="scss">
.authlayout {
  display: flex;
  width: 100%;
  min-height: 100vh;
  background: var(--paper);
}
.auth-visual {
  position: relative;
  width: 52%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px 7vw;
  color: #f3ead9;
  background:
    radial-gradient(circle at 18% 12%, rgba(216, 189, 139, 0.18), transparent 26rem),
    radial-gradient(circle at 88% 86%, rgba(168, 71, 59, 0.18), transparent 26rem),
    linear-gradient(150deg, #243a36 0%, #182927 52%, #12201e 100%);
}
.auth-visual::before {
  content: "";
  position: absolute;
  inset: 0;
  opacity: 0.4;
  background-image: repeating-linear-gradient(
    118deg,
    rgba(255, 255, 255, 0.02) 0 1px,
    transparent 1px 7px
  );
  pointer-events: none;
}
.visual-orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(216, 189, 139, 0.12);
  pointer-events: none;
}
.orbit-one {
  width: 520px;
  height: 520px;
  top: -160px;
  right: -180px;
}
.orbit-two {
  width: 300px;
  height: 300px;
  bottom: -120px;
  left: -120px;
  border-color: rgba(168, 71, 59, 0.14);
}
.visual-inner {
  position: relative;
  z-index: 2;
  max-width: 460px;
}
.visual-seal {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  box-shadow: 0 14px 32px rgba(0, 0, 0, 0.28);
  margin-bottom: 30px;
}
.visual-eyebrow {
  margin: 0 0 14px;
  font-size: 10.5px;
  letter-spacing: 0.36em;
  color: rgba(216, 189, 139, 0.7);
}
.visual-title {
  margin: 0;
  font-size: clamp(60px, 7vw, 92px);
  line-height: 1;
  letter-spacing: 0.14em;
  color: #f7eedf;
  font-weight: 600;
  text-shadow: 0 4px 30px rgba(0, 0, 0, 0.2);
}
.visual-sub {
  margin: 22px 0 0;
  font-family: var(--serif);
  font-size: 18px;
  letter-spacing: 0.28em;
  color: #e6d5b6;
}
.visual-note {
  margin: 30px 0 0;
  font-size: 13px;
  line-height: 2;
  letter-spacing: 0.06em;
  color: rgba(233, 225, 209, 0.56);
}
.lineage-art {
  position: absolute;
  right: -40px;
  bottom: 56px;
  width: min(620px, 78%);
  opacity: 0.9;
  pointer-events: none;
}
.visual-footer {
  position: absolute;
  left: 7vw;
  bottom: 34px;
  margin: 0;
  font-size: 10.5px;
  letter-spacing: 0.22em;
  color: rgba(226, 214, 190, 0.34);
}
.auth-panel {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  overflow: hidden;
  background:
    radial-gradient(circle at 80% 8%, rgba(173, 134, 80, 0.1), transparent 22rem),
    radial-gradient(circle at 10% 92%, rgba(50, 107, 96, 0.07), transparent 24rem),
    var(--paper);
}
.panel-halo {
  position: absolute;
  width: 560px;
  height: 560px;
  border-radius: 50%;
  border: 1px solid rgba(173, 134, 80, 0.14);
  right: -260px;
  top: -260px;
  pointer-events: none;
}
.auth-card {
  position: relative;
  z-index: 1;
  width: min(420px, 100%);
  padding: 42px 40px 36px;
  border-radius: 22px;
  border: 1px solid rgba(228, 220, 203, 0.9);
  background: #fffdf8;
  box-shadow:
    0 30px 70px rgba(34, 49, 44, 0.12),
    0 2px 0 rgba(255, 255, 255, 0.8) inset;
}
.auth-copyright {
  position: relative;
  z-index: 1;
  margin: 28px 0 0;
  font-family: var(--serif);
  font-size: 11.5px;
  letter-spacing: 0.28em;
  color: var(--ink-400);
}
@media (max-width: 960px) {
  .authlayout {
    flex-direction: column;
  }
  .auth-visual {
    width: 100%;
    min-height: 260px;
    padding: 40px 8vw 46px;
  }
  .visual-title {
    font-size: 52px;
  }
  .visual-note,
  .lineage-art,
  .visual-footer {
    display: none;
  }
  .visual-seal {
    width: 48px;
    height: 48px;
    margin-bottom: 18px;
  }
  .visual-sub {
    margin-top: 14px;
    font-size: 15px;
  }
  .auth-panel {
    padding: 30px 18px 40px;
  }
  .auth-card {
    padding: 32px 24px 28px;
  }
}
</style>
