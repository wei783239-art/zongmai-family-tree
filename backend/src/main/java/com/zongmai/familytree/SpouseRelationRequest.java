package com.zongmai.familytree;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SpouseRelationRequest(
         @JsonDeserialize(using = StrictLongDeserializer.class)
         @NotNull(message = "成员编号不能为空")
         @Positive(message = "成员编号必须大于0")
         Long personId,

         @JsonDeserialize(using = StrictLongDeserializer.class) 
         @NotNull(message = "配偶成员编号不能为空")
         @Positive(message = "配偶成员编号必须大于0")
         Long spousePersonId) {
}