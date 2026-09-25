import { useEffect, useMemo, useState } from 'react';
import { Download, FileBarChart, Printer, RefreshCw } from 'lucide-react';
import { Button, Card, DataTable, Field, inputClass, PageHeader, StatusBadge } from '../../../components/ui/index.jsx';
import { get } from '../../../services/api.js';
import { routeTo } from '../../../utils/index.js';
import { buildReport, localDate, monthRange } from '../services/reportEngine.js';
import { reportTitles, transactionTypes } from '../services/reportCatalog.js';
import { cellText, exportReport, formatNumber, periodLabel, printReport } from '../services/reportExport.js';

const monthlyTypes=['monthly-savings','monthly-summary'];
const memberTypes=['all-transactions','member-statement','savings','savings-transactions','loans','active-loans','closed-loans','repayments','outstanding','overdue'];
const statuses={ 'all-transactions':['DRAFT','PENDING_APPROVAL','APPROVED','REJECTED'], 'savings-transactions':['DRAFT','PENDING_APPROVAL','APPROVED','REJECTED'], repayments:['DRAFT','PENDING_APPROVAL','APPROVED','REJECTED'],loans:['ACTIVE','CLOSED','PENDING_APPROVAL','REJECTED','DRAFT'],savings:['ACTIVE','LEFT'],'monthly-savings':['PAID','PARTIAL','PENDING'],overdue:['DUE','OVERDUE','SERIOUSLY_OVERDUE'] };
const types={ 'all-transactions':transactionTypes,'savings-transactions':['Monthly Saving','Savings Deposit','Savings Withdrawal','Adjustment'],'income-expense':['Income','Expense'] };
function initialFilters(type,params){const today=localDate(new Date().toISOString());return {from:type==='daily'?today:monthlyTypes.includes(type)?monthRange(today.slice(0,7)).from:'',to:monthlyTypes.includes(type)?monthRange(today.slice(0,7)).to:today,search:'',memberId:params.get('memberId')||'',status:'',type:'',user:'',module:'',action:''};}

