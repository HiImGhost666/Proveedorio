# Stage 1: Install dependencies
FROM node:18-alpine AS deps
WORKDIR /app
COPY package.json ./
RUN npm install

# Stage 2: Run the mock servers
FROM node:18-alpine
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY api1.json api1.json
COPY api2.json api2.json
COPY api3.json api3.json
COPY api4.json api4.json

# curl is needed for the Docker healthcheck
RUN apk add --no-cache curl

# Make locally installed binaries (json-server, concurrently) available in PATH
ENV PATH=/app/node_modules/.bin:$PATH

EXPOSE 3001 3002 3003 3004

# json-server v1.x: file argument first, --port flag, --watch removed (watches by default)
CMD ["concurrently", "json-server api1.json --port 3001", "json-server api2.json --port 3002", "json-server api3.json --port 3003", "json-server api4.json --port 3004"]
