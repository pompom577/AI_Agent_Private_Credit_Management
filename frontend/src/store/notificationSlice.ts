import { create } from 'zustand';

// Defines the payload format coming from the Spring Boot Gateway broker
export interface ExtractionNotification {
  doc_id: number;
  filename: string;
  status: string;
}

interface NotificationState {
  notifications: ExtractionNotification[];
  addNotification: (notification: ExtractionNotification) => void;
  dismissNotification: (doc_id: number) => void;
}

export const useNotificationStore = create<NotificationState>((set) => ({
  notifications: [],

  addNotification: (notification) =>
    set((state) => ({
      // Prevents multiple duplicate banner cards for the same document ID
      notifications: state.notifications.some(
        (n) => n.doc_id === notification.doc_id,
      )
        ? state.notifications
        : [...state.notifications, notification],
    })),

  dismissNotification: (doc_id) =>
    set((state) => ({
      notifications: state.notifications.filter((n) => n.doc_id !== doc_id),
    })),
}));
