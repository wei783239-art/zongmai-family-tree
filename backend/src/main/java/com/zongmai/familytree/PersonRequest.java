package com.zongmai.familytree;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.hibernate.validator.constraints.CodePointLength;

public record PersonRequest(
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @NotBlank(message = "成员姓名不能为空")
        @CodePointLength(max = 120, message = "成员姓名不能超过120个字符")
        String name,
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @Pattern(regexp = "MALE|FEMALE|UNKNOWN", message = "性别必须是MALE、FEMALE或UNKNOWN")
        String gender,
    @JsonDeserialize(using = StrictIntegerDeserializer.class) @Positive(message = "世代必须大于0")
        Integer generation,
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @CodePointLength(max = 160, message = "出生地不能超过160个字符")
        String birth_place,
    @JsonDeserialize(using = StrictStringDeserializer.class)
        @CodePointLength(max = 20000, message = "人物简介不能超过20000字")
        String biography,
    @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE) LocalDate birth_date,
    @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE) LocalDate death_date,
    @CodePointLength(max = 80, message = "字辈不能超过80字") String nickname,
    @CodePointLength(max = 160, message = "籍贯不能超过160字") String hometown,
    @Pattern(regexp = "PUBLIC|FAMILY|PRIVATE", message = "可见范围无效") String visibility,
    @CodePointLength(max = 500, message = "头像地址过长") String avatar_url) {}
