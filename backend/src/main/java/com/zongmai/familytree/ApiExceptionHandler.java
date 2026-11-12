package com.zongmai.familytree;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, Object>> integrity(
      org.springframework.dao.DataIntegrityViolationException exception) {
    return ResponseEntity.status(409).body(Map.of("status", 409, "message", "数据存在重复或关联冲突，请刷新后检查"));
  }

  @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
  public ResponseEntity<Map<String, Object>> uploadTooLarge(Exception exception) {
    return ResponseEntity.status(413).body(Map.of("status", 413, "message", "文件不能超过8MB"));
  }

  @ExceptionHandler({
    org.springframework.web.bind.MissingServletRequestParameterException.class,
    org.springframework.web.multipart.support.MissingServletRequestPartException.class
  })
  public ResponseEntity<Map<String, Object>> missingParameter(Exception exception) {
    return ResponseEntity.badRequest().body(Map.of("status", 400, "message", "缺少必填参数或文件"));
  }

  @ExceptionHandler(org.springframework.dao.DataAccessException.class)
  public ResponseEntity<Map<String, Object>> databaseFailure(
      org.springframework.dao.DataAccessException exception) {
    org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class).error("数据库访问失败", exception);
    return ResponseEntity.status(500).body(Map.of("status", 500, "message", "数据库访问失败，请查看服务端日志"));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Map<String, Object>> handleParameterTypeMismatch(
      MethodArgumentTypeMismatchException exception) {

    String message = "参数" + exception.getName() + " 格式错误，请检查参数类型";

    Map<String, Object> body = Map.of("status", 400, "message", message);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatusException(
      ResponseStatusException exception) {
    String message = exception.getReason();

    if (message == null) {
      message = "请求处理失败";
    }

    Map<String, Object> body =
        Map.of("status", exception.getStatusCode().value(), "message", message);

    return ResponseEntity.status(exception.getStatusCode()).body(body);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(
      MethodArgumentNotValidException exception) {

    String message = "请求参数不符合要求";

    FieldError error = exception.getBindingResult().getFieldError();
    if (error != null && error.getDefaultMessage() != null) {
      message = error.getDefaultMessage();
    }

    Map<String, Object> body = Map.of("status", 400, "message", message);

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, Object>> handleUnreadableMessage(
      HttpMessageNotReadableException exception) {

    Map<String, Object> body = Map.of("status", 400, "message", "请求体格式错误，请检查JSON格式和字段类型");

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException exception) {

    String message = "该接口不支持 " + exception.getMethod() + " 请求，请检查请求方式";

    Map<String, Object> body = Map.of("status", 405, "message", message);

    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .headers(exception.getHeaders())
        .body(body);
  }
}
