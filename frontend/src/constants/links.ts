/**
 * Enlaces externos utilizados por la aplicación (por ejemplo, Grafana).
 * Los valores se leen de variables de entorno para no hardcodear URLs sensibles.
 *
 * Para configurar Grafana:
 * - Definir VITE_GRAFANA_DASHBOARD_URL en el entorno de build (ej. .env.local).
 * - No commitear el valor real al repositorio (cumplimiento ISO 27001/seguridad).
 */
export const GRAFANA_DASHBOARD_URL: string | undefined =
  import.meta.env.VITE_GRAFANA_DASHBOARD_URL || undefined;

export const PROMETHEUS_DASHBOARD_URL: string | undefined =
  import.meta.env.VITE_PROMETHEUS_DASHBOARD_URL || undefined;

