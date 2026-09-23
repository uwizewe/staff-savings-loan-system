import { authService } from '../features/auth/services/authService.js';
import { createContext, useEffect, useMemo, useState } from "react";
import { getToken, setToken } from "../services/api.js";

export const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(Boolean(getToken()));

  useEffect(() => {
    const expire = () => { setUser(null); setLoading(false); };
    window.addEventListener("auth-expired", expire);
    if (getToken()) {
      authService.me().then(setUser).catch(() => setUser(null)).finally(() => setLoading(false));
    }
    return () => window.removeEventListener("auth-expired", expire);
  }, []);

  const value = useMemo(() => ({
    user,
    loading,
    async login(username, password) {
      const response = await authService.login(username, password);
      setToken(response.token);
      setUser(response.user);
      return response.user;
    },
    async logout() {
      try { await authService.logout(); } catch { /* local logout still succeeds */ }
      setToken(null);
      setUser(null);
      window.location.hash = "dashboard";
    },
  }), [user, loading]);

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

