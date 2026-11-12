package com.zongmai.familytree;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AncestorRequest(
          @JsonDeserialize(using = StrictLongDeserializer.class)
          @NotNull(message = "始祖成员编号不能为空")
          @Positive(message = "始祖成员编号必须大于0")
          Long personId) {
            
}