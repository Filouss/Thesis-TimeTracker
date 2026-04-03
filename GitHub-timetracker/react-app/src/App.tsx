import { Routes, Route, useLocation } from "react-router-dom";
import { RootRedirect } from "./components/nav/RootRedirect";
import LandingPage from "./pages/LandingPage";
import HomePage from "./pages/HomePage";
import { IssueProvider } from "./context/IssueContext";
import IssueDetailPage from "./pages/IssueDetailPage";
import TopBar from "./components/nav/TopBar";
import HistoryPage from "./pages/HistoryPage";
import OverViewPage from "./pages/OverviewPage";

import { ProtectedRoute } from "./pages/ProtectedRoute";

export default function App() {
    const location = useLocation();

    return (
        <IssueProvider>
            <main className="app-content-wrapper">
                    {!location.pathname.endsWith("landing") ? <TopBar /> : ""}
                    <Routes>
                        <Route path="/" element={<RootRedirect/>} />
                        <Route path="/landing" element={<LandingPage/>} />
                        <Route path="/home" element={<ProtectedRoute><HomePage/></ProtectedRoute>} />
                        <Route path="/issues/:id" element={<ProtectedRoute><IssueDetailPage/></ProtectedRoute>} />
                        <Route path="/issues/:id/history" element={<ProtectedRoute><HistoryPage/></ProtectedRoute>} />
                        <Route path="/history" element={<ProtectedRoute><HistoryPage/></ProtectedRoute>} />
                        <Route path="/overview" element={<ProtectedRoute><OverViewPage/></ProtectedRoute>} />
                    </Routes>
                </main>
        </IssueProvider>
    );
}
