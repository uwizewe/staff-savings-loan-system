import { useState } from "react";
import { Eye, EyeOff, Landmark, LockKeyhole, ShieldCheck, UserRound } from "lucide-react";
import { useAuth } from "../auth";
import { Button, inputClass } from "../components/ui";

const demos = [
  { label: "Administrator", username: "admin", password: "Admin@123" },
  { label: "Initiator", username: "initiator", password: "Initiator@123" },
  { label: "Approver", username: "approver", password: "Approver@123" },
  { label: "Member", username: "member", password: "Member@123" },
];

export default function LoginPage() {
  const { login } = useAuth();
  const [form, setForm] = useState({ username: "admin", password: "Admin@123" });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const submit = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError("");
    try {
      await login(form.username, form.password);
      window.location.hash = "dashboard";
    } catch (requestError) {
      setError(requestError.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#063a33] p-4 sm:p-8">
      <div className="mx-auto grid min-h-[calc(100vh-2rem)] max-w-6xl overflow-hidden rounded-[2rem] bg-white shadow-2xl sm:min-h-[calc(100vh-4rem)] lg:grid-cols-[1.05fr_.95fr]">
        <section className="relative hidden overflow-hidden bg-gradient-to-br from-[#073d35] via-[#0b584a] to-[#0f766e] p-12 text-white lg:flex lg:flex-col lg:justify-between">
          <div className="absolute -right-24 -top-24 size-80 rounded-full border-[50px] border-white/5" />
          <div className="absolute -bottom-32 -left-24 size-96 rounded-full bg-emerald-300/10 blur-2xl" />
          <div className="relative flex items-center gap-3"><span className="grid size-11 place-items-center rounded-2xl bg-emerald-300 text-[#063a33]"><Landmark size={25} /></span><div><p className="font-bold">VFR ASSOCIATION</p><p className="text-xs text-teal-100/70">Savings & Loan Management</p></div></div>
          <div className="relative max-w-lg">
            <span className="mb-5 inline-flex items-center gap-2 rounded-full border border-white/15 bg-white/10 px-3 py-1.5 text-xs font-semibold text-teal-50"><ShieldCheck size={14} /> Controlled financial workflows</span>
            <h1 className="text-4xl font-bold leading-tight tracking-tight">Clear records. Strong controls. Better member service.</h1>
            <p className="mt-5 max-w-md text-sm leading-6 text-teal-50/70">Manage member savings, loan applications, repayments, income, expenses, approvals and reports from one simple workspace.</p>
          </div>
          <div className="relative grid grid-cols-3 gap-3 text-center">
            {["Role-based access", "Maker-checker", "Full audit trail"].map((item) => <div key={item} className="rounded-2xl border border-white/10 bg-white/5 px-3 py-4 text-xs font-semibold text-teal-50/85">{item}</div>)}
          </div>
        </section>

        <section className="flex items-center justify-center p-6 sm:p-12">
          <div className="w-full max-w-md">
            <div className="mb-8 lg:hidden"><span className="mb-3 grid size-11 place-items-center rounded-2xl bg-teal-700 text-white"><Landmark size={24} /></span><h1 className="text-xl font-bold">VFR Association</h1></div>
            <p className="text-xs font-bold uppercase tracking-[.16em] text-teal-700">Welcome back</p>
            <h2 className="mt-2 text-3xl font-bold tracking-tight text-slate-900">Sign in to your account</h2>
            <p className="mt-2 text-sm text-slate-500">Use the account assigned by your administrator.</p>

            <form onSubmit={submit} className="mt-8 space-y-5">
              {error && <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{error}</div>}
              <label className="block"><span className="mb-1.5 block text-xs font-semibold text-slate-700">Username</span><div className="relative"><UserRound size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" /><input className={`${inputClass} pl-10`} autoComplete="username" value={form.username} onChange={(event) => setForm({ ...form, username: event.target.value })} required /></div></label>
              <label className="block"><span className="mb-1.5 block text-xs font-semibold text-slate-700">Password</span><div className="relative"><LockKeyhole size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" /><input className={`${inputClass} px-10`} type={showPassword ? "text" : "password"} autoComplete="current-password" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /><button type="button" onClick={() => setShowPassword((value) => !value)} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-700">{showPassword ? <EyeOff size={17} /> : <Eye size={17} />}</button></div></label>
              <Button type="submit" size="lg" loading={loading} className="w-full">Sign in securely</Button>
            </form>

            <div className="mt-8 border-t border-slate-100 pt-6">
              <p className="mb-3 text-[11px] font-bold uppercase tracking-wider text-slate-400">Demo account shortcuts</p>
              <div className="grid grid-cols-2 gap-2">{demos.map((demo) => <button key={demo.username} type="button" onClick={() => setForm({ username: demo.username, password: demo.password })} className="rounded-xl border border-slate-200 px-3 py-2 text-left text-xs font-semibold text-slate-600 transition hover:border-teal-300 hover:bg-teal-50 hover:text-teal-800">{demo.label}</button>)}</div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}

