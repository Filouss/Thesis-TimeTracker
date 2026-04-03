import { useState } from "react";
import { useExportActions } from "../../hooks/useExportActions";

type EditSessionModalProps = {
   onCancel: () => void
};

export function ExportModal({onCancel} : EditSessionModalProps) {
    const { 
        handleChange, 
        getExportData, 
        repoSuggestions, 
        issueSuggestions, 
        error 
    } = useExportActions();
    
    const [interval, setInterval] = useState("ThisWeek");
    const [repo, setRepo] = useState("");
    const [issue, setIssue] = useState("");
    const [format,setFormat] = useState("csv")
    const [showRepoSuggestions, setShowRepoSuggestions] = useState(false);
    const [showIssueSuggestions, setShowIssueSuggestions] = useState(false);

    return (
    <div className="modal-overlay" onClick={onCancel}>
      <div className="modal edit-modal" onClick={e => e.stopPropagation()}>
        <h3>Select export data</h3>

        <div className="issue-section autocomplete-field">
          <h4>GitHub Issue</h4>
          <input
            value={issue}
            onChange={(e) => {
                setIssue(e.target.value)    
                handleChange(e.target.value, "issue")
                setShowIssueSuggestions(true)
            }}
            placeholder="insert issue title if you want to filter exported data"
            className="issue-url-input"
            onBlur={() => setTimeout(() => setShowIssueSuggestions(false), 200)}
          />
          {showIssueSuggestions && issueSuggestions.length > 0 && (
            <ul className="suggestions-list">
              {issueSuggestions.map(s => (
                <li key={s} onClick={() => {
                    setIssue(s);
                    setShowIssueSuggestions(false);
                }}>
                  {s}
                </li>
              ))}
            </ul>
          )}

        </div>

        <div className="repo-section autocomplete-field">
          <h4>GitHub repository</h4>
          <input
            value={repo}
            onChange={(e) => {
                setRepo(e.target.value)
                handleChange(e.target.value, "repo")
                setShowRepoSuggestions(true)
            }}
            placeholder="insert repository name if you want to filter exported data"
            className="issue-url-input"
            onBlur={() => setTimeout(() => setShowRepoSuggestions(false), 200)}
          />
          {showRepoSuggestions && repoSuggestions.length > 0 && (
            <ul className="suggestions-list">
              {repoSuggestions.map(s => (
                <li key={s} onClick={() => {
                    setRepo(s);
                    setShowRepoSuggestions(false);
                }}>
                  {s}
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="interval-section">
          <h4>Select a period of time for the exported data</h4>
          <div className="data-select-wrapper">
                <select name="interval" value={interval} onChange={e => setInterval(e.target.value)} >
                    <option value="Today">Today</option>
                    <option value="Yesterday">Yesterday</option>
                    <option value="ThisWeek">This week</option>
                    <option value="LastWeek">Last week</option>
                    <option value="ThisMonth">This month</option>
                    <option value="LastMonth">Last month</option>
                    <option value="year">This year</option>
                </select>
            </div>
        </div>
        <h4>Select export format</h4>
        <div className="export-type-wrapper">
          <input type="radio" name="export-format" id="csv" value="csv" onClick={() => setFormat("csv")} checked={format === "csv"}/>
          <label htmlFor="csv">CSV</label>
          <input type="radio" name="export-format" id="pdf" value="pdf" onClick={() => setFormat("pdf")} checked={format === "pdf"}/>
          <label htmlFor="pdf">PDF</label>
        </div>
        {error && (
          <div className="modal-error" color="red">{error}</div>
        )}
        <div className="modal-actions">
          <button onClick={onCancel} className="modal-cancel">Cancel</button>
          <button 
            onClick={() => getExportData(issue, repo, interval, format)} 
            className="modal-confirm"
          >
            Export data
          </button>
        </div>
      </div>
    </div>
    );
}