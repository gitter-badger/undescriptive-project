package com.github.javadev.undescriptive.protocol.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ErrorResponse {
    @JsonProperty private final ErrorResponseItem error;

    public ErrorResponse(@JsonProperty("error") final ErrorResponseItem error) {
        this.error = error;
    }

    public ErrorResponseItem getError() {
        return error;
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
            "error=" + error +
            '}';
    }
}
