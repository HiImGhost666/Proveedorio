import { useEffect, useMemo, useState, ReactNode } from 'react';
import {
    fetchNotificationCounts,
    fetchNotifications,
    markAllNotificationsAsRead,
    markNotificationAsRead,
    NotificationCounts,
    NotificationItem,
    NotificationTab,
} from '../services/notificationService';
import { ErrorContext } from './ErrorContextStore';

type NotificationsByTab = Record<NotificationTab, NotificationItem[]>;

export interface ErrorContextType {
    notificationsByTab: NotificationsByTab;
    counts: NotificationCounts;
    unreadCount: number;
    isLoading: boolean;
    loadTab: (tab: NotificationTab) => Promise<void>;
    refreshCounts: () => Promise<void>;
    markAsRead: (id: number) => Promise<void>;
    markAllAsRead: (tab: NotificationTab) => Promise<void>;
}

const POLL_INTERVAL_MS = 30000;

const EMPTY_COUNTS: NotificationCounts = {
    general: 0,
    errores: 0,
    ofertas: 0,
    nuevoProducto: 0,
};

const EMPTY_NOTIFICATIONS: NotificationsByTab = {
    general: [],
    errores: [],
    ofertas: [],
    'nuevo-producto': [],
};

export const ErrorProvider = ({ children }: { children: ReactNode }) => {
    const [notificationsByTab, setNotificationsByTab] = useState<NotificationsByTab>(EMPTY_NOTIFICATIONS);
    const [counts, setCounts] = useState<NotificationCounts>(EMPTY_COUNTS);
    const [isLoading, setIsLoading] = useState(false);

    const refreshCounts = async () => {
        try {
            const nextCounts = await fetchNotificationCounts();
            setCounts(nextCounts);
        } catch {
            // If network is unavailable, keep previous state and avoid crashing the app.
        }
    };

    const loadTab = async (tab: NotificationTab) => {
        setIsLoading(true);
        try {
            const items = await fetchNotifications(tab, 50);
            setNotificationsByTab((prev) => ({
                ...prev,
                [tab]: items,
            }));
        } catch {
            // Keep previous tab content on transient API/network failures.
        } finally {
            setIsLoading(false);
        }
    };

    const markAsRead = async (id: number) => {
        try {
            await markNotificationAsRead(id);
        } catch {
            return;
        }
        setNotificationsByTab((prev) => ({
            general: prev.general.map((n) => (n.id === id ? { ...n, read: true } : n)),
            errores: prev.errores.map((n) => (n.id === id ? { ...n, read: true } : n)),
            ofertas: prev.ofertas.map((n) => (n.id === id ? { ...n, read: true } : n)),
            'nuevo-producto': prev['nuevo-producto'].map((n) => (n.id === id ? { ...n, read: true } : n)),
        }));
        await refreshCounts();
    };

    const markAllAsRead = async (tab: NotificationTab) => {
        try {
            await markAllNotificationsAsRead(tab);
        } catch {
            return;
        }

        setNotificationsByTab((prev) => {
            if (tab === 'general') {
                return {
                    general: prev.general.map((n) => ({ ...n, read: true })),
                    errores: prev.errores.map((n) => ({ ...n, read: true })),
                    ofertas: prev.ofertas.map((n) => ({ ...n, read: true })),
                    'nuevo-producto': prev['nuevo-producto'].map((n) => ({ ...n, read: true })),
                };
            }

            return {
                ...prev,
                [tab]: prev[tab].map((n) => ({ ...n, read: true })),
            };
        });

        await refreshCounts();
    };

    useEffect(() => {
        const token = localStorage.getItem('token');
        if (!token) {
            setCounts(EMPTY_COUNTS);
            setNotificationsByTab(EMPTY_NOTIFICATIONS);
            return;
        }

        const bootstrap = async () => {
            await Promise.all([refreshCounts(), loadTab('general')]);
        };

        void bootstrap();

        const interval = window.setInterval(() => {
            void Promise.all([refreshCounts(), loadTab('general')]);
        }, POLL_INTERVAL_MS);

        return () => window.clearInterval(interval);
    }, []);

    const unreadCount = useMemo(() => counts.general, [counts.general]);

    return (
        <ErrorContext.Provider
            value={{
                notificationsByTab,
                counts,
                unreadCount,
                isLoading,
                loadTab,
                refreshCounts,
                markAsRead,
                markAllAsRead,
            }}
        >
            {children}
        </ErrorContext.Provider>
    );
};

