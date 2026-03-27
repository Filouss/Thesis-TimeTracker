import { useState, useRef } from "react";
import { http } from "../lib/http";
import { formatTrackedTime } from "../lib/utils";
import jsPDF from "jspdf";
import autoTable from "jspdf-autotable";

export function useExportActions(){
    const [repoSuggestions, setRepoSuggestions] = useState<string[]>([]);
    const [issueSuggestions, setIssueSuggestions] = useState<string[]>([]);
    const [error, setError] = useState("");
    const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    
    async function getExportData(issue: string, repo: string, interval: string, format: string){
        const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone;
        setError("");
        try {
            const res = await http.post(`/export`, {
              issueTitle: issue,
              repoName: repo,
              interval: interval,
              zoneId: timeZone
            });
            
            if (res.data.exportItems && res.data.exportItems.length === 0) {
                setError("No data found for these parameters");
                return;
            }
            console.log(format)
            if(format === "csv") generateCSV(res.data.exportItems, interval);
            else generatePDF(res.data.exportItems,interval);
         } catch (e) {
            console.log(e)
            setError("Failed to fetch export data");
            return null;
        }
    }

    function generateCSV(data: {issueTitle: string, repoName: string, timeTracked:number, createdAt: string}[], interval: string) {
        const headers = ["Issue Title", "Repository", "Time tracked", "Date"];
    
        const rows = data.map(item => [
            `"${item.issueTitle.replace(/"/g, '""')}"`, 
            `"${item.repoName.replace(/"/g, '""')}"`,
            `"${formatTrackedTime(item.timeTracked)}"`,
            `"${item.createdAt}"`
        ]);

        const csvString = [headers.map(h => `"${h}"`), ...rows].map(e => e.join(";")).join("\n");

        const blob = new Blob(["\ufeff", csvString], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.style.display = 'none'; 
        link.href = url;
        link.download = `timetracker-export-${interval}.csv`;

        document.body.appendChild(link); 
        link.click();
        
        document.body.removeChild(link);
        URL.revokeObjectURL(url);
        console.log("Download triggered successfully");
    }

    function generatePDF(data: {issueTitle: string, repoName: string, timeTracked:number, createdAt: string}[], interval: string){
        const doc = new jsPDF();

    doc.setFontSize(18);
    doc.text("GitHub Timetracker Report", 14, 22);
    
    doc.setFontSize(11);
    doc.setTextColor(100);
    doc.text(`Exported on: ${new Date().toLocaleDateString()}`, 14, 30);

    autoTable(doc, {
        startY: 40,
        head: [['Issue', 'Repository', 'Duration (s)', 'Date of creation']],
        body: data.map(item => [
            item.issueTitle, 
            item.repoName, 
            formatTrackedTime(item.timeTracked), 
            item.createdAt
        ]),
        theme: 'striped',
        headStyles: { fillColor: [15, 113, 115] }
    });

    doc.save(`timetracker-report-${interval}.pdf`);
    }

    function handleChange(input: string, fieldType: string){
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
        }

        if(input.length < 2){
            if(fieldType === "repo"){
                setRepoSuggestions([])
            } else {
                setIssueSuggestions([])
            }
            return;
        }

        timeoutRef.current = setTimeout(async () => {
            if(fieldType === "repo"){
                const res = await http.get(`/export/repo?query=${input}`);
                setRepoSuggestions(res.data.suggestions || []);
            } else {
                const res = await http.get(`/export/issue?query=${input}`);
                setIssueSuggestions(res.data.suggestions || []);
            }
        }, 300); 
    }
  
    return {
        handleChange,
        getExportData,
        generateCSV,
        generatePDF,
        repoSuggestions,
        issueSuggestions,
        error,
        setError
    };
}