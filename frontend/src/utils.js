export const money = (value, currency = "RWF") =>
  new Intl.NumberFormat("en-RW", {
    style: "currency",
    currency,
    maximumFractionDigits: 0,
  }).format(Number(value || 0));

export const date = (value) => value
  ? new Intl.DateTimeFormat("en-GB", { day: "2-digit", month: "short", year: "numeric" }).format(new Date(value))
  : "—";

export const dateTime = (value) => value
  ? new Intl.DateTimeFormat("en-GB", {
      day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit",
    }).format(new Date(value))
  : "—";

export const today = () => new Date().toISOString().slice(0, 10);
export const thisMonth = () => new Date().toISOString().slice(0, 7);

export function downloadCsv(filename, rows) {
  if (!rows.length) return;
  const headers = Object.keys(rows[0]);
  const escape = (value) => `"${String(value ?? "").replaceAll('"', '""')}"`;
  const csv = [headers.map(escape).join(","), ...rows.map((row) => headers.map((key) => escape(row[key])).join(","))].join("\n");
  const blob = new Blob(["\ufeff", csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

export function routeTo(page) {
  window.location.hash = page;
}

export function currentRoute() {
  return window.location.hash.replace(/^#\/?/, "") || "dashboard";
}

