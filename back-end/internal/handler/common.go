package handler

import "net/http"

func handleSuccess(writer http.ResponseWriter, request *http.Request) {
	writer.WriteHeader(http.StatusOK)
}
