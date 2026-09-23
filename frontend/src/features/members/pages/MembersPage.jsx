import { useEffect, useMemo, useState } from "react";
import { BookOpen, Pencil, Plus, UserRoundCheck } from "lucide-react";
import { post, put } from "../../../services/api.js";
import { Button, DataTable, ErrorState, Field, inputClass, Loading, Modal, MoneyCell, PageHeader, SearchBox, StatusBadge, textareaClass, useNotice } from "../../../components/ui/index.jsx";
import { useApiData } from "../../../hooks/useApiData.js";
import { date, routeTo, today } from "../../../utils/index.js";

const blank = {
  memberCode: "", fullName: "", department: "", phone: "", email: "",
  monthlySavingAmount: "", membershipStatus: "ACTIVE", riskStatus: "NORMAL",
  joiningDate: today(), exitDate: "", remarks: "",
};

export default function MembersPage({ route }) {
  const { data: members, loading, error, reload } = useApiData("/members");
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(blank);
  const [saving, setSaving] = useState(false);
  const notify = useNotice();
  const view = new URLSearchParams(route.split("?")[1] || "").get("view");

  useEffect(() => {
    if (view === "add") { setEditing("new"); setForm(blank); }
  }, [view]);

  const filtered = useMemo(() => (members || []).filter((member) => {
    const term = search.toLowerCase();
    return (!term || [member.fullName, member.memberCode, member.department, member.phone].some((value) => value?.toLowerCase().includes(term)))
      && (!status || member.membershipStatus === status);
  }), [members, search, status]);

  const edit = (member) => {
    setEditing(member);
    setForm({ ...member, exitDate: member.exitDate || "", remarks: member.remarks || "" });
  };

  const close = () => {
    setEditing(null);
    if (view === "add") routeTo("members");
  };

  const submit = async (event) => {
    event.preventDefault();
    setSaving(true);
    try {
      const payload = { ...form, monthlySavingAmount: Number(form.monthlySavingAmount), exitDate: form.membershipStatus === "LEFT" ? form.exitDate : null };
      if (editing === "new") await post("/members", payload);
      else await put(`/members/${editing.id}`, payload);
      notify(editing === "new" ? "Member registered" : "Member updated");
      close();
      reload();
    } catch (requestError) {
      notify(requestError.message, "error");
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <Loading label="Loading members…" />;
  if (error) return <ErrorState message={error} onRetry={reload} />;

  return (
    <>
      <PageHeader title={view === "statements" ? "Member statements" : "Members"} description="Central member register with savings and outstanding loan positions." actions={<Button onClick={() => { setEditing("new"); setForm(blank); }}><Plus size={16} />Add member</Button>} />
      <div className="mb-4 flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <SearchBox value={search} onChange={setSearch} placeholder="Search name, ID or department" />
        <select className={`${inputClass} sm:w-48`} value={status} onChange={(event) => setStatus(event.target.value)}><option value="">All memberships</option><option>ACTIVE</option><option>DISABLED</option><option>LEFT</option></select>
      </div>
      <DataTable rows={filtered} columns={[
        { key: "memberCode", label: "Member ID", className: "font-semibold text-teal-800" },
        { key: "fullName", label: "Member", render: (row) => <div><p className="font-semibold text-slate-800">{row.fullName}</p><p className="text-xs text-slate-400">{row.email}</p></div> },
        { key: "department", label: "Department" },
        { key: "monthlySavingAmount", label: "Monthly saving", render: (row) => <MoneyCell value={row.monthlySavingAmount} /> },
        { key: "totalSavings", label: "Total savings", render: (row) => <MoneyCell value={row.totalSavings} /> },
        { key: "outstandingLoans", label: "Outstanding", render: (row) => <MoneyCell value={row.outstandingLoans} /> },
        { key: "membershipStatus", label: "Membership", render: (row) => <StatusBadge value={row.membershipStatus} /> },
        { key: "riskStatus", label: "Risk", render: (row) => <StatusBadge value={row.riskStatus} /> },
        { key: "actions", label: "Actions", render: (row) => <div className="flex gap-1"><Button size="sm" variant="ghost" title="Edit" onClick={() => edit(row)}><Pencil size={15} /></Button><Button size="sm" variant="ghost" title="Statement" onClick={() => routeTo(`statement?id=${row.id}`)}><BookOpen size={15} /></Button></div> },
      ]} />

      <Modal open={Boolean(editing)} onClose={close} title={editing === "new" ? "Register member" : "Update member"} description="Membership and risk status are managed separately." size="lg">
        <form onSubmit={submit} className="grid gap-4 sm:grid-cols-2">
          <Field label="Registration ID"><input className={inputClass} value={form.memberCode || "Generated automatically (VFC)"} readOnly /></Field>
          <Field label="Full name" required><input className={inputClass} value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required /></Field>
          <Field label="Department" required><input className={inputClass} value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} required /></Field>
          <Field label="Phone number" required><input className={inputClass} value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} required /></Field>
          <Field label="Email" required><input className={inputClass} type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required /></Field>
          <Field label="Monthly saving (RWF)" required><input className={inputClass} type="number" min="0" value={form.monthlySavingAmount} onChange={(e) => setForm({ ...form, monthlySavingAmount: e.target.value })} required /></Field>
          <Field label="Membership status" required><select className={inputClass} value={form.membershipStatus} onChange={(e) => setForm({ ...form, membershipStatus: e.target.value })}><option>ACTIVE</option><option>DISABLED</option><option>LEFT</option></select></Field>
          <Field label="Risk status" required><select className={inputClass} value={form.riskStatus} onChange={(e) => setForm({ ...form, riskStatus: e.target.value })}><option>NORMAL</option><option>WATCHLIST</option></select></Field>
          <Field label="Joining date" required><input className={inputClass} type="date" value={form.joiningDate} onChange={(e) => setForm({ ...form, joiningDate: e.target.value })} required /></Field>
          {form.membershipStatus === "LEFT" && <Field label="Exit date" required><input className={inputClass} type="date" value={form.exitDate} onChange={(e) => setForm({ ...form, exitDate: e.target.value })} required /></Field>}
          <Field label="Remarks" className="sm:col-span-2"><textarea className={textareaClass} value={form.remarks} onChange={(e) => setForm({ ...form, remarks: e.target.value })} /></Field>
          <div className="flex justify-end gap-2 border-t border-slate-100 pt-4 sm:col-span-2"><Button type="button" variant="secondary" onClick={close}>Cancel</Button><Button type="submit" loading={saving}><UserRoundCheck size={16} />Save member</Button></div>
        </form>
      </Modal>
    </>
  );
}

