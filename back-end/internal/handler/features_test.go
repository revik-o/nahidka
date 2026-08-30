package handler

import (
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestFeatureRoutes(t *testing.T) {
	serveMux := http.NewServeMux()
	RegisterFeatureRoutes(serveMux)

	testCases := []struct {
		method string
		path   string
	}{
		{"GET", "/api/v1/features/dashboard"},
		{"GET", "/api/v1/features/tasks"},
		{"POST", "/api/v1/features/tasks"},
		{"PATCH", "/api/v1/features/tasks"},
		{"GET", "/api/v1/features/social/battery"},
		{"PATCH", "/api/v1/features/social/battery"},
		{"GET", "/api/v1/features/goals"},
		{"POST", "/api/v1/features/goals"},
		{"PATCH", "/api/v1/features/goals"},
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
