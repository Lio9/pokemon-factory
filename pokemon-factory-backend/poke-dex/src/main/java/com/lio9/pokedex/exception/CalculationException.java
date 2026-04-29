package com.lio9.pokedex.exception;



/**
 * 计算异常
 * 当伤害计算等计算过程中发生错误时抛出
 *
 * @author Lio9
 * @version 1.0
 */
public class CalculationException extends BusinessException {

    public CalculationException(String message) {
        super("CALCULATION_ERROR", message);
    }

    public CalculationException(String message, Throwable cause) {
        super("CALCULATION_ERROR", message, cause);
    }
}