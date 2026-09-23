import { AuthProvider } from '../context/AuthContext.jsx';
import { NoticeProvider } from '../components/ui/index.jsx';

export default function Providers({ children }) {
  return <AuthProvider><NoticeProvider>{children}</NoticeProvider></AuthProvider>;
}
