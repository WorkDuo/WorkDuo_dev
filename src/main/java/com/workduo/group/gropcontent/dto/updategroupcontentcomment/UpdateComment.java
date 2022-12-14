package com.workduo.group.gropcontent.dto.updategroupcontentcomment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

public class UpdateComment {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Request {
        @NotNull(message = "내용은 필수 입력 사항입니다.")
        @Schema(example = "HolyWak, Is there hell ?",description = "댓글 업데이트")
        private String comment;
    }
}