export default function ReportsPage({route}) {
  const params=new URLSearchParams(route.split('?')[1]);
  const type=params.get('page')||'vsa-account';
  const [draft,setDraft]=useState(()=>initialFilters(type,params));
  const [applied,setApplied]=useState(()=>initialFilters(type,params));
  const [data,setData]=useState(null),[loading,setLoading]=useState(true),[error,setError]=useState(''),[busy,setBusy]=useState('');
  const path=type==='audit'?'/reports/audit':'/reports/snapshot';
  useEffect(()=>{let live=true;get(path).then(result=>{if(live)setData(result);}).catch(e=>{if(live)setError(e.message);}).finally(()=>{if(live)setLoading(false);});return()=>{live=false;};},[path]);
  const report=useMemo(()=>data?buildReport(type,data,applied):null,[type,data,applied]);
  const update=(key,value)=>setDraft(old=>({...old,[key]:value}));
  const invalid=Boolean(draft.from&&draft.to&&draft.from>draft.to);
  const dirty=JSON.stringify(draft)!==JSON.stringify(applied);
  const missingMember=type==='member-statement'&&!draft.memberId;
  async function generate(event){event?.preventDefault();if(invalid||missingMember)return;setLoading(true);setError('');try{const snapshot=await get(path);setData(snapshot);setApplied({...draft});}catch(e){setError(e.message);}finally{setLoading(false);}}
  async function output(format){setBusy(format);setError('');try{if(format==='print')await printReport(report);else await exportReport(report,format);}catch(e){setError(`Could not prepare report: ${e.message}`);}finally{setBusy('');}}
  const disabled=!report||loading||Boolean(busy)||dirty||(type==='member-statement'&&!applied.memberId);
  const select=(key,label,options,all='All')=><Field key={key} label={label}><select className={inputClass} value={draft[key]} onChange={e=>update(key,e.target.value)}><option value="">{all}</option>{options.map(value=><option key={value} value={value}>{String(value).replaceAll('_',' ')}</option>)}</select></Field>;
  if(!reportTitles[type])return <Card><h1 className="text-xl font-bold">Report not found</h1><Button className="mt-4" onClick={()=>routeTo('reports?page=vsa-account')}>Open VSA Account Report</Button></Card>;
  return <div className="min-w-0 space-y-5">
    <PageHeader title={reportTitles[type]} description="Reports / Financial and operational records" actions={<><Button variant="secondary" disabled={disabled} onClick={()=>output('print')}><Printer size={16}/>Print</Button><Button variant="secondary" disabled={disabled} onClick={()=>output('pdf')}><Download size={16}/>{busy==='pdf'?'Preparing…':'Export PDF'}</Button><Button disabled={disabled} onClick={()=>output('xlsx')}><Download size={16}/>{busy==='xlsx'?'Preparing…':'Export Excel'}</Button></>}/>
    <Card><form onSubmit={generate} className="space-y-4">
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        {monthlyTypes.includes(type)&&<Field label="Reporting month"><input type="month" className={inputClass} value={draft.from?.slice(0,7)||''} onChange={e=>{if(e.target.value)setDraft(old=>({...old,...monthRange(e.target.value)}));}}/></Field>}
        <Field label="From Date"><input className={inputClass} type="date" value={draft.from} max={draft.to||undefined} onChange={e=>setDraft(old=>({...old,from:e.target.value,...(type==='daily'?{to:e.target.value}:{})}))}/></Field>
        <Field label="To Date"><input className={inputClass} type="date" value={draft.to} min={draft.from||undefined} onChange={e=>setDraft(old=>({...old,to:e.target.value,...(type==='daily'?{from:e.target.value}:{})}))}/></Field>
        {memberTypes.includes(type)&&<Field label="Member" required={type==='member-statement'}><select className={inputClass} value={draft.memberId} onChange={e=>update('memberId',e.target.value)}><option value="">{type==='member-statement'?'Select a member':'All members'}</option>{(data?.members||[]).map(m=><option key={m.id} value={m.id}>{m.memberCode} — {m.fullName}</option>)}</select></Field>}
        {statuses[type]&&select('status','Status',statuses[type],'All statuses')}
        {types[type]&&select('type','Transaction Type',types[type],'All types')}
        {type==='audit'&&['user','module','action'].map(key=>select(key,key[0].toUpperCase()+key.slice(1),[...new Set((data?.audits||[]).map(r=>key==='module'?r.entityType:r[key]).filter(Boolean))].sort()))}
        <Field label="Search"><input className={inputClass} type="search" placeholder={type==='monthly-summary'?'Search summary metrics':'Name, reference, description…'} value={draft.search} onChange={e=>update('search',e.target.value)}/></Field>
      </div>
      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-4"><p className="text-xs text-slate-500">{dirty?'Filters changed. Generate to update this report and its exports.':'Print and export include every matching row across all pages.'}</p><Button type="submit" loading={loading} disabled={invalid||missingMember}><RefreshCw size={16}/>Generate</Button></div>
      {invalid&&<p role="alert" className="text-sm text-rose-700">From Date must be on or before To Date.</p>}
    </form></Card>
    {error&&<div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-800">{error}<Button variant="ghost" className="ml-2" onClick={generate}>Retry</Button></div>}
    {report&&<>
      <div className="flex flex-wrap items-center justify-between gap-4 rounded-2xl bg-teal-900 p-5 text-white"><div className="flex items-center gap-3"><FileBarChart size={25}/><div><p className="font-semibold">{report.organization}</p><p className="mt-1 text-xs text-teal-100">{periodLabel(report)}</p></div></div><div className="text-xs leading-6 text-teal-100"><p>Generated: {new Date(report.generatedAt).toLocaleString('en-GB')}</p><p>Currency: {report.currency}</p></div></div>
      {report.member&&<Card><dl className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">{Object.entries(report.member).map(([key,value])=><div key={key}><dt className="text-xs capitalize text-slate-500">{key==='code'?'Member Code':key==='name'?'Member Name':key}</dt><dd className="mt-1 font-semibold text-slate-800">{value||'—'}</dd></div>)}</dl></Card>}
      <ReportSummary items={report.summary} currency={report.currency}/>
      {report.sections.map(section=><ReportSection key={`${section.title}-${report.generatedAt}`} section={section} loading={loading}/>)}
      {!!report.notes.length&&<Card><h2 className="mb-2 text-sm font-semibold text-slate-700">About this report</h2><ul className="list-disc space-y-2 pl-4 text-xs leading-5 text-slate-500">{report.notes.map(note=><li key={note}>{note}</li>)}</ul></Card>}
    </>}
    {!report&&loading&&<DataTable columns={[]} rows={[]} loading label="Report" searchable={false}/>}
  </div>;
}

function ReportSummary({items,currency}) {return !!items.length&&<div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{items.map(item=><Card key={item.label}><p className="text-xs font-medium text-slate-500">{item.label}</p><p className="mt-3 break-words text-2xl font-semibold tabular-nums text-slate-900">{item.money?formatNumber(item.value):item.value}</p><p className="mt-2 text-[10px] uppercase tracking-wider text-teal-700">{item.money?currency:'Count'}</p></Card>)}</div>;}
function ReportSection({section,loading}) {
  const columns=section.columns.map(column=>({...column,render:row=>column.type==='savings-action'?<Button size="sm" variant="secondary" onClick={()=>routeTo(`savings?view=dashboard&id=${row.memberId}`)}>View Savings Statement</Button>:column.type==='status'?<StatusBadge value={row[column.key]}/>:<span className={column.type==='money'?'font-medium tabular-nums':column.key==='description'?'inline-block min-w-48 max-w-sm whitespace-normal':''}>{cellText(row,column)}</span>}));
  return <section className="min-w-0 space-y-3"><div className="flex flex-wrap items-center justify-between gap-2"><h2 className="text-base font-semibold text-slate-800">{section.title}</h2><span className="text-xs text-slate-500">{section.rows.length} matching records</span></div><DataTable columns={columns} rows={section.rows} loading={loading} label={section.title} searchable={false} empty="No records match these filters"/>{!!section.totals.length&&<div className="flex flex-wrap justify-end gap-5 rounded-xl bg-slate-100 p-4">{section.totals.map(item=><p key={item.label} className="text-sm text-slate-600">{item.label}: <strong>{item.money?formatNumber(item.value):item.value}</strong></p>)}</div>}</section>;
}
