package handler

import "net/http"

func RegisterFeatureRoutes(mux *http.ServeMux) {
	mux.HandleFunc("GET /api/v1/features/dashboard", handleSuccess)
	
	mux.HandleFunc("GET /api/v1/features/tasks", handleSuccess)
	mux.HandleFunc("POST /api/v1/features/tasks", handleSuccess)
	mux.HandleFunc("PATCH /api/v1/features/tasks", handleSuccess)
	
	mux.HandleFunc("GET /api/v1/features/social/battery", handleSuccess)
	mux.HandleFunc("PATCH /api/v1/features/social/battery", handleSuccess)
	
	mux.HandleFunc("GET /api/v1/features/goals", handleSuccess)
	mux.HandleFunc("POST /api/v1/features/goals", handleSuccess)
	mux.HandleFunc("PATCH /api/v1/features/goals", handleSuccess)
}
