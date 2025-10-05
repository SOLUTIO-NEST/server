package com.solutio.api.global.response;
import java.time.LocalDateTime;
import java.util.List;

public class CursorPageResponse<T> {

    private final List<T> contents;
    private final LocalDateTime nextCursor;
    private final boolean hasNext;

    public CursorPageResponse(List<T> contents, LocalDateTime nextCursor, boolean hasNext) {
        this.contents = contents;
        this.nextCursor = nextCursor;
        this.hasNext = hasNext;
    }

    public List<T> getContents() {
        return contents;
    }

    public LocalDateTime getNextCursor() {
        return nextCursor;
    }

    public boolean isHasNext() {
        return hasNext;
    }
}

