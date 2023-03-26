FROM node:19.8.1-alpine3.17 as builder

WORKDIR /app

COPY package*.json ./
RUN npm install

COPY nginx.conf ./
COPY src/ ./src
COPY public/ ./public

RUN npm run build

COPY localhost.conf ./

RUN apk add openssl
RUN openssl req -x509 -nodes -days 1000 -newkey rsa:2048 -keyout localhost.key -out localhost.crt -batch

FROM nginx:1.23

COPY --from=builder /app/build/ /usr/share/nginx/html

# Copy the default nginx.conf
COPY --from=builder /app/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/localhost.crt /etc/ssl/certs/localhost.crt
COPY --from=builder /app/localhost.key /etc/ssl/private/localhost.key