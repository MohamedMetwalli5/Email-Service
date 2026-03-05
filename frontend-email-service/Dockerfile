# ---- Stage 1: Build ----
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .

# Declare build args
ARG VITE_BACKEND_API_URL
ARG VITE_CLIENT_ID
ARG VITE_DISCORD_REDIRECT_URI
ARG VITE_TERMS_OF_USE_URL
ARG VITE_PRIVACY_POLICY_URL

# Make them available to Vite during build
ENV VITE_BACKEND_API_URL=$VITE_BACKEND_API_URL
ENV VITE_CLIENT_ID=$VITE_CLIENT_ID
ENV VITE_DISCORD_REDIRECT_URI=$VITE_DISCORD_REDIRECT_URI
ENV VITE_TERMS_OF_USE_URL=$VITE_TERMS_OF_USE_URL
ENV VITE_PRIVACY_POLICY_URL=$VITE_PRIVACY_POLICY_URL

RUN npm run build

# ---- Stage 2: Serve with Nginx ----
FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]