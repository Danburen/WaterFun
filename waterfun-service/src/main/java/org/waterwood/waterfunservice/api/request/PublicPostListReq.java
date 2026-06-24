package org.waterwood.waterfunservice.api.request;

import lombok.Data;

import java.util.List;

@Data
public class PublicPostListReq {
    private Integer categoryId;
    private List<Integer> tagIds;
    private int page = 1;
    private int size = 10;
}
