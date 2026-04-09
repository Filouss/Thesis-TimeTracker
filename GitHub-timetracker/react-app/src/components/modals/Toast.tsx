import { useEffect } from "react";
import "../../styles/Toast.css";

type ToastProps = {
    message: string;
    isVisible: boolean;
    onClose: () => void;
}

export default function Toast({ message, isVisible, onClose } : ToastProps) {
    
    useEffect(() => {
        if (isVisible) {
            const timer = setTimeout(() => {
                onClose(); 
            }, 3000);
            
            return () => clearTimeout(timer);
        }
    }, [isVisible, onClose]);

    if (!isVisible) return null;

    return (
        <div className="toast-container">
            <svg 
                className="toast-icon" 
                fill="none" 
                stroke="currentColor" 
                viewBox="0 0 24 24" 
                xmlns="http://www.w3.org/2000/svg"
            >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
            <span className="toast-message">{message}</span>
        </div>
    );
}