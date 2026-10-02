FROM golang:1.23-alpine AS builder

WORKDIR /app
RUN apk add --no-cache gcc musl-dev

COPY go.mod ./
COPY go.sum* ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=1 GOOS=linux go build -a -o bin/server main.go

FROM alpine:latest
WORKDIR /app

RUN apk add --no-cache sqlite-libs

COPY --from=builder /app/bin/server .

VOLUME /app/data

EXPOSE 8080
CMD ["./server"]
