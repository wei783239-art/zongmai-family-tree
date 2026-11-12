<template>
  <div ref="container" class="tree-canvas" aria-label="家族世系图"></div>
</template>
<script setup>
import { onMounted, onBeforeUnmount, ref } from "vue";
import { Graph } from "@antv/g6";
import { buildTreeData } from "@/utils/treeLayout";
const props = defineProps({
  persons: { type: Array, required: true },
  relations: { type: Array, required: true },
});
const emit = defineEmits(["select", "ready", "error"]);
const container = ref(null);
let graph,
  rendering = false,
  disposed = false;
onMounted(async () => {
  rendering = true;
  try {
    graph = new Graph({
      container: container.value,
      autoResize: true,
      animation: false,
      padding: 40,
      data: buildTreeData(props.persons, props.relations),
      zoomRange: [0.03, 4],
      node: {
        type: "rect",
        style: {
          size: [172, 84],
          radius: 10,
          fill: "#fffdf8",
          stroke: "#b6a37e",
          lineWidth: 1.3,
          shadowColor: "rgba(50, 63, 58, 0.12)",
          shadowBlur: 8,
          shadowOffsetY: 3,
          labelText: (node) =>
            `${node.data.name}\n${node.data.generation ? `第 ${node.data.generation} 代` : "世代未填写"} · #${node.id}`,
          labelPlacement: "center",
          labelOffsetX: (node) => node.data.avatar_url ? 20 : 0,
          labelFill: "#233634",
          labelFontFamily: "Songti SC, Microsoft YaHei, serif",
          labelFontSize: 14,
          labelFontWeight: 500,
          labelLineHeight: 24,
          labelWordWrap: true,
          labelMaxWidth: (node) => node.data.avatar_url ? 104 : 150,
          labelMaxLines: 3,
          icon: (node) => Boolean(node.data.avatar_url),
          iconSrc: (node) => node.data.avatar_url || undefined,
          iconWidth: 30,
          iconHeight: 30,
          iconX: -68,
          iconY: -15,
          cursor: "pointer",
        },
        state: {
          selected: {
            fill: "#f8e9e2",
            stroke: "#a8473b",
            lineWidth: 2.4,
            shadowColor: "rgba(168, 71, 59, 0.22)",
            shadowBlur: 14,
          },
        },
      },
      behaviors: ["drag-canvas", "zoom-canvas", "drag-element", "click-select"],
    });
    graph.on("node:click", (event) => emit("select", String(event.target.id)));
    await graph.render();
    if (!disposed) await graph.fitView();
    if (!disposed) emit("ready");
  } catch (error) {
    if (!disposed) emit("error", error);
  } finally {
    rendering = false;
    if (disposed) graph?.destroy();
  }
});
onBeforeUnmount(() => {
  disposed = true;
  if (!rendering) graph?.destroy();
});
defineExpose({
  fit: () => graph?.fitView(),
  focus: async (id) => {
    if (!graph || disposed) return;
    await graph.zoomTo(Math.max(graph.getZoom(), 0.8), false);
    await graph.focusElement(String(id), false);
  },
  zoom: (ratio) => graph?.zoomBy(ratio, false),
  image: () =>
    graph?.toDataURL({
      mode: "overall",
      type: "image/png",
      backgroundColor: "#f8f4ea",
    }),
});
</script>
<style scoped>
.tree-canvas {
  width: 100%;
  height: min(68vh, 720px);
  min-height: 420px;
  border: 1px solid var(--line, #e4dccb);
  background-color: #fbf8f1;
  background-image:
    radial-gradient(circle at 50% 0%, rgba(173, 134, 80, 0.05), transparent 24rem),
    linear-gradient(rgba(120, 105, 76, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(120, 105, 76, 0.045) 1px, transparent 1px);
  background-size: auto, 28px 28px, 28px 28px;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: inset 0 0 50px rgba(120, 100, 60, 0.05);
}
</style>
