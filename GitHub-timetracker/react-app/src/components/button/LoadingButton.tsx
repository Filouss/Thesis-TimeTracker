import React, { type ButtonHTMLAttributes } from 'react';

export type LoadingButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
    isLoading: boolean;
    loadingText?: React.ReactNode;
    children: React.ReactNode;
};

export default function LoadingButton({ 
    isLoading, 
    loadingText = "Loading...", 
    children, 
    disabled, 
    style,
    ...props 
}: LoadingButtonProps) {
    return (
        <button 
            {...props} 
            disabled={isLoading || disabled}
            style={{ 
                opacity: isLoading ? 0.7 : 1,
                cursor: (isLoading || disabled) ? 'not-allowed' : 'pointer',
                ...style
            }}
        >
            {isLoading ? (
                <>
                    <span>{loadingText}</span>
                </>
            ) : (
                children
            )}
        </button>
    );
}