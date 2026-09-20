package com.recallhub.common;

import java.util.List;

public record CursorPage<T>(List<T> items, String nextCursor, boolean hasMore) {}

