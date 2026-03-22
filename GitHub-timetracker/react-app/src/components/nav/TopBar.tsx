import { useLocation, useNavigate } from "react-router-dom"
import "../../styles/nav/TopBar.css"

export default function TopBar(){
    const navigate = useNavigate();
    const location = useLocation();

    function isActive(path: string) {
        return location.pathname.startsWith(path);
    }

    function handleLogout(): void {
        throw new Error("Function not implemented.");
    }

    return (
        <div className="topbar-wrapper">
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
                        <button className="export-btn" onClick={() => {}}>
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