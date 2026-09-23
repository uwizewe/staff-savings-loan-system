import { renderRoute } from './router.jsx';
import { useEffect, useState } from "react";
import { Loader2 } from "lucide-react";
import { useAuth } from "../features/auth/hooks/useAuth.jsx";
import Layout from "../components/layout/Layout.jsx";
import LoginPage from "../features/auth/pages/LoginPage.jsx";
import { currentRoute } from "../utils/index.js";

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

  const page = renderRoute(route, user);

  return <Layout route={route}>{page}</Layout>;
}

