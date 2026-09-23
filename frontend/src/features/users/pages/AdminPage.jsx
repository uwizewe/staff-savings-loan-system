import { useState } from "react";
import { KeyRound, Plus, Settings2, ShieldCheck, UserCog } from "lucide-react";
import { post, put } from "../../../services/api.js";
import { Button, Card, DataTable, ErrorState, Field, inputClass, Loading, Modal, PageHeader, StatusBadge, Tabs, useNotice } from "../../../components/ui/index.jsx";
import { useApiData } from "../../../hooks/useApiData.js";
import { dateTime, routeTo } from "../../../utils/index.js";

const userBlank = { username: "", fullName: "", email: "", role: "INITIATOR", password: "", enabled: true, memberId: "" };
const categoryBlank = { name: "", categoryType: "INCOME", active: true };

export default function AdminPage({ route }) {
  const tab = new URLSearchParams(route.split("?")[1] || "").get("tab") || "users";
  const users = useApiData("/admin/users");
  const permissions = useApiData("/admin/permissions");
  const categories = useApiData("/admin/categories");
  const settings = useApiData("/admin/settings");
  const audits = useApiData("/admin/audit-logs");
  const members = useApiData("/members");
  const notify = useNotice();
  const [userModal, setUserModal] = useState(null);
  const [userForm, setUserForm] = useState(userBlank);
  const [categoryModal, setCategoryModal] = useState(null);
  const [categoryForm, setCategoryForm] = useState(categoryBlank);
  const [settingModal, setSettingModal] = useState(null);
  const [settingValue, setSettingValue] = useState("");
  const [passwordUser, setPasswordUser] = useState(null);
  const [newPassword, setNewPassword] = useState("");
  const [busy, setBusy] = useState(false);

  const loading = users.loading || permissions.loading || categories.loading || settings.loading || audits.loading || members.loading;
  const error = users.error || permissions.error || categories.error || settings.error || audits.error || members.error;
  if (loading) return <Loading label="Loading administration…" />;
  if (error) return <ErrorState message={error} onRetry={() => { users.reload(); permissions.reload(); categories.reload(); settings.reload(); audits.reload(); members.reload(); }} />;

  const openUser = (user = null) => { setUserModal(user || "new"); setUserForm(user ? { ...user, password: "", memberId: user.memberId || "" } : userBlank); };
  const saveUser = async (event) => { event.preventDefault(); setBusy(true); try { const payload = { ...userForm, memberId: userForm.memberId ? Number(userForm.memberId) : null, password: userForm.password || null }; if (userModal === "new") await post("/admin/users", payload); else await put(`/admin/users/${userModal.id}`, payload); notify(userModal === "new" ? "User created" : "User updated"); setUserModal(null); users.reload(); } catch (e) { notify(e.message, "error"); } finally { setBusy(false); } };
  const savePassword = async (event) => { event.preventDefault(); setBusy(true); try { await post(`/admin/users/${passwordUser.id}/password`, { password: newPassword }); notify("Password updated; previous sessions were closed"); setPasswordUser(null); setNewPassword(""); } catch (e) { notify(e.message, "error"); } finally { setBusy(false); } };
  const openCategory = (category = null) => { setCategoryModal(category || "new"); setCategoryForm(category || categoryBlank); };
  const saveCategory = async (event) => { event.preventDefault(); setBusy(true); try { if (categoryModal === "new") await post("/admin/categories", categoryForm); else await put(`/admin/categories/${categoryModal.id}`, categoryForm); notify("Category saved"); setCategoryModal(null); categories.reload(); } catch (e) { notify(e.message, "error"); } finally { setBusy(false); } };
  const saveSetting = async (event) => { event.preventDefault(); setBusy(true); try { await put(`/admin/settings/${settingModal.id}`, { value: settingValue }); notify("Setting updated"); setSettingModal(null); settings.reload(); } catch (e) { notify(e.message, "error"); } finally { setBusy(false); } };

  return (
    <>
      <PageHeader title="Administration" description="Manage users, access responsibilities, categories, settings and audit history." actions={tab === "users" ? <Button onClick={() => openUser()}><Plus size={16} />Add user</Button> : tab === "categories" ? <Button onClick={() => openCategory()}><Plus size={16} />Add category</Button> : null} />
      <Tabs value={tab} onChange={(value) => routeTo(`admin?tab=${value}`)} items={[{ value: "users", label: "Users" }, { value: "roles", label: "Roles & permissions" }, { value: "categories", label: "Categories" }, { value: "settings", label: "System settings" }, { value: "audit", label: "Audit logs" }]} />

      {tab === "users" && <DataTable rows={users.data} columns={[{ key: "username", label: "Username", className: "font-semibold text-teal-800" }, { key: "fullName", label: "Full name" }, { key: "email", label: "Email" }, { key: "role", label: "Role", render: (row) => <StatusBadge value={row.role} /> }, { key: "enabled", label: "Account", render: (row) => <StatusBadge value={row.enabled ? "ACTIVE" : "DISABLED"} /> }, { key: "actions", label: "Actions", render: (row) => <div className="flex gap-1"><Button size="sm" variant="ghost" onClick={() => openUser(row)}><UserCog size={15} />Edit</Button><Button size="sm" variant="ghost" onClick={() => setPasswordUser(row)}><KeyRound size={15} />Password</Button></div> }]} />}

      {tab === "roles" && <div className="grid gap-5 md:grid-cols-2">{Object.entries(permissions.data).map(([role, items]) => <Card key={role}><div className="flex items-center gap-3"><span className="grid size-11 place-items-center rounded-2xl bg-teal-50 text-teal-700"><ShieldCheck size={22} /></span><div><h2 className="font-bold text-slate-900">{role.replaceAll("_", " ")}</h2><p className="text-xs text-slate-500">Built-in responsibility group</p></div></div><ul className="mt-5 space-y-2">{items.map((item) => <li key={item} className="flex items-center gap-2 rounded-xl bg-slate-50 px-3 py-2 text-sm text-slate-600"><span className="size-1.5 rounded-full bg-teal-500" />{item}</li>)}</ul></Card>)}</div>}

      {tab === "categories" && <DataTable rows={categories.data} columns={[{ key: "name", label: "Category", className: "font-semibold" }, { key: "categoryType", label: "Type", render: (row) => <StatusBadge value={row.categoryType} /> }, { key: "active", label: "Status", render: (row) => <StatusBadge value={row.active ? "ACTIVE" : "DISABLED"} /> }, { key: "actions", label: "Actions", render: (row) => <Button size="sm" variant="ghost" onClick={() => openCategory(row)}><Settings2 size={15} />Edit</Button> }]} />}

      {tab === "settings" && <div className="grid gap-4 lg:grid-cols-2">{settings.data.map((setting) => <Card key={setting.id}><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-wider text-slate-400">{setting.key}</p><p className="mt-2 text-xl font-bold text-slate-900">{setting.value}</p><p className="mt-2 text-sm text-slate-500">{setting.description}</p></div><Button size="sm" variant="secondary" onClick={() => { setSettingModal(setting); setSettingValue(setting.value); }}>Change</Button></div></Card>)}</div>}

      {tab === "audit" && <DataTable pageSize={15} rows={audits.data} columns={[{ key: "createdAt", label: "Date & time", render: (row) => dateTime(row.createdAt) }, { key: "user", label: "User" }, { key: "action", label: "Action", render: (row) => <span className="font-semibold text-teal-800">{row.action}</span> }, { key: "entityType", label: "Area" }, { key: "reference", label: "Reference" }, { key: "previousStatus", label: "Previous" }, { key: "newStatus", label: "New" }, { key: "details", label: "Details", className: "max-w-72 truncate" }]} />}

      <Modal open={Boolean(userModal)} onClose={() => setUserModal(null)} title={userModal === "new" ? "Create user" : "Update user"} size="lg"><form onSubmit={saveUser} className="grid gap-4 sm:grid-cols-2"><Field label="Username" required><input className={inputClass} value={userForm.username} onChange={(e) => setUserForm({ ...userForm, username: e.target.value })} required /></Field><Field label="Full name" required><input className={inputClass} value={userForm.fullName} onChange={(e) => setUserForm({ ...userForm, fullName: e.target.value })} required /></Field><Field label="Email" required><input type="email" className={inputClass} value={userForm.email} onChange={(e) => setUserForm({ ...userForm, email: e.target.value })} required /></Field><Field label="Role" required><select className={inputClass} value={userForm.role} onChange={(e) => setUserForm({ ...userForm, role: e.target.value })}><option>MEMBER</option><option>INITIATOR</option><option>APPROVER</option><option>ADMIN</option></select></Field>{userForm.role === "MEMBER" && <Field label="Linked member" required className="sm:col-span-2"><select className={inputClass} value={userForm.memberId} onChange={(e) => setUserForm({ ...userForm, memberId: e.target.value })} required><option value="">Select member</option>{members.data.map((member) => <option key={member.id} value={member.id}>{member.memberCode} — {member.fullName}</option>)}</select></Field>}<Field label={userModal === "new" ? "Temporary password" : "New password (optional)"} required={userModal === "new"}><input type="password" minLength="8" className={inputClass} value={userForm.password} onChange={(e) => setUserForm({ ...userForm, password: e.target.value })} required={userModal === "new"} /></Field><label className="mt-6 flex items-center gap-2 text-sm font-medium text-slate-600"><input type="checkbox" className="size-4 accent-teal-700" checked={userForm.enabled} onChange={(e) => setUserForm({ ...userForm, enabled: e.target.checked })} />Account is active</label><div className="flex justify-end gap-2 border-t border-slate-100 pt-4 sm:col-span-2"><Button type="button" variant="secondary" onClick={() => setUserModal(null)}>Cancel</Button><Button type="submit" loading={busy}>Save user</Button></div></form></Modal>

      <Modal open={Boolean(passwordUser)} onClose={() => setPasswordUser(null)} title="Reset password" description={passwordUser?.fullName} size="sm"><form onSubmit={savePassword} className="space-y-4"><Field label="New password" required hint="Minimum 8 characters"><input type="password" minLength="8" className={inputClass} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} required /></Field><p className="rounded-xl bg-amber-50 p-3 text-xs text-amber-700">All active sessions for this user will be closed.</p><div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={() => setPasswordUser(null)}>Cancel</Button><Button type="submit" loading={busy}>Update password</Button></div></form></Modal>

      <Modal open={Boolean(categoryModal)} onClose={() => setCategoryModal(null)} title={categoryModal === "new" ? "Add category" : "Update category"} size="sm"><form onSubmit={saveCategory} className="space-y-4"><Field label="Category name" required><input className={inputClass} value={categoryForm.name} onChange={(e) => setCategoryForm({ ...categoryForm, name: e.target.value })} required /></Field><Field label="Category type" required><select className={inputClass} value={categoryForm.categoryType} onChange={(e) => setCategoryForm({ ...categoryForm, categoryType: e.target.value })}><option>INCOME</option><option>EXPENSE</option></select></Field><label className="flex items-center gap-2 text-sm font-medium text-slate-600"><input type="checkbox" className="size-4 accent-teal-700" checked={categoryForm.active} onChange={(e) => setCategoryForm({ ...categoryForm, active: e.target.checked })} />Category is active</label><div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={() => setCategoryModal(null)}>Cancel</Button><Button type="submit" loading={busy}>Save category</Button></div></form></Modal>

      <Modal open={Boolean(settingModal)} onClose={() => setSettingModal(null)} title="Change system setting" description={settingModal?.description} size="sm"><form onSubmit={saveSetting} className="space-y-4"><Field label={settingModal?.key || "Value"} required><input className={inputClass} value={settingValue} onChange={(e) => setSettingValue(e.target.value)} required /></Field><div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={() => setSettingModal(null)}>Cancel</Button><Button type="submit" loading={busy}>Save setting</Button></div></form></Modal>
    </>
  );
}

