import { http } from "../lib/http";

export function useCardActions(refetch?: () => void) {

    async function startTracking(issueNumber: number, repository_url: string) {
        // Parse owner and repo from repository_url
        // Format: https://api.github.com/repos/{owner}/{repo}
        const parts = repository_url.split("/repos/");
        if (parts.length !== 2) {
            throw new Error("Invalid repository_url: " + repository_url);
        }
        const [owner, repo] = parts[1].split("/");
        
        await http.post("/session/start", {
            issueNumber: issueNumber,
            owner: owner,
            repo: repo
        });

        refetch?.();
    }
    
    function openGithub(url: string) {
        window.open(url, "_blank");
    }

    async function Pin(issueNumber: number, repository_url: string) {
        // Parse owner and repo from repository_url
        // Format: https://api.github.com/repos/{owner}/{repo}
        const parts = repository_url.split("/repos/");
        if (parts.length !== 2) {
            throw new Error("Invalid repository_url: " + repository_url);
        }
        const [owner, repo] = parts[1].split("/");

        await http.post("/issues/pin", {
            issueNumber: issueNumber,
            owner: owner,
            repo: repo
        });
        refetch?.();
    }

    async function unPin(issueNumber: number, repository_url: string) {
        // Parse owner and repo from repository_url
        // Format: https://api.github.com/repos/{owner}/{repo}
        const parts = repository_url.split("/repos/");
        if (parts.length !== 2) {
            throw new Error("Invalid repository_url: " + repository_url);
        }
        const [owner, repo] = parts[1].split("/");

        await http.post("/issues/unpin", {
            issueNumber: issueNumber,
            owner: owner,
            repo: repo
        });
        refetch?.();
    }

    async function syncSession(sessionId: number, notes: string) {
        const zoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
        await http.post(`/session/sync?zoneId=${encodeURIComponent(zoneId)}`, {
            sessionId,
            notes,
        });
        refetch?.();
    }

    async function editSession(sessionId: number, timeblocks: {start: string, end: string}[], notes: string, synced: boolean, issueUrl?: string) {
        const payload: any = {
            issue: {repoOwner: "", repoName: "", issueNumber: null},
            notes: notes,
            timeBlocks: timeblocks,
            synced: synced
        };

        if (issueUrl) {
            // Parse the GitHub URL to extract owner, repo, and issue number
            const urlPattern = /^https:\/\/github\.com\/([^\/]+)\/([^\/]+)\/issues\/(\d+)$/;
            const match = issueUrl.match(urlPattern);
            if (match) {
                const [, owner, repo, issueNumber] = match;
                payload.issue = {
                    repoOwner: owner,
                    repoName: repo,
                    issueNumber: parseInt(issueNumber, 10)
                };
            }
        }

        await http.put(`/session/${sessionId}/update`, payload);
        refetch?.();
    }

    async function pauseTracking(){
        await http.post("/session/pause");
        refetch?.();
    }

    async function resumeTracking(){
        await http.post("/session/resume");
        refetch?.();
    }

    async function endTracking(notes: string){
        await http.post("/session/end", { notes });
        refetch?.();
    }

    async function deleteSession(sessionId: number) {
        const zoneId = Intl.DateTimeFormat().resolvedOptions().timeZone;
        await http.delete(`/session/${sessionId}/delete?zoneId=${encodeURIComponent(zoneId)}`);
        refetch?.();
    }


    return {
        startTracking,
        syncSession,
        openGithub,
        Pin,
        unPin,
        editSession,
        pauseTracking,
        resumeTracking,
        endTracking,
        deleteSession
    };
}