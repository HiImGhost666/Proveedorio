# Stage 1: Build the React application
FROM node:18-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
ARG VITE_GRAFANA_DASHBOARD_URL
ENV VITE_GRAFANA_DASHBOARD_URL=$VITE_GRAFANA_DASHBOARD_URL
ARG VITE_PROMETHEUS_DASHBOARD_URL
ENV VITE_PROMETHEUS_DASHBOARD_URL=$VITE_PROMETHEUS_DASHBOARD_URL
RUN npm run build

# Stage 2: Serve the application with Nginx
FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY frontend/nginx/default.conf /etc/nginx/conf.d/default.conf
CMD ["nginx", "-g", "daemon off;"]
