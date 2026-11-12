package com.zongmai.familytree;

import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class DemoController {
  private final DemoDataService demo;

  public DemoController(DemoDataService demo) {
    this.demo = demo;
  }

  @PostMapping("/api/demo-families")
  @PreAuthorize("@familyAccess.isActive(authentication)")
  public Map<String, Object> create(@AuthenticationPrincipal LoginUser user) {
    return Map.of("id", demo.create(user.id()));
  }
}
