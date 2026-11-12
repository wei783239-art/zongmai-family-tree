package com.zongmai.familytree;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ParentChildRelationRequset(
         @JsonDeserialize(using = StrictLongDeserializer.class)
         @NotNull(message = "必须提供父母编号")
         @Positive(message = "父母编号必须大于0")
         Long parentPersonId,
         
         @JsonDeserialize(using = StrictLongDeserializer.class)
         @NotNull(message = "必须提供子女编号")
         @Positive(message = "子女编号必须大于0")
         Long childPersonId) {

}