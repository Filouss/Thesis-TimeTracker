import React, { createContext, useContext, useState, useEffect } from 'react';
import Toast from '../components/modals/Toast';

type ToastType = "success" | "error";

interface ToastContextType {
    showToast: (message: string, type?: ToastType) => void;
}

const ToastContext = createContext<ToastContextType | undefined>(undefined);

export const ToastProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
    const [isVisible, setIsVisible] = useState(false);
    const [message, setMessage] = useState('');
    const [type, setType] = useState<ToastType>('success');

    const showToast = (newMessage: string, newType: ToastType = 'success') => {
        setMessage(newMessage);
        setType(newType);
        setIsVisible(true);
    };

    // catch unhandled errors globally and show toast
    useEffect(() => {
        const handleUnhandledRejection = () => {
            showToast("An error occurred during performing this action", "error");
        };

        
        window.addEventListener('unhandledrejection', handleUnhandledRejection);

        return () => {
            window.removeEventListener('unhandledrejection', handleUnhandledRejection);
        };
    }, []);

    return (
        <ToastContext.Provider value={{ showToast }}>
            {children}
            <Toast 
                message={message} 
                isVisible={isVisible} 
                type={type} 
                onClose={() => setIsVisible(false)} 
            />
        </ToastContext.Provider>
    );
};

export const useToast = () => {
    const context = useContext(ToastContext);
    if (!context) throw new Error("useToast must be used within ToastProvider");
    return context;
};