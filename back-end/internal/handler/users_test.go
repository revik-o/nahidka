package handler

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestUserRoutes(t *testing.T) {
	serveMux := http.NewServeMux()
	RegisterUserRoutes(serveMux)

	testCases := []struct {
		method string
		path   string
	}{
		{"POST", "/api/v1/users/login"},
		{"POST", "/api/v1/users/register"},
		{"POST", "/api/v1/users/oauth/google"},
		{"GET", "/api/v1/users/profile"},
		{"PUT", "/api/v1/users/profile"},
		{"PATCH", "/api/v1/users/profile"},
		{"GET", "/api/v1/users/application/options"},
		{"PUT", "/api/v1/users/application/options"},
		{"PATCH", "/api/v1/users/application/options"},
	}

	for _, testCase := range testCases {
		t.Run(testCase.method+" "+testCase.path, func(t *testing.T) {
			request, err := http.NewRequest(testCase.method, testCase.path, nil)
			if err != nil {
				t.Fatalf("Failed to create request: %v", err)
			}

			responseRecorder := httptest.NewRecorder()
			serveMux.ServeHTTP(responseRecorder, request)

			if responseRecorder.Code != http.StatusOK {
				t.Errorf("Expected status OK (200), got %v", responseRecorder.Code)
			}
		})
	}
}
