package com.solutio.api.global.request;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.Getter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Getter
public class BasePageRequest {

    @Parameter(description = "페이지 번호 (0번 부터 시작)")
    private final Integer page;

    @Parameter(description = "페이지 크기")
    private final Integer size;

    public BasePageRequest(Integer page, Integer size) {
        this.page = page != null ? page : 0;
        this.size = size != null ? size : 10;
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }

    public Pageable toPageable(Sort sort) {
        return PageRequest.of(page, size, sort);
    }
}
