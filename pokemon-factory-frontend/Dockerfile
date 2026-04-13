FROM node:20-alpine AS build

ARG VITE_API_BASE=http://localhost:8081/api/pokedex
ARG VITE_DAMAGE_API_BASE=http://localhost:8090/api/damage
ARG VITE_SPRITES_BASE=http://localhost:8081

WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

ENV VITE_API_BASE=${VITE_API_BASE}
ENV VITE_DAMAGE_API_BASE=${VITE_DAMAGE_API_BASE}
ENV VITE_SPRITES_BASE=${VITE_SPRITES_BASE}

RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80