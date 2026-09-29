import { BrowserRouter, Route, Routes } from "react-router-dom";
import { Layout } from "./components/Layout";
import { AdminPage } from "./pages/AdminPage";
import { EmPage } from "./pages/EmPage";
import { GroupPage } from "./pages/GroupPage";
import { HomePage } from "./pages/HomePage";
import { TeamPage } from "./pages/TeamPage";
import { VmPage } from "./pages/VmPage";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="gruppe/:groupId" element={<GroupPage />} />
          <Route path="lag/:teamId" element={<TeamPage />} />
          <Route path="em" element={<EmPage />} />
          <Route path="vm" element={<VmPage />} />
          {/* Deliberately not linked from the public pages; the backend enforces access. */}
          <Route path="admin" element={<AdminPage />} />
          <Route path="*" element={<p>Fant ikke siden.</p>} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
