package com.agent_chat.agent_chat.Exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

  //Xử lý ngoại lệ RuntimeException
  @ExceptionHandler(value =  RuntimeException.class)
  ResponseEntity<String> handleRunTimeException(RuntimeException ex) {
    return ResponseEntity.badRequest().body(ex.getMessage());
  }
  
}