import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { AuthProvider } from "./auth";
import { NoticeProvider } from "./components/ui";
import "./index.css";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <AuthProvider>
      <NoticeProvider>
        <App />
      </NoticeProvider>
    </AuthProvider>
  </StrictMode>,
);

