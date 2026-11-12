package com.zongmai.familytree;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.CodePointLength;

public record LoginRequest(
        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "用户名不能为空")
        @CodePointLength(
                max = 32,
                message = "用户名不能超过32个字符")
        String username,

        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "密码不能为空")
        @CodePointLength(
                max = 64,
                message = "密码不能超过64个字符")
        String password) {
}