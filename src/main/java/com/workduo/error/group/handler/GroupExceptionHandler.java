package com.workduo.error.group.handler;

import com.workduo.error.group.exception.GroupException;
import com.workduo.error.group.result.GroupErrorResult;
import com.workduo.error.group.type.GroupErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(2)
public class GroupExceptionHandler {

    @ExceptionHandler(GroupException.class)
    public ResponseEntity<GroupErrorResult> groupException(GroupException e) {
        log.error("error ", e);
        GroupErrorCode groupErrorCode = e.getErrorCode();
        GroupErrorResult result = GroupErrorResult.of(e.getErrorCode());

        return new ResponseEntity<>(result, groupErrorCode.getHttpStatus());
    }
}
