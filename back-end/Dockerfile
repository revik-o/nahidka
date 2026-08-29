FROM golang:1.23-alpine AS builder

WORKDIR /app
# Install gcc for SQLite CGO dependencies
RUN apk add --no-cache gcc musl-dev

COPY go.mod ./
# Only copy go.sum if it exists, otherwise ignore error
COPY go.sum* ./
RUN go mod download

COPY . .
# Build with CGO_ENABLED=1 for SQLite
RUN CGO_ENABLED=1 GOOS=linux go build -a -o bin/server main.go

FROM alpine:latest
WORKDIR /app

# Add dependencies for running CGO binaries
RUN apk add --no-cache sqlite-libs

COPY --from=builder /app/bin/server .

# Volume for SQLite database
VOLUME /app/data

EXPOSE 8080
CMD ["./server"]
