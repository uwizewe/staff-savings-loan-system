import { useState } from 'react';
import { Plus, Settings2 } from 'lucide-react';
import { post, put } from '../../../services/api.js';
import { Button, DataTable, ErrorState, Field, inputClass, Modal, PageHeader, StatusBadge, useNotice } from '../../../components/ui/index.jsx';
import { useApiData } from '../../../hooks/useApiData.js';

const categoryBlank = { name: "", categoryType: "INCOME", active: true };
export default function FinanceCategoriesPage() {
  const categories=useApiData('/admin/categories');
  const notify=useNotice();
  const [categoryModal, setCategoryModal] = useState(null);
  const [categoryForm, setCategoryForm] = useState(categoryBlank);
  const [busy, setBusy] = useState(false);
  const [error,setError]=useState('');
  const openCategory = (category = null) => { setError(''); setCategoryModal(category || "new"); setCategoryForm(category || categoryBlank); };
  const saveCategory = async (event) => { event.preventDefault(); setBusy(true); try { if (categoryModal === "new") await post("/admin/categories", categoryForm); else await put(`/admin/categories/${categoryModal.id}`, categoryForm); notify("Category saved"); setCategoryModal(null); categories.reload(); } catch (e) { setError(e.message); } finally { setBusy(false); } };
  const toggle=async row=>{setBusy(true);setError('');try{await put(`/admin/categories/${row.id}`,{...row,active:!row.active});categories.reload();}catch(e){setError(e.message);}finally{setBusy(false);}};
  return <><PageHeader title="I&E Categories" description="System Settings · Organize income and expense transactions." actions={<Button disabled={busy} onClick={()=>openCategory()}><Plus size={16}/>Add I&E category</Button>}/>
  {!categoryModal&&error&&<ErrorState message={error}/>}
  {categories.error?<ErrorState message={categories.error} onRetry={categories.reload}/>:<DataTable label="I&E categories" loading={categories.loading} rows={categories.data} columns={[{ key: "name", label: "Category", className: "font-semibold" }, { key: "categoryType", label: "Type", render: (row) => <StatusBadge value={row.categoryType} /> }, { key: "active", label: "Status", render: (row) => <StatusBadge value={row.active ? "ACTIVE" : "DISABLED"} /> }, { key: "actions", label: "Actions", render: (row) => <div className="flex gap-2"><Button size="sm" variant="ghost" onClick={() => openCategory(row)}><Settings2 size={15} />Edit</Button><Button size="sm" variant="secondary" disabled={busy} onClick={()=>toggle(row)}>{row.active?'Deactivate':'Activate'}</Button></div> }]} />}
      <Modal open={Boolean(categoryModal)} onClose={() => setCategoryModal(null)} title={categoryModal === "new" ? "Add I&E category" : "Edit I&E category"} size="sm"><form onSubmit={saveCategory} className="space-y-4">{error&&<ErrorState message={error}/>}<Field label="Category name" required><input className={inputClass} value={categoryForm.name} onChange={(e) => setCategoryForm({ ...categoryForm, name: e.target.value })} required /></Field><Field label="Category type" required><select className={inputClass} value={categoryForm.categoryType} onChange={(e) => setCategoryForm({ ...categoryForm, categoryType: e.target.value })}><option>INCOME</option><option>EXPENSE</option></select></Field><label className="flex items-center gap-2 text-sm font-medium text-slate-600"><input type="checkbox" className="size-4 accent-teal-700" checked={categoryForm.active} onChange={(e) => setCategoryForm({ ...categoryForm, active: e.target.checked })} />Category is active</label><div className="flex flex-wrap justify-end gap-2"><Button type="button" variant="secondary" onClick={() => setCategoryModal(null)}>Cancel</Button><Button type="submit" loading={busy}>Save category</Button></div></form></Modal>


</>;
}
