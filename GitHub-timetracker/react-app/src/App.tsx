import { BrowserRouter, Routes, Route } from "react-router-dom";
import { RootRedirect } from "./components/nav/RootRedirect";
import LandingPage from "./pages/LandingPage";
import HomePage from "./pages/HomePage";

export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                {/* Public routes */}
                <Route path="/" element={<RootRedirect/>} />
                <Route path="/landing" element={<LandingPage/>} />
                <Route path="/home" element={<HomePage/>} />
            </Routes>
        </BrowserRouter>
    );
}
