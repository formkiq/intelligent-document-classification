FROM node:19.8.1-alpine3.17 as builder

ARG SERVER_NAME

WORKDIR /app

RUN apk add gettext
COPY package*.json ./
RUN npm install

COPY nginx.conf ./
RUN SERVER_NAME=$SERVER_NAME uri=\$uri host=\$host request_uri=\$request_uri envsubst < nginx.conf > /app/default.conf
COPY src/ ./src
COPY public/ ./public

RUN npm run build

COPY localhost.conf ./

FROM nginx:1.23

COPY --from=builder /app/build/ /usr/share/nginx/html

# Copy the default nginx.conf
COPY --from=builder /app/default.conf /etc/nginx/conf.d/default.conf
