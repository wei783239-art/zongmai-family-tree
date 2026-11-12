import service from "@/utils/request";

export const familyRequest = (familyId, path, config = {}) =>
  service({
    url: `/families/${familyId}${path}`,
    ...config,
  });

export async function downloadFamily(familyId, format) {
  const blob = await familyRequest(familyId, "/export", {
    params: { format },
    responseType: "blob",
    timeout: 60000,
  });
  saveBlob(blob, `family-${familyId}.${format}`);
}

export function saveBlob(blob, filename) {
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

export const createDemoFamily = () =>
  service.post("/demo-families", {}, { timeout: 30000 });
export const listSystemUsers = () => service.get("/system/users");
export const changeUserStatus = (id, status) =>
  service.patch(`/system/users/${id}`, { status });
