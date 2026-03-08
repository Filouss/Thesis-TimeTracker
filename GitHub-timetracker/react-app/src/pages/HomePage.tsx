import TopBar from "../components/nav/TopBar";
import "../styles/homepage.css"

export default function HomePage() {
    return(
        <div className="homepage-container">
            <TopBar></TopBar>
            <div className="homepage-content-wrapper">
                <div className="homepage-content">
                    <div className="homepage-issues">
                        <div className="assigned-wrapper">
                            <div className="assigned-title">
                                Assigned Issues
                            </div>
                            <div className="issue-card">
                                <div className="card-row">
                                    <div className="header-left">
                                        <div className="issue-info">
                                            <span className="repo-name">repo-name</span> / 
                                            <span className="issue-title">Issue title</span>
                                        </div>
                                        <div className="status-badge">Open</div>
                                    </div>
                                    
                                    <div className="pin-action">Pin</div>
                                </div>

                                <div className="card-row">
                                    <div className="issue-label">label</div>
                                    <div className="tracked-time">Tracked time: 1h 20m</div>
                                </div>

                                <div className="card-row footer">
                                    <button className="tile-btn">Start tracking</button>
                                    <button className="tile-btn">Open in GitHub</button>
                                </div>
                            </div>
                        </div>
                        <div className="pinned-wrapper">

                        </div>
                        <div className="session-tracking-wrapper">
                            <div className="tracking-wrapper">

                            </div>
                            <div className="sync-wrapper">

                            </div>
                        </div>
                    </div>
                <div className="homepage-btns">
                    <button>End session</button>
                    <button>Sync all to Github</button>
                </div>
                </div>
            </div>
        </div>
    )
};
