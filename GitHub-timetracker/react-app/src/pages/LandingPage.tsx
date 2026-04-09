import "../styles/landing.css"
import GitHubLogo from "../assets/GitHub_Invertocat_Black_Clearspace.svg"

export default function LandingPage() {
    function startLoginFlow() {
        window.location.href = "http://localhost:8080/oauth2/authorization/github";
    }

    return (
        <div className="landing-container">
            <header className="hero">
                <h1>Precision time tracking <br/><span>for the GitHub ecosystem.</span></h1>
                <p>Stop manual logging. Sync your focus directly to GitHub issues, visualize your productivity, and export reports in seconds.</p>
                
                <button className="loginBtn" onClick={startLoginFlow}>
                    <img src={GitHubLogo} alt="GitHub" width="24" height="24" />
                    Sign in with GitHub
                </button>
                
            </header>

            <section className="features-grid">
                <div className="glass-card">
                    <div className="icon-circle">⚙️</div>
                    <h3>Native Integration</h3>
                    <p>Fetches your assigned GitHub issues automatically. No manual entry.</p>
                </div>
                <div className="glass-card">
                    <div className="icon-circle">📊</div>
                    <h3>Deep Analytics</h3>
                    <p>Beautiful charts show exactly where your time goes each week.</p>
                </div>
                <div className="glass-card">
                    <div className="icon-circle">📄</div>
                    <h3>Audit-Ready Reporting</h3>
                    <p>Generate CSV and PDF reports with a single click.</p>
                </div>
            </section>

            <div className="glow-1"></div>
            <div className="glow-2"></div>
            <div className="privacy-note">
                <p className="privacy-note">
                    <strong>Permissions Transparency:</strong><br/>
            We request <code>repo</code> access to synchronize time-logs with your issues, 
            and <code>user</code> access to identify your assignments. 
            <br/>
            <span className="sub-notice">
                *GitHub labels this as "Full Control," but we only interact with Issue and Session metadata.
            </span>
                </p>
            </div>
        </div>
    );
}