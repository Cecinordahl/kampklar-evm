import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { AdminModeProvider } from "./lib/adminMode";
import { AdminPage } from "./pages/AdminPage";
import { EmPage } from "./pages/EmPage";
import { GroupPage } from "./pages/GroupPage";
import { HomePage } from "./pages/HomePage";
import { TeamPage } from "./pages/TeamPage";
import { VmPage } from "./pages/VmPage";

export default function App() {
  return (
    <BrowserRouter>
      <AdminModeProvider>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<HomePage />} />
            <Route path="gruppe/:groupId" element={<GroupPage />} />
            <Route path="lag/:teamId" element={<TeamPage />} />
            <Route path="em" element={<EmPage />} />
            <Route path="vm" element={<VmPage />} />
            {/* Login for admin mode; the backend enforces access on every write. */}
            <Route path="admin" element={<AdminPage />} />
            <Route path="*" element={<p>Fant ikke siden.</p>} />
          </Route>
        </Routes>
      </AdminModeProvider>
    </BrowserRouter>
  );
}
