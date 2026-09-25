import { reportGroups } from '../features/reports/services/reportCatalog.js';
import {
  BarChart3, BookOpenCheck, ChevronDown, CircleDollarSign, ClipboardCheck,
  Coins, FileBarChart, Gauge, Landmark, LogOut, Menu, Settings, ShieldCheck,
  Users, UserRound, WalletCards, X,
} from "lucide-react";

export const menu = [
  { label: "Dashboard", icon: Gauge, route: "dashboard", roles: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"] },
  { label: "Members", icon: Users, route: "members", roles: ["INITIATOR", "APPROVER", "ADMIN"] },
  {
    label: "Savings", icon: Coins, roles: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"], children: [
      { label: "Members Saving", route: "savings?view=members" },
      { label: "Monthly Saving", route: "savings?view=monthly", roles: ["INITIATOR", "APPROVER", "ADMIN"] },
      { label: "Savings History", route: "savings?view=history" },
    ],
  },
  {
    label: "Loans", icon: Landmark, roles: ["MEMBER", "INITIATOR", "APPROVER", "ADMIN"], children: [
      { label: "Loan Application", route: "loans?view=applications" },
      { label: "Monthly Loan", route: "loans?view=monthly", roles: ["INITIATOR", "APPROVER", "ADMIN"] },
      { label: "Active Loan", route: "loans?view=active" },
      { label: "Closed Loan", route: "loans?view=closed" },
      { label: "Loan Repayment History", route: "loans?view=repayments" },
    ],
  },
  {
    label: "Income & Expenses", icon: WalletCards, roles: ["INITIATOR", "APPROVER", "ADMIN"], children: [
      { label: "All transactions", route: "finance?tab=history" },
      { label: "Income", route: "finance?tab=income" },
      { label: "Expenses", route: "finance?tab=expense" },
    ],
  },
  { label: "Approvals", icon: ClipboardCheck, route: "approvals", roles: ["APPROVER", "ADMIN"] },
  { label: "Reports", icon: FileBarChart, roles: ["INITIATOR", "APPROVER", "ADMIN"], children: reportGroups.flatMap(group => group.items.map(([key, label]) => ({label, group: group.label, route: `reports?page=${key}`, ...(key === "audit" ? {roles: ["ADMIN"]} : {})}))) },
  { label: "System Settings", icon: Settings, roles: ["ADMIN"], children: [
    { label: "Users", route: "settings?page=users" },
    { label: "Loan Categories", route: "settings?page=loan-categories" },
    { label: "I&E Categories", route: "settings?page=ie-categories" },
    { label: "Roles & Permissions", route: "settings?page=roles" },
    { label: "Audit Logs", route: "settings?page=audit" },
  ] },
  { label: "Savings Statement", icon: BookOpenCheck, route: "savings?view=dashboard", roles: ["MEMBER"] },
];
