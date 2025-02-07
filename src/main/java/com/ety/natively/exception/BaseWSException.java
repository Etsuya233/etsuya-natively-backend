package com.ety.natively.exception;

import com.ety.natively.enums.ExceptionEnum;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class BaseWSException extends BaseException {
    private String destination;

    public BaseWSException(ExceptionEnum exceptionEnum, String destination) {
        super(exceptionEnum);
        this.destination = destination;
    }

    public BaseWSException(ExceptionEnum exceptionEnum, String destination, Object... exceptionArgs) {
        super(exceptionEnum, exceptionArgs);
        this.destination = destination;
    }
}
