package handler

import (
	"net/http"
)

func RegisterUserRoutes(mux *http.ServeMux) {
	mux.HandleFunc("POST /api/v1/users/login", handleSuccess)
	mux.HandleFunc("POST /api/v1/users/register", handleSuccess)
	mux.HandleFunc("POST /api/v1/users/oauth/google", handleSuccess)
	
	mux.HandleFunc("GET /api/v1/users/profile", handleSuccess)
	mux.HandleFunc("PUT /api/v1/users/profile", handleSuccess)
	mux.HandleFunc("PATCH /api/v1/users/profile", handleSuccess)
	
	mux.HandleFunc("GET /api/v1/users/application/options", handleSuccess)
	mux.HandleFunc("PUT /api/v1/users/application/options", handleSuccess)
	mux.HandleFunc("PATCH /api/v1/users/application/options", handleSuccess)
}
