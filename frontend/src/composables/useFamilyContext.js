import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { listFamilies } from "@/api/family";

const families = ref([]);
const loadingFamilies = ref(false);
let pending = null;

export async function refreshFamilies() {
  if (pending) return pending;
  loadingFamilies.value = true;
  pending = listFamilies()
    .then((data) => {
      families.value = data;
    })
    .finally(() => {
      pending = null;
      loadingFamilies.value = false;
    });
  return pending;
}

export function useFamilyContext() {
  const route = useRoute();
  const router = useRouter();
  const initialPath = route.path;
  const familyId = computed(() => {
    const id = route.query.familyId;
    return typeof id === "string" && /^[1-9]\d*$/.test(id) ? id : "";
  });
  const family = computed(() =>
    families.value.find((item) => String(item.id) === familyId.value),
  );
  const canManage = computed(() => family.value?.role === "FAMILY_ADMIN");
  const selectFamily = (id) =>
    router.replace({ query: { ...route.query, familyId: String(id) } });

  onMounted(async () => {
    try {
      await refreshFamilies();
      if (
        route.path === initialPath &&
        !familyId.value &&
        families.value.length
      ) {
        await selectFamily(families.value[0].id);
      }
    } catch {
      // 请求层显示具体错误，页面保留家族选择和刷新入口。
    }
  });
  return {
    families,
    loadingFamilies,
    familyId,
    family,
    canManage,
    selectFamily,
    refreshFamilies,
  };
}
