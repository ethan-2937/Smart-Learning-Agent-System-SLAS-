package com.hlju.learning.common;

import java.time.Instant;

public record ApiError(String message, String path, Instant timestamp) {
}
