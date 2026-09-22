import { createContext, useContext, useEffect, useMemo, useState } from "react";
import { api, getToken, setToken } from "./api";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(getToken()));

  useEffect(() => {
    const expire = () => { setUser(null); setLoading(false); };
    window.addEventListener("auth-expired", expire);
    if (getToken()) {
      api("/auth/me").then(setUser).catch(() => setUser(null)).finally(() => setLoading(false));
    }
    return () => window.removeEventListener("auth-expired", expire);
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    async login(username, password) {
      const response = await api("/auth/login", { method: "POST", body: { username, password } });
      setToken(response.token);
      setUser(response.user);
      return response.user;
    },
    async logout() {
      try { await api("/auth/logout", { method: "POST" }); } catch { /* local logout still succeeds */ }
      setToken(null);
      setUser(null);
      window.location.hash = "dashboard";
    },
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  return useContext(AuthContext);
}

