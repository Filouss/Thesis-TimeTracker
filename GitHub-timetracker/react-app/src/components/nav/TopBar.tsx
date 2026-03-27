import { useLocation, useNavigate } from "react-router-dom"
import "../../styles/nav/TopBar.css"
import { useState } from "react";
import { ExportModal } from "../modals/ExportModal";
import { http } from "../../lib/http";

export default function TopBar(){
    const navigate = useNavigate();
    const location = useLocation();
    const [showExport, setShowExport] = useState(false);
    

    function isActive(path: string) {
        return location.pathname.startsWith(path);
    }

    async function handleLogout() {
        await http.get("/logout")
        navigate("/landing")
    }

    return (
        <div className="topbar-wrapper">
            {showExport && (
                <ExportModal
                    onCancel={() => setShowExport(false)}
                />
            )}
            <div className="topbar-container">
                <div className="topbar-left-nav">
                    <div className="homepage-btn-wrapper">
                        <button className={`homepage-btn ${isActive("/home") ? "active" : ""}`} onClick={() => navigate("/home")}>
                            Home
                        </button>
                    </div>
                    <div className="overview-btn-wrapper">
                        <button className={`overview-btn ${isActive("/overview") ? "active" : ""}`} onClick={() => navigate("/overview")}>
                            Overview
                        </button>
                    </div>
                    <div className="export-btn-wrapper">
                        <button className="export-btn" onClick={() => {setShowExport(true)}}>
                            Export
                        </button>
                    </div>                    
                </div>
                <div className="topbar-logo">Timetracker</div>
                <div className="topbar-right-nav">
                    <div className="logout-btn-wrapper">
                        <button className="logout-btn" onClick={() => handleLogout()}>
                            Logout
                        </button>
                    </div>
                    <div className="history-btn-wrapper">
                        <button className={`history-btn ${isActive("/history") ? "active" : ""}`} onClick={() => navigate("/history")}>
                            Session History
                        </button>
                    </div>
                </div>
            </div>
        </div>
    )
}