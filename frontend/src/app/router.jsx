import LoanDashboardPage from '../features/loans/pages/LoanDashboardPage.jsx';
import SystemSettingsPage from '../features/settings/pages/SystemSettingsPage.jsx';
import { AlertTriangle } from 'lucide-react';
import { Button, Card } from "../components/ui/index.jsx";
import ApprovalsPage from "../features/approvals/pages/ApprovalsPage.jsx";
import DashboardPage from "../features/dashboard/pages/DashboardPage.jsx";
import FinancePage from "../features/finance/pages/FinancePage.jsx";
import LoansPage from "../features/loans/pages/LoansPage.jsx";
import LoginPage from "../features/auth/pages/LoginPage.jsx";
import MembersPage from "../features/members/pages/MembersPage.jsx";
import ReportsPage from "../features/reports/pages/ReportsPage.jsx";
import SavingsPage from "../features/savings/pages/SavingsPage.jsx";
import StatementPage from "../features/members/pages/StatementPage.jsx";
import { currentRoute, routeTo } from "../utils/index.js";

const access = {
  dashboard: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"],
  members: ["INITIATOR", "APPROVER", "ADMIN"],
  savings: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"],
  loans: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"],
  finance: ["INITIATOR", "APPROVER", "ADMIN"],
  approvals: ["APPROVER", "ADMIN"],
  reports: ["INITIATOR", "APPROVER", "ADMIN"],
  admin: ["ADMIN"],
  settings: ["ADMIN"],
  statement: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"],
};

export function renderRoute(route, user) {
  const base = route.split("?")[0];
  const permitted = access[base]?.includes(user.role);
  let page;
  if (!permitted) page = <Forbidden />;
  else switch (base) {
    case "dashboard": page = <DashboardPage />; break;
    case "members": page = <MembersPage route={route} />; break;
    case "savings": page = <SavingsPage route={route} />; break;
    case "loans": page = new URLSearchParams(route.split("?")[1]).get("view") === "dashboard" ? <LoanDashboardPage key={route} route={route} /> : <LoansPage route={route} />; break;
    case "finance": page = <FinancePage route={route} />; break;
    case "approvals": page = <ApprovalsPage />; break;
    case "reports": page = new URLSearchParams(route.split("?")[1]).get("page") === "audit" && user.role !== "ADMIN" ? <Forbidden /> : <ReportsPage key={route} route={route} />; break;
    case "settings": page = <SystemSettingsPage route={route} />; break;
    case "admin": page = <SystemSettingsPage route={route} />; break;
    case "statement": page = <StatementPage route={route} />; break;
    default: page = <NotFound />;
  }

  return page;
}

function Forbidden() {
  return <Card className="grid min-h-[60vh] place-items-center text-center"><div><span className="mx-auto grid size-14 place-items-center rounded-2xl bg-amber-50 text-amber-700"><AlertTriangle /></span><h1 className="mt-4 text-xl font-bold">Access not permitted</h1><p className="mt-2 text-sm text-slate-500">Your assigned role does not include this function.</p><Button className="mt-5" onClick={() => routeTo("dashboard")}>Return to dashboard</Button></div></Card>;
}

function NotFound() {
  return <Card className="grid min-h-[60vh] place-items-center text-center"><div><h1 className="text-5xl font-bold text-teal-700">404</h1><p className="mt-3 text-sm text-slate-500">This page does not exist.</p><Button className="mt-5" onClick={() => routeTo("dashboard")}>Return to dashboard</Button></div></Card>;
}

