package com.zongmai.familytree;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final UserService userService;
  private final SecurityContextRepository contexts;
  private final CsrfTokenRepository csrfTokens;
  private final SystemAccess systemAccess;

  public AuthController(
      UserService userService,
      SecurityContextRepository contexts,
      CsrfTokenRepository csrfTokens,
      SystemAccess systemAccess) {
    this.userService = userService;
    this.contexts = contexts;
    this.csrfTokens = csrfTokens;
    this.systemAccess = systemAccess;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public Map<String, String> register(@Valid @RequestBody RegisterRequest request) {
    userService.register(request);

    return Map.of("message", "注册成功");
  }

  @PostMapping("/login")
  public Map<String, Object> login(
      @Valid @RequestBody LoginRequest request,
      HttpServletRequest httpRequest,
      HttpServletResponse httpResponse) {

    Map<String, Object> user = userService.authenticate(request);

    LoginUser loginUser =
        new LoginUser(((Number) user.get("id")).longValue(), (String) user.get("username"));

    if (httpRequest.getSession(false) != null) {
      httpRequest.changeSessionId();
    }

    SecurityContext context = SecurityContextHolder.createEmptyContext();

    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(loginUser, null, List.of()));

    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, httpRequest, httpResponse);

    csrfTokens.saveToken(null, httpRequest, httpResponse);

    return Map.of("message", "登录成功", "user", loginUser);
  }

  @GetMapping("/me")
  @org.springframework.security.access.prepost.PreAuthorize(
      "@familyAccess.isActive(authentication)")
  public Map<String, Object> me(@AuthenticationPrincipal LoginUser user) {
    return Map.of(
        "id", user.id(), "username", user.username(), "systemAdmin", systemAccess.allowed(user));
  }
}
