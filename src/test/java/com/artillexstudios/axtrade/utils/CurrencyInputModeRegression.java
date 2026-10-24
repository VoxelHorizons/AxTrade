package com.artillexstudios.axtrade.utils;

public final class CurrencyInputModeRegression {
    public static void main(String[] args) {
        check(CurrencyInputMode.useDialog("auto", true));
        check(CurrencyInputMode.useDialog("dialog", true));
        check(CurrencyInputMode.useDialog(" DIALOG ", true));
        check(CurrencyInputMode.useDialog(null, true));
        check(!CurrencyInputMode.useDialog("sign", true));
        check(!CurrencyInputMode.useDialog("SIGN", true));
        check(!CurrencyInputMode.useDialog("auto", false));
        check(!CurrencyInputMode.useDialog("dialog", false));
        check(!CurrencyInputMode.useDialog("sign", false));
        System.out.println("Currency input backend selection passed.");
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("Wrong dialog/sign selection");
    }
}
