import api from 'services/api';

export type NotificationType = 'ERROR' | 'OFERTA' | 'NUEVO_PRODUCTO' | 'GENERAL';
export type NotificationTab = 'general' | 'errores' | 'ofertas' | 'nuevo-producto';

export interface NotificationItem {
  id: number;
  type: NotificationType;
  title: string;
  message: string;
  supplierId: number | null;
  supplierName: string | null;
  productId: number | null;
  productLabel: string | null;
  actionPath: string | null;
  scopeTag: string | null;
  createdAt: string;
  read: boolean;
}

export interface NotificationCounts {
  general: number;
  errores: number;
  ofertas: number;
  nuevoProducto: number;
}

interface ApiEnvelope<T> {
  success: boolean;
  message: string;
  data: T;
}

export const fetchNotifications = async (tab: NotificationTab, limit = 50): Promise<NotificationItem[]> => {
  const { data } = await api.get<ApiEnvelope<NotificationItem[]>>('/notificaciones', {
    params: { tab, limit },
  });
  return data.data;
};

export const fetchNotificationCounts = async (): Promise<NotificationCounts> => {
  const { data } = await api.get<ApiEnvelope<NotificationCounts>>('/notificaciones/counts');
  return data.data;
};

export const markNotificationAsRead = async (notificationId: number): Promise<void> => {
  await api.put(`/notificaciones/${notificationId}/read`);
};

export const markAllNotificationsAsRead = async (tab: NotificationTab): Promise<void> => {
  await api.put('/notificaciones/read-all', null, { params: { tab } });
};
