import { createPortal } from "react-dom";
import { createContext, useContext, useEffect, useId, useRef, useState } from "react";
import { AlertCircle, Check, ChevronLeft, ChevronRight, Loader2, Search, X } from "lucide-react";
import { money } from "../utils";

export function Button({ children, variant = "primary", size = "md", className = "", loading, ...props }) {
  const variants = {
    primary: "bg-teal-700 text-white hover:bg-teal-800 shadow-sm",
    secondary: "bg-white text-slate-700 border border-slate-200 hover:bg-slate-50",
    danger: "bg-rose-600 text-white hover:bg-rose-700",
    ghost: "bg-transparent text-slate-600 hover:bg-slate-100",
    success: "bg-emerald-600 text-white hover:bg-emerald-700",
  };
  const sizes = { sm: "h-8 px-3 text-xs", md: "h-10 px-4 text-sm", lg: "h-12 px-5 text-sm" };
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-semibold transition focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600 disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${sizes[size]} ${className}`}
      disabled={loading || props.disabled}
      {...props}
    >
      {loading && <Loader2 size={16} className="animate-spin" />}{children}
    </button>
  );
}

export function Card({ children, className = "", padding = true }) {
  return <div className={`surface-shadow rounded-xl border border-slate-200/80 bg-white ${padding ? "p-5" : ""} ${className}`}>{children}</div>;
}

export function PageHeader({ title, description, actions }) {
  return (
    <div className="mb-7 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-slate-900 sm:text-[28px]">{title}</h1>
        {description && <p className="mt-1 text-sm text-slate-500">{description}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-2">{actions}</div>}
    </div>
  );
}

export function StatCard({ label, value, icon: Icon, tone = "teal", note }) {
  const tones = {
    teal: "bg-teal-50 text-teal-700",
    blue: "bg-blue-50 text-blue-700",
    amber: "bg-amber-50 text-amber-700",
    rose: "bg-rose-50 text-rose-700",
    violet: "bg-violet-50 text-violet-700",
    emerald: "bg-emerald-50 text-emerald-700",
  };
  return (
    <Card className="stat-card relative min-w-0 overflow-hidden">
      <div className="mb-5 flex items-center justify-between gap-3">
        <p className="text-xs font-medium text-slate-500">{label}</p>
        {Icon && <span className={`grid size-9 shrink-0 place-items-center rounded-lg ${tones[tone]}`}><Icon size={18} /></span>}
      </div>
      <p className="break-words text-2xl font-semibold tracking-tight text-slate-900" style={{ fontVariantNumeric: "tabular-nums" }}>{value}</p>
      <p className="mt-3 border-t border-slate-100 pt-3 text-xs text-slate-500">{note || "Current total"}</p>
    </Card>
  );
}
export function Field({ label, error, required, children, hint, className = "" }) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-xs font-semibold text-slate-700">{label}{required && <span className="ml-1 text-rose-500">*</span>}</span>
      {children}
      {hint && !error && <span className="mt-1 block text-xs text-slate-400">{hint}</span>}
      {error && <span className="mt-1 block text-xs text-rose-600">{error}</span>}
    </label>
  );
}

export const inputClass = "h-10 w-full rounded-xl border border-slate-200 bg-white px-3 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/10 disabled:bg-slate-50";
export const textareaClass = "min-h-24 w-full resize-y rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm text-slate-800 outline-none transition placeholder:text-slate-400 focus:border-teal-500 focus:ring-4 focus:ring-teal-500/10";

const statusColors = {
  ACTIVE: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  APPROVED: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  COMPLETED: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  PAID: "bg-emerald-50 text-emerald-700 ring-emerald-600/20",
  PENDING_APPROVAL: "bg-amber-50 text-amber-700 ring-amber-600/20",
  PENDING: "bg-amber-50 text-amber-700 ring-amber-600/20",
  PARTIAL: "bg-blue-50 text-blue-700 ring-blue-600/20",
  DRAFT: "bg-slate-100 text-slate-600 ring-slate-500/20",
  DISABLED: "bg-slate-100 text-slate-600 ring-slate-500/20",
  LEFT: "bg-violet-50 text-violet-700 ring-violet-600/20",
  WATCHLIST: "bg-rose-50 text-rose-700 ring-rose-600/20",
  REJECTED: "bg-rose-50 text-rose-700 ring-rose-600/20",
  OVERDUE: "bg-rose-50 text-rose-700 ring-rose-600/20",
};

export function StatusBadge({ value }) {
  if (!value) return <span className="text-slate-400">—</span>;
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-[11px] font-bold ring-1 ring-inset ${statusColors[value] || "bg-blue-50 text-blue-700 ring-blue-600/20"}`}>{String(value).replaceAll("_", " ")}</span>;
}

export function SearchBox({ value, onChange, placeholder = "Search…" }) {
  return (
    <div className="relative w-full sm:w-72">
      <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
      <input className={`${inputClass} pl-9`} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
    </div>
  );
}

