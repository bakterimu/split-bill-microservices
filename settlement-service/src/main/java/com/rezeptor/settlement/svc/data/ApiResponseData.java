package com.rezeptor.settlement.svc.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponseData<T> {
    private boolean success;
    private String message;
    private T data;
    private Integer statusCode;
}
