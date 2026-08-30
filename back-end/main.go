package main

import (
	"log"
	"net/http"

	"nahidka.com/internal/handler"
)

func main() {
	serveMux := http.NewServeMux()

	handler.RegisterUserRoutes(serveMux)
	handler.RegisterFeatureRoutes(serveMux)

	log.Println("Server is running on port 8080")
	err := http.ListenAndServe(":8080", serveMux)
	if err != nil {
		log.Fatalf("Server failed to start: %v", err)
	}
}
