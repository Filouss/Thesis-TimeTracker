import { useParams } from "react-router-dom";
import HistoryContent from "../components/history/HistoryContent";
import { useGetHistory } from "../hooks/useGetHistory";
import "../styles/HistoryPage.css";

export default function HistoryPage() {

    const {id} = useParams();
    const {sessions, refetch} = useGetHistory(id ? Number(id) : undefined);


    return (
        <div className="history-page">
            <HistoryContent sessions={sessions} onRefetch={refetch}/>
        </div>
    )
};
