package com.zongmai.familytree;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.CodePointLength;

public record RegisterRequest(
        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "用户名不能为空")
        @Pattern(
                regexp = "[A-Za-z0-9_]{3,32}",
                message = "用户名必须为3到32位英文字母，数字或下划线")
        String username,

        @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "密码不能为空")
        @CodePointLength(
                min = 6,
                max = 64,
                message = "密码长度必须为6到64个字符")
        String password){
}