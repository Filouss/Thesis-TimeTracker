import { Routes, Route, useLocation, matchPath } from "react-router-dom";
import { RootRedirect } from "./components/nav/RootRedirect";
import LandingPage from "./pages/LandingPage";
import HomePage from "./pages/HomePage";
import { IssueProvider } from "./context/IssueContext";
import IssueDetailPage from "./pages/IssueDetailPage";
import TopBar from "./components/nav/TopBar";
import FaviconTracker from "./components/nav/FaviconTracker";
import HistoryPage from "./pages/HistoryPage";
import OverViewPage from "./pages/OverviewPage";
import NotFoundPage from "./pages/NotFoundPage";

import { ProtectedRoute } from "./pages/ProtectedRoute";

export default function App() {
    const location = useLocation();
    const knownRoutePatterns = [
        "/",
        "/landing",
        "/home",
        "/issues/:id",
        "/issues/:id/history",
        "/history",
        "/overview",
    ];
    const isKnownRoute = knownRoutePatterns.some((pattern) =>
        matchPath({ path: pattern, end: true }, location.pathname),
    );
    const shouldShowTopBar = !location.pathname.endsWith("landing") && isKnownRoute;

    return (
        <IssueProvider>
            <FaviconTracker />
            <main className="app-content-wrapper">
                    {shouldShowTopBar ? <TopBar /> : null}
                    <Routes>
                        <Route path="/" element={<RootRedirect/>} />
                        <Route path="/landing" element={<LandingPage/>} />
                        <Route path="/home" element={<ProtectedRoute><HomePage/></ProtectedRoute>} />
                        <Route path="/issues/:id" element={<ProtectedRoute><IssueDetailPage/></ProtectedRoute>} />
                        <Route path="/issues/:id/history" element={<ProtectedRoute><HistoryPage/></ProtectedRoute>} />
                        <Route path="/history" element={<ProtectedRoute><HistoryPage/></ProtectedRoute>} />
                        <Route path="/overview" element={<ProtectedRoute><OverViewPage/></ProtectedRoute>} />
                        <Route path="*" element={<NotFoundPage />} />
                    </Routes>
                </main>
        </IssueProvider>
    );
}
