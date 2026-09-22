import { useEffect, useState } from "react";
import { AlertTriangle, Loader2 } from "lucide-react";
import { useAuth } from "./auth";
import Layout from "./components/Layout";
import { Button, Card } from "./components/ui";
import AdminPage from "./pages/AdminPage";
import ApprovalsPage from "./pages/ApprovalsPage";
import DashboardPage from "./pages/DashboardPage";
import FinancePage from "./pages/FinancePage";
import LoansPage from "./pages/LoansPage";
import LoginPage from "./pages/LoginPage";
import MembersPage from "./pages/MembersPage";
import ReportsPage from "./pages/ReportsPage";
import SavingsPage from "./pages/SavingsPage";
import StatementPage from "./pages/StatementPage";
import { currentRoute, routeTo } from "./utils";

const access = {
  dashboard: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"],
  members: ["INITIATOR", "APPROVER", "ADMIN"],
  savings: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"],
  loans: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"],
  finance: ["INITIATOR", "APPROVER", "ADMIN"],
  approvals: ["APPROVER", "ADMIN"],
  reports: ["INITIATOR", "APPROVER", "ADMIN"],
  admin: ["ADMIN"],
  statement: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"],
};

export default function App() {
  const { user, loading } = useAuth();
  const [route, setRoute] = useState(currentRoute());

  useEffect(() => {
    const update = () => setRoute(currentRoute());
    window.addEventListener("hashchange", update);
    return () => window.removeEventListener("hashchange", update);
  }, []);

  if (loading) return <div className="grid min-h-screen place-items-center bg-[#073d35] text-teal-50"><div className="flex items-center gap-3 text-sm"><Loader2 className="animate-spin" />Starting VFR Association…</div></div>;
  if (!user) return <LoginPage />;

  const base = route.split("?")[0];
  const permitted = access[base]?.includes(user.role);
  let page;
  if (!permitted) page = <Forbidden />;
  else switch (base) {
    case "dashboard": page = <DashboardPage />; break;
    case "members": page = <MembersPage route={route} />; break;
    case "savings": page = <SavingsPage route={route} />; break;
    case "loans": page = <LoansPage route={route} />; break;
    case "finance": page = <FinancePage route={route} />; break;
    case "approvals": page = <ApprovalsPage />; break;
    case "reports": page = <ReportsPage />; break;
    case "admin": page = <AdminPage route={route} />; break;
    case "statement": page = <StatementPage route={route} />; break;
    default: page = <NotFound />;
  }

  return <Layout route={route}>{page}</Layout>;
}

function Forbidden() {
  return <Card className="grid min-h-[60vh] place-items-center text-center"><div><span className="mx-auto grid size-14 place-items-center rounded-2xl bg-amber-50 text-amber-700"><AlertTriangle /></span><h1 className="mt-4 text-xl font-bold">Access not permitted</h1><p className="mt-2 text-sm text-slate-500">Your assigned role does not include this function.</p><Button className="mt-5" onClick={() => routeTo("dashboard")}>Return to dashboard</Button></div></Card>;
}

function NotFound() {
  return <Card className="grid min-h-[60vh] place-items-center text-center"><div><h1 className="text-5xl font-bold text-teal-700">404</h1><p className="mt-3 text-sm text-slate-500">This page does not exist.</p><Button className="mt-5" onClick={() => routeTo("dashboard")}>Return to dashboard</Button></div></Card>;
}

