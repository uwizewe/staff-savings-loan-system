import { useId, useMemo, useState } from 'react';
import { ArrowDown, ArrowUp, ArrowUpDown, ChevronLeft, ChevronRight, Loader2, Search, Table2 } from 'lucide-react';

const control = 'min-h-10 rounded-lg border border-slate-200 bg-white px-3 text-sm focus-visible:outline-2 focus-visible:outline-teal-600 disabled:opacity-40';
const isDataColumn = column => column.sortable !== false && !['actions', 'review', 'include', 'payment'].includes(column.key);

export default function DataTable({ columns, rows = [], keyField = 'id', empty = 'No records found', pageSize = 10, loading = false, searchPlaceholder = 'Search records…', label = 'Records', searchable = true }) {
  const id = useId();
  const [query, setQuery] = useState('');
  const [size, setSize] = useState(pageSize);
  const [page, setPage] = useState(1);
  const [sort, setSort] = useState({ key: '', direction: 'asc' });
  const data = useMemo(() => {
    const needle = query.trim().toLocaleLowerCase();
    const result = (rows || []).filter(row => !needle || columns.some(column => isDataColumn(column) && String(column.searchValue ? column.searchValue(row) : row[column.key] ?? '').toLocaleLowerCase().includes(needle)));
    const column = columns.find(item => item.key === sort.key);
    if (column) result.sort((a, b) => {
      const left = column.sortValue ? column.sortValue(a) : a[column.key];
      const right = column.sortValue ? column.sortValue(b) : b[column.key];
      const comparison = left == null ? (right == null ? 0 : 1) : right == null ? -1 : typeof left === 'number' && typeof right === 'number' ? left - right : String(left).localeCompare(String(right), undefined, { numeric: true, sensitivity: 'base' });
      return sort.direction === 'asc' ? comparison : -comparison;
    });
    return result;
  }, [rows, columns, query, sort]);
  const pages = Math.max(1, Math.ceil(data.length / size));
  const current = Math.min(page, pages);
  const start = (current - 1) * size;
  const sizes = [...new Set([10, 25, 50, 100, pageSize])].sort((a, b) => a - b);
  const changeSort = key => { setSort(previous => ({ key, direction: previous.key === key && previous.direction === 'asc' ? 'desc' : 'asc' })); setPage(1); };
  return <section aria-label={label} aria-busy={loading} className="data-table min-w-0 max-w-full overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm">
    <div className="flex flex-col gap-3 border-b border-slate-100 p-4 sm:flex-row sm:items-center sm:justify-between">
      {searchable && <label className="relative block w-full sm:max-w-xs"><span className="sr-only">Search {label}</span><Search aria-hidden="true" size={17} className="absolute left-3 top-3 text-slate-400"/><input type="search" className={`${control} w-full pl-10`} placeholder={searchPlaceholder} value={query} onChange={event => { setQuery(event.target.value); setPage(1); }}/></label>}
      <label className="flex items-center gap-2 text-xs text-slate-500">Rows per page<select className={control} value={size} onChange={event => { setSize(Number(event.target.value)); setPage(1); }}>{sizes.map(value => <option key={value} value={value}>{value}</option>)}</select></label>
    </div>
    <p id={`${id}-hint`} className="px-4 pt-3 text-xs text-slate-400 sm:hidden">Swipe horizontally to see all columns and actions.</p>
    <div className="max-w-full overflow-x-auto overscroll-x-contain" role="region" aria-label={`${label} table`} aria-describedby={`${id}-hint`} tabIndex={0}>
      <table className="w-full min-w-full text-left">
        <thead className="border-b border-slate-200 bg-slate-50"><tr>{columns.map(column => <th key={column.key} scope="col" aria-sort={sort.key === column.key ? sort.direction === 'asc' ? 'ascending' : 'descending' : undefined} className={`whitespace-nowrap px-4 py-2 text-xs font-semibold text-slate-600 ${column.headerClass || ''}`}>
          {isDataColumn(column) ? <button type="button" className="flex min-h-10 items-center gap-2 rounded focus-visible:outline-2 focus-visible:outline-teal-600" onClick={() => changeSort(column.key)}>{column.label}{sort.key === column.key ? sort.direction === 'asc' ? <ArrowUp size={14}/> : <ArrowDown size={14}/> : <ArrowUpDown size={14} className="text-slate-400"/>}</button> : column.label}
        </th>)}</tr></thead>
        <tbody className="divide-y divide-slate-100">{!loading && data.slice(start, start + size).map((row, index) => <tr key={(typeof keyField === 'function' ? keyField(row) : row[keyField]) ?? index} className="transition even:bg-slate-50/40 hover:bg-teal-50/40">{columns.map(column => <td key={column.key} className={`whitespace-nowrap px-4 py-4 text-sm text-slate-700 ${column.className || ''}`}>{column.render ? column.render(row) : row[column.key] ?? '—'}</td>)}</tr>)}</tbody>
      </table>
    </div>
    {loading ? <div role="status" className="flex min-h-48 items-center justify-center gap-2 text-sm text-slate-500"><Loader2 size={20} className="animate-spin text-teal-600"/>Loading records…</div> : !data.length && <div role="status" className="flex min-h-48 flex-col items-center justify-center gap-3 px-5 text-center"><Table2 size={28} className="text-slate-300"/><p className="font-medium text-slate-600">{query ? 'No matching records' : empty}</p><p className="text-sm text-slate-400">{query ? 'Try another search or clear your filters.' : 'Records will appear here when available.'}</p>{query && <button type="button" className={`${control} text-teal-700`} onClick={() => { setQuery(''); setPage(1); }}>Clear search</button>}</div>}
    <div className="flex flex-col gap-3 border-t border-slate-100 px-4 py-3 text-xs text-slate-500 sm:flex-row sm:items-center sm:justify-between">
      <span aria-live="polite">{loading ? 'Loading…' : `${data.length ? start + 1 : 0}–${Math.min(start + size, data.length)} of ${data.length} records`}</span>
      <div className="flex items-center justify-between gap-3"><button type="button" aria-label={`Previous page of ${label}`} className={control} disabled={loading || current === 1} onClick={() => setPage(current - 1)}><ChevronLeft size={17}/></button><span>Page {current} of {pages}</span><button type="button" aria-label={`Next page of ${label}`} className={control} disabled={loading || current === pages} onClick={() => setPage(current + 1)}><ChevronRight size={17}/></button></div>
    </div>
  </section>;
}
