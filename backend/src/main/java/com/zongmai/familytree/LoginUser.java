package com.zongmai.familytree;

import java.io.Serializable;

public record LoginUser(Long id, String username) implements Serializable {
}