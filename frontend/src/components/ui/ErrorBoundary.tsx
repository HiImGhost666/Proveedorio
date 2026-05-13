import { Component, ErrorInfo, ReactNode } from 'react';
import { RotateCcw, TriangleAlert } from 'lucide-react';

interface Props {
    children?: ReactNode;
    fallbackMessage?: string;
    onRetry?: () => void;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

class ErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false,
        error: null
    };

    public static getDerivedStateFromError(error: Error): State {
        // Update state so the next render will show the fallback UI.
        return { hasError: true, error };
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        console.error("Uncaught error:", error, errorInfo);
    }

    resetError = () => {
        this.setState({ hasError: false, error: null });
        if (this.props.onRetry) {
            this.props.onRetry();
        }
    };

    public render() {
        if (this.state.hasError) {
            return (
                <div className="error-boundary-box">
                    <h3 style={{ margin: "0 0 10px 0", fontSize: "1.1rem", display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                        <TriangleAlert size={18} aria-hidden="true" />
                        {this.props.fallbackMessage || "Ocurrió un error inesperado al cargar esta sección."}
                    </h3>
                    <p style={{ margin: "0 0 1rem 0", fontSize: "0.875rem", opacity: 0.8 }}>
                        {this.state.error?.message}
                    </p>
                    {this.props.onRetry && (
                        <button
                            onClick={this.resetError}
                            className="btn btn-secondary"
                            style={{ fontSize: "0.75rem", padding: "0.3rem 0.6rem", display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}
                        >
                            <RotateCcw size={14} aria-hidden="true" />
                            Reintentar
                        </button>
                    )}
                </div>
            );
        }

        return this.props.children;
    }
}

export default ErrorBoundary;