export function DataTable({ columns, rows = [], keyField = "id", empty = "No records found", pageSize = 10 }) {
  const [page, setPage] = useState(1);
  const pages = Math.max(1, Math.ceil(rows.length / pageSize));
  useEffect(() => { if (page > pages) setPage(pages); }, [page, pages]);
  const visible = rows.slice((page - 1) * pageSize, page * pageSize);
  return (
    <div className="data-table surface-shadow overflow-hidden rounded-xl border border-slate-200 bg-white">
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200">
          <thead className="bg-slate-50">
            <tr>{columns.map((column) => <th key={column.key} className={`whitespace-nowrap px-4 py-3 text-left text-[11px] font-bold uppercase tracking-wider text-slate-500 ${column.headerClass || ""}`}>{column.label}</th>)}</tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {visible.map((row, index) => (
            <tr key={(typeof keyField === "function" ? keyField(row) : row[keyField]) ?? index} className="transition even:bg-slate-50/30 hover:bg-teal-50/40">
                {columns.map((column) => <td key={column.key} className={`whitespace-nowrap px-4 py-3 text-sm text-slate-700 ${column.className || ""}`}>{column.render ? column.render(row) : row[column.key] ?? "—"}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {!rows.length && <div className="grid min-h-44 place-items-center px-6 text-center text-sm text-slate-400">{empty}</div>}
      {rows.length > pageSize && (
        <div className="flex items-center justify-between border-t border-slate-100 px-4 py-3 text-xs text-slate-500">
          <span>{(page - 1) * pageSize + 1}–{Math.min(page * pageSize, rows.length)} of {rows.length}</span>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="sm" disabled={page === 1} onClick={() => setPage((value) => value - 1)}><ChevronLeft size={15} /></Button>
            <span>Page {page} of {pages}</span>
            <Button variant="ghost" size="sm" disabled={page === pages} onClick={() => setPage((value) => value + 1)}><ChevronRight size={15} /></Button>
          </div>
        </div>
      )}
    </div>
  );
}

export function Modal({ open, onClose, title, description, children, size = "lg" }) {
  const dialogRef = useRef(null);
  const titleId = useId();
  const descriptionId = useId();
  const widths = { sm: "max-w-md", md: "max-w-xl", lg: "max-w-2xl", xl: "max-w-4xl" };

  useEffect(() => {
    if (!open) return;
    const dialog = dialogRef.current;
    const previousFocus = document.activeElement;
    const previousOverflow = document.body.style.overflow;
    dialog.showModal();
    document.body.style.overflow = "hidden";
    return () => {
      dialog.close();
      document.body.style.overflow = previousOverflow;
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus();
    };
  }, [open]);

  if (!open) return null;
  return createPortal(
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      aria-modal="true"
      className={`app-modal w-[calc(100%-2rem)] border-0 bg-white p-0 text-slate-900 ${widths[size] || widths.lg}`}
      onCancel={(event) => { event.preventDefault(); onClose(); }}
      onClick={(event) => {
        if (event.target !== event.currentTarget) return;
        const bounds = event.currentTarget.getBoundingClientRect();
        if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) onClose();
      }}
    >
      <div className="modal-shell">
        <div className="modal-heading flex shrink-0 items-start justify-between gap-4 px-5 py-5 sm:px-7 sm:py-6">
          <div className="min-w-0">
            <span aria-hidden="true" className="mb-3 block h-1 w-9 rounded-full bg-teal-600" />
            <h2 id={titleId} className="text-xl font-bold tracking-tight text-slate-900">{title}</h2>
            {description && <p id={descriptionId} className="mt-2 max-w-xl text-sm leading-6 text-slate-500">{description}</p>}
          </div>
          <button type="button" autoFocus onClick={onClose} aria-label="Close dialog" className="mt-1 inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl border border-slate-200 bg-white px-3 text-sm font-semibold text-slate-600 shadow-sm transition hover:border-teal-300 hover:bg-teal-50 hover:text-teal-800 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600">
            <X size={17} aria-hidden="true" /><span>Close</span>
          </button>
        </div>
        <div className="modal-content min-h-0 overflow-y-auto overscroll-contain px-5 py-6 sm:px-7">{children}</div>
      </div>
    </dialog>,
    document.body,
  );
}
export function Tabs({ items, value, onChange }) {
  return <div className="mb-5 flex gap-1 overflow-x-auto rounded-xl bg-slate-100 p-1">{items.map((item) => <button key={item.value} onClick={() => onChange(item.value)} className={`whitespace-nowrap rounded-lg px-4 py-2 text-xs font-semibold transition ${value === item.value ? "bg-white text-teal-800 shadow-sm" : "text-slate-500 hover:text-slate-800"}`}>{item.label}</button>)}</div>;
}

export function Loading({ label = "Loading…" }) {
  return <div className="grid min-h-56 place-items-center"><div className="flex items-center gap-2 text-sm text-slate-500"><Loader2 size={18} className="animate-spin text-teal-700" />{label}</div></div>;
}

export function ErrorState({ message, onRetry }) {
  return <Card className="flex items-center justify-between gap-4 border-rose-200 bg-rose-50"><div className="flex items-center gap-3 text-sm text-rose-700"><AlertCircle size={18} />{message}</div>{onRetry && <Button variant="secondary" size="sm" onClick={onRetry}>Try again</Button>}</Card>;
}

const NoticeContext = createContext(() => {});

export function NoticeProvider({ children }) {
  const [notice, setNotice] = useState(null);
  const notify = (message, type = "success") => {
    setNotice({ message, type, id: Date.now() });
    setTimeout(() => setNotice(null), 3200);
  };
  return (
    <NoticeContext.Provider value={notify}>
      {children}
      {notice && <div className={`fixed bottom-5 right-5 z-[70] flex max-w-sm items-center gap-3 rounded-2xl px-4 py-3 text-sm font-medium text-white shadow-xl ${notice.type === "error" ? "bg-rose-600" : "bg-slate-900"}`}><span className={`grid size-6 place-items-center rounded-full ${notice.type === "error" ? "bg-white/20" : "bg-teal-500"}`}>{notice.type === "error" ? <AlertCircle size={14} /> : <Check size={14} />}</span>{notice.message}</div>}
    </NoticeContext.Provider>
  );
}

export const useNotice = () => useContext(NoticeContext);
export const MoneyCell = ({ value }) => <span className="font-semibold tabular-nums text-slate-800">{money(value)}</span>;
