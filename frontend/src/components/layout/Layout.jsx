import { menu } from '../../constants/navigation.js';
import { useEffect, useMemo, useRef, useId, useState } from "react";
import {
  BarChart3, BookOpenCheck, ChevronDown, CircleDollarSign, ClipboardCheck,
  Coins, FileBarChart, Gauge, Landmark, LogOut, Menu, Settings, ShieldCheck,
  Users, UserRound, WalletCards, X,
} from "lucide-react";
import { useAuth } from "../../features/auth/hooks/useAuth.jsx";
import { Button, Modal } from "../ui/index.jsx";
import { currentRoute, routeTo } from "../../utils/index.js";



const allowed = (roles, role) => !roles || roles.includes(role);

export default function Layout({ children, route }) {
  const { user, logout } = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const [profileOpen, setProfileOpen] = useState(false);
  const [accountOpen, setAccountOpen] = useState(false);
  const accountRef = useRef(null);
  const accountButtonRef = useRef(null);
  const accountId = useId();

  useEffect(() => {
    if (!accountOpen) return;
    const outside = (event) => {
      if (!accountRef.current?.contains(event.target)) setAccountOpen(false);
    };
    const escape = (event) => {
      if (event.key === "Escape") {
        setAccountOpen(false);
        accountButtonRef.current?.focus();
      }
    };
    document.addEventListener("pointerdown", outside);
    document.addEventListener("focusin", outside);
    document.addEventListener("keydown", escape);
    return () => {
      document.removeEventListener("pointerdown", outside);
      document.removeEventListener("focusin", outside);
      document.removeEventListener("keydown", escape);
    };
  }, [accountOpen]);

  useEffect(() => { setAccountOpen(false); }, [route]);
  const [loggingOut, setLoggingOut] = useState(false);
  const signOut = async () => { setLoggingOut(true); try { await logout(); } finally { setLoggingOut(false); } };
  const [collapsed, setCollapsed] = useState(false);
  const baseRoute = route.split("?")[0];
  const [openGroups, setOpenGroups] = useState({});

  useEffect(() => {
    const matching = menu.find((item) => item.children?.some((child) => child.route.split("?")[0] === baseRoute));
    if (matching) setOpenGroups((value) => ({ ...value, [matching.label]: true }));
  }, [baseRoute]);

  const visibleMenu = useMemo(() => menu.filter((item) => allowed(item.roles, user.role)).map((item) => ({
    ...item,
    children: item.children?.filter((child) => allowed(child.roles, user.role)),
  })), [user.role]);

  const go = (nextRoute) => { routeTo(nextRoute); setMobileOpen(false); };
  const title = (baseRoute === "loans" && new URLSearchParams(route.split("?")[1]).get("view") === "dashboard" ? "Loan Dashboard" : null) || (baseRoute === "savings" && new URLSearchParams(route.split("?")[1]).get("view") === "dashboard" ? "Savings Dashboard" : null) || visibleMenu.flatMap((item) => item.children || [item]).find((item) => item.route === route)?.label || visibleMenu.find((item) => item.route === baseRoute || item.children?.some((child) => child.route.split("?")[0] === baseRoute))?.label || "VFR Association";

  const sidebar = (
    <aside className={`workspace-sidebar flex h-full flex-col border-r border-slate-200 bg-white text-slate-700 transition-all ${collapsed ? "w-[84px]" : "w-[272px]"}`}>
      <div className="flex h-20 items-center gap-3 border-b border-slate-100 px-5">
        <div className="grid size-10 shrink-0 place-items-center rounded-2xl bg-teal-700 text-white"><CircleDollarSign size={24} /></div>
        {!collapsed && <div className="min-w-0"><p className="truncate text-sm font-bold">VFR Association</p><p className="truncate text-[10px] uppercase tracking-[.18em] text-slate-400">Savings & Loans</p></div>}
        <button aria-label="Close navigation" onClick={() => setMobileOpen(false)} className="ml-auto grid size-8 place-items-center rounded-lg text-slate-400 hover:bg-slate-100 lg:hidden"><X size={18} /></button>
      </div>

      <nav aria-label="Main navigation" className="flex-1 space-y-1 overflow-y-auto px-3 py-5">
        {!collapsed && <p className="mb-3 px-3 text-[10px] font-semibold uppercase tracking-[.16em] text-slate-400">Main menu</p>}
        {visibleMenu.map((item) => {
          const Icon = item.icon;
          const childActive = item.children?.some((child) => child.route.split("?")[0] === baseRoute);
          const active = item.route?.split("?")[0] === baseRoute;
          const open = openGroups[item.label] ?? childActive;
          if (item.children) return (
            <div key={item.label}>
              <button title={item.label} aria-expanded={Boolean(open && !collapsed)} onClick={() => { if (collapsed) { setCollapsed(false); setOpenGroups((value) => ({ ...value, [item.label]: true })); } else setOpenGroups((value) => ({ ...value, [item.label]: !open })); }} className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition ${childActive ? "bg-slate-50 text-slate-900" : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"}`}>
                <Icon size={19} className="shrink-0" />
                {!collapsed && <><span className="flex-1">{item.label}</span><ChevronDown size={15} className={`transition ${open ? "rotate-180" : ""}`} /></>}
              </button>
              {open && !collapsed && <div className="ml-5 mt-1 space-y-0.5 border-l border-slate-200 pl-4">{item.children.map((child, childIndex) => {
                const isActive = currentRoute() === child.route || (child.route === baseRoute && route === child.route);
                return <div key={child.route}>{child.group && (childIndex === 0 || child.group !== item.children[childIndex - 1].group) && <p className="px-3 pb-1 pt-4 text-[10px] font-bold uppercase tracking-wider text-slate-400">{child.group}</p>}<button aria-current={isActive ? "page" : undefined} onClick={() => go(child.route)} className={`block w-full rounded-lg px-3 py-2 text-left text-xs transition ${isActive ? "bg-teal-50 font-semibold text-teal-800" : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"}`}>{child.label}</button></div>;
              })}</div>}
            </div>
          );
          return <button title={item.label} aria-current={active ? "page" : undefined} key={item.route} onClick={() => go(item.route)} className={`flex w-full items-center gap-3 rounded-xl px-3 py-2.5 text-left text-sm font-medium transition ${active ? "bg-teal-50 font-semibold text-teal-800" : "text-slate-500 hover:bg-slate-50 hover:text-slate-900"}`}><Icon size={19} className="shrink-0" />{!collapsed && item.label}</button>;
        })}
      </nav>

    </aside>
  );

  return (
    <div className="flex min-h-screen bg-[#f6f8fb]">
      <div className="fixed inset-y-0 left-0 z-40 hidden lg:block">{sidebar}</div>
      {mobileOpen && <div className="fixed inset-0 z-50 flex lg:hidden"><div className="h-full">{sidebar}</div><button className="flex-1 bg-slate-950/40 backdrop-blur-sm" onClick={() => setMobileOpen(false)} /></div>}

      <div className={`min-w-0 flex-1 transition-all ${collapsed ? "lg:ml-[84px]" : "lg:ml-[272px]"}`}>
        <header className="sticky top-0 z-30 flex min-h-20 items-center justify-between gap-3 border-b border-slate-200/70 bg-white/95 px-4 py-3 shadow-[0_2px_16px_rgba(15,42,34,0.03)] backdrop-blur-xl sm:px-6 lg:px-8">
          <div className="flex min-w-0 flex-1 items-center gap-3">
            <button aria-label="Open navigation" onClick={() => setMobileOpen(true)} className="grid size-10 place-items-center rounded-xl border border-slate-200 text-slate-600 hover:bg-teal-50 lg:hidden"><Menu size={18} /></button>
            <button aria-label={collapsed ? "Expand navigation" : "Collapse navigation"} aria-expanded={!collapsed} onClick={() => setCollapsed((value) => !value)} className="hidden size-10 place-items-center rounded-xl border border-slate-200 text-slate-500 hover:bg-teal-50 lg:grid"><Menu size={18} /></button>
            <div className="min-w-0"><p className="text-[10px] font-bold uppercase tracking-[.16em] text-teal-700">Workspace</p><p className="truncate text-base font-bold text-slate-900">{title}</p></div>
          </div>
          <div ref={accountRef} className="relative ml-auto shrink-0">
            <button ref={accountButtonRef} type="button" aria-label="Account options" aria-expanded={accountOpen} aria-controls={accountId} onClick={() => setAccountOpen((value) => !value)} className="flex items-center gap-2 rounded-2xl border border-slate-200 bg-white p-1.5 pr-2.5 text-left shadow-sm transition hover:border-teal-300 hover:bg-teal-50 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600 sm:gap-3 sm:pr-3">
              <span aria-hidden="true" className="grid size-9 shrink-0 place-items-center rounded-xl bg-teal-700 text-sm font-bold text-white">{user.fullName?.charAt(0)?.toUpperCase() || <UserRound size={18} />}</span>
              <span className="hidden min-w-0 sm:block"><span className="block max-w-40 truncate text-sm font-semibold text-slate-800">{user.fullName}</span><span className="block text-[10px] font-medium uppercase tracking-wider text-slate-500">{user.role.replaceAll("_", " ")}</span></span>
              <ChevronDown size={16} aria-hidden="true" className={`text-slate-500 transition ${accountOpen ? "rotate-180" : ""}`} />
            </button>
            <div id={accountId} hidden={!accountOpen} className="absolute right-0 top-full z-50 mt-2 w-64 max-w-[calc(100vw-2rem)] overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-xl">
              <div className="border-b border-slate-100 bg-slate-50 px-4 py-3"><p className="text-[10px] font-semibold uppercase tracking-wider text-slate-400">Signed in as</p><p className="mt-1 break-words text-sm font-bold text-slate-900">{user.fullName}</p><p className="mt-0.5 break-words text-xs text-slate-500">{user.email || user.username}</p></div>
              <div className="space-y-1 p-2">
                <button type="button" onClick={() => { accountButtonRef.current?.focus(); setAccountOpen(false); setProfileOpen(true); }} className="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-slate-700 transition hover:bg-teal-50 hover:text-teal-800 focus-visible:outline-2 focus-visible:outline-teal-600"><UserRound size={17} aria-hidden="true" />My profile</button>
                <button type="button" disabled={loggingOut} onClick={signOut} className="flex w-full items-center gap-3 rounded-xl px-3 py-3 text-sm font-semibold text-rose-700 transition hover:bg-rose-50 focus-visible:outline-2 focus-visible:outline-rose-500 disabled:opacity-50"><LogOut size={17} aria-hidden="true" />{loggingOut ? "Signing out…" : "Logout"}</button>
              </div>
            </div>
          </div>
        </header>
        <main className="animate-in mx-auto max-w-[1600px] p-4 pb-28 sm:p-6 sm:pb-24 lg:p-8 lg:pb-24">{children}</main>
        <footer className={`no-print fixed inset-x-0 bottom-0 z-20 flex min-h-14 flex-wrap items-center justify-center gap-x-2 gap-y-1 border-t border-slate-200 bg-white/95 px-4 py-3 text-center text-xs leading-5 text-slate-500 backdrop-blur-xl ${collapsed ? "lg:left-[84px]" : "lg:left-[272px]"}`}>
          <span>Developed by <span className="font-semibold text-slate-700">UWIZEWE JEAN D'AMOUR </span></span>
          <span aria-hidden="true" className="text-slate-300">·</span>
          <a href="tel:+250788672782" className="rounded font-medium text-teal-700 hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-teal-600">+2550788672782</a>
        </footer>
      </div>
      <Modal open={profileOpen} onClose={() => setProfileOpen(false)} title="My profile" description="Your account details and workspace access." size="md">
        <div className="mb-6 flex items-center gap-4 rounded-2xl border border-teal-100 bg-teal-50 p-5">
          <span className="grid size-14 shrink-0 place-items-center rounded-2xl bg-teal-700 text-xl font-bold text-white">{user.fullName?.charAt(0)?.toUpperCase() || <UserRound />}</span>
          <div className="min-w-0"><p className="break-words text-lg font-bold text-slate-900">{user.fullName}</p><p className="mt-1 flex items-center gap-1.5 text-xs font-medium text-teal-700"><ShieldCheck size={14} />{user.role.replaceAll("_", " ")}</p></div>
        </div>
        <dl className="grid gap-5 sm:grid-cols-2">
          {[ ["Username", user.username], ["Email address", user.email], ["Account status", user.enabled ? "Active" : "Disabled"], ["Member ID", user.memberId ?? "Not linked"] ].map(([label, value]) => <div key={label}><dt className="text-xs font-semibold text-slate-500">{label}</dt><dd className="mt-1.5 break-words text-sm font-medium text-slate-900">{value || "—"}</dd></div>)}
        </dl>
        <p className="mt-6 rounded-xl bg-slate-100 p-3 text-xs leading-5 text-slate-500">Contact your administrator to update your account details or permissions.</p>
        <div className="mt-6 flex justify-end border-t border-slate-200 pt-4"><Button type="button" variant="secondary" onClick={() => setProfileOpen(false)}>Close profile</Button></div>
      </Modal>
    </div>
  );
}
