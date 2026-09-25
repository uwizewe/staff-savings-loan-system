import { useState } from 'react';
import { put } from '../../../services/api.js';
import { Button, Card, ErrorState, Field, inputClass, Loading, Modal, PageHeader, useNotice } from '../../../components/ui/index.jsx';
import { useApiData } from '../../../hooks/useApiData.js';

export default function GeneralSettingsPage() {
  const settings=useApiData('/admin/settings');
  const notify=useNotice();
  const [settingModal, setSettingModal] = useState(null);
  const [settingValue, setSettingValue] = useState("");
  const [busy, setBusy] = useState(false);
  const saveSetting = async (event) => { event.preventDefault(); setBusy(true); try { await put(`/admin/settings/${settingModal.id}`, { value: settingValue }); notify("Setting updated"); setSettingModal(null); settings.reload(); } catch (e) { notify(e.message, "error"); } finally { setBusy(false); } };
  if(settings.loading)return <Loading/>;
  if(settings.error)return <ErrorState message={settings.error} onRetry={settings.reload}/>;
  return <><PageHeader title="General Settings" description="System Settings · Application configuration."/>
<div className="grid gap-4 lg:grid-cols-2">{settings.data.map((setting) => <Card key={setting.id}><div className="flex items-start justify-between gap-4"><div><p className="text-xs font-bold uppercase tracking-wider text-slate-400">{setting.key}</p><p className="mt-2 text-xl font-bold text-slate-900">{setting.value}</p><p className="mt-2 text-sm text-slate-500">{setting.description}</p></div><Button size="sm" variant="secondary" onClick={() => { setSettingModal(setting); setSettingValue(setting.value); }}>Change</Button></div></Card>)}</div>
      <Modal open={Boolean(settingModal)} onClose={() => setSettingModal(null)} title="Change system setting" description={settingModal?.description} size="sm"><form onSubmit={saveSetting} className="space-y-4"><Field label={settingModal?.key || "Value"} required><input className={inputClass} value={settingValue} onChange={(e) => setSettingValue(e.target.value)} required /></Field><div className="flex justify-end gap-2"><Button type="button" variant="secondary" onClick={() => setSettingModal(null)}>Cancel</Button><Button type="submit" loading={busy}>Save setting</Button></div></form></Modal>
</>;
}
