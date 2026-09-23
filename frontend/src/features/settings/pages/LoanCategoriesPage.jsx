import { useState } from 'react';
import { post, put } from '../../../services/api.js';
import { useApiData } from '../../../hooks/useApiData.js';
import { Button, DataTable, ErrorState, Field, inputClass, Loading, Modal, PageHeader, StatusBadge, textareaClass } from '../../../components/ui/index.jsx';
import { dateTime } from '../../../utils/index.js';
const blank={name:'',annualRate:'',description:'',active:true};
export default function LoanCategoriesPage() {
  const categories=useApiData('/loan-categories');
  const [form,setForm]=useState(null),[busy,setBusy]=useState(false),[error,setError]=useState('');
  const save=async event=>{event.preventDefault();setBusy(true);setError('');try{await(form.id?put(`/loan-categories/${form.id}`,form):post('/loan-categories',form));setForm(null);categories.reload();}catch(e){setError(e.message);}finally{setBusy(false);}};
  const toggle=async row=>{setBusy(true);setError('');try{await put(`/loan-categories/${row.id}`,{...row,active:!row.active});categories.reload();}catch(e){setError(e.message);}finally{setBusy(false);}};
  return <><PageHeader title="Loan categories" description="System Settings · Configure loan rates and availability. Existing loans keep their agreed rate." actions={<Button onClick={()=>{setError('');setForm({...blank});}}>Create category</Button>} />
    {!form && error && <ErrorState message={error} />}
    {categories.loading?<Loading/>:categories.error?<ErrorState message={categories.error} onRetry={categories.reload}/>:<DataTable rows={categories.data} columns={[
      {key:'name',label:'Category',className:'font-semibold'},{key:'annualRate',label:'Annual rate',render:r=>`${r.annualRate}%`},{key:'description',label:'Description'},
      {key:'active',label:'Status',render:r=><StatusBadge value={r.active?'ACTIVE':'INACTIVE'}/>},
      {key:'createdBy',label:'Created by'},{key:'createdAt',label:'Created',render:r=>dateTime(r.createdAt)},{key:'modifiedBy',label:'Modified by'},{key:'updatedAt',label:'Modified',render:r=>dateTime(r.updatedAt)},
      {key:'actions',label:'Actions',render:r=><div className="flex gap-2"><Button size="sm" variant="secondary" onClick={()=>{setError('');setForm({...r});}}>View / Edit</Button><Button size="sm" variant="ghost" disabled={busy} onClick={()=>toggle(r)}>{r.active?'Deactivate':'Activate'}</Button></div>}
    ]}/>}
    <Modal open={Boolean(form)} onClose={()=>!busy&&setForm(null)} title={form?.id?'Edit loan category':'Create loan category'}><form onSubmit={save} className="space-y-4">{error&&<ErrorState message={error}/>}<Field label="Category name" required><input className={inputClass} required maxLength={100} value={form?.name||''} onChange={e=>setForm({...form,name:e.target.value})}/></Field><Field label="Annual interest rate (%)" required><input className={inputClass} required type="number" min="0" max="100" step="0.001" value={form?.annualRate??''} onChange={e=>setForm({...form,annualRate:e.target.value})}/></Field><Field label="Description"><textarea className={textareaClass} maxLength={1000} value={form?.description||''} onChange={e=>setForm({...form,description:e.target.value})}/></Field><label className="flex items-center gap-2 text-sm"><input type="checkbox" checked={Boolean(form?.active)} onChange={e=>setForm({...form,active:e.target.checked})}/>Active category</label><div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={()=>setForm(null)}>Cancel</Button><Button type="submit" loading={busy}>Save category</Button></div></form></Modal>
  </>;
}
