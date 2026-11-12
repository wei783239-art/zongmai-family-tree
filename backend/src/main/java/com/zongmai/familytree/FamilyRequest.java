package com.zongmai.familytree;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record FamilyRequest(
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "家族名称不能为空")
        @CodePointLength(max = 120, message = "家族名称不能超过120个字符")
        String name,
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @CodePointLength(max = 64, message = "姓氏不能超过64个字符")
        String surname,
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @CodePointLength(max = 120, message = "堂号不能超过120个字符")
        String ancestral_hall,
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @CodePointLength(max = 160, message = "籍贯不能超过160个字符")
        String hometown,
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @CodePointLength(max = 20000, message = "家族简介不能超过20000字")
        String description,
    @CodePointLength(max = 2000, message = "家训不能超过2000字") String motto,
    Boolean published) {}
