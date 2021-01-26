package com.datapath.integration.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ValidationUtils {

    private static final int IDENTIFIER_ID_MAX_LENGTH = 1000;
    private static final int IDENTIFIER_SCHEME_MAX_LENGTH = 255;
    private static final int IDENTIFIER_LEGAL_NAME_MAX_LENGTH = 2000;
    private static final int EMAIL_MAX_LENGTH = 255;
    private static final int TELEPHONE_MAX_LENGTH = 2500;

    public static boolean validateIdentifierId(String identifierId) {
        return validateLength(identifierId, IDENTIFIER_ID_MAX_LENGTH);
    }

    public static boolean validateIdentifierScheme(String identifierScheme) {
        return validateLength(identifierScheme, IDENTIFIER_SCHEME_MAX_LENGTH);
    }

    public static boolean validateIdentifierLegalName(String identifierLegalName) {
        return validateLength(identifierLegalName, IDENTIFIER_LEGAL_NAME_MAX_LENGTH);
    }

    public static boolean validateEmail(String email) {
        return validateLength(email, EMAIL_MAX_LENGTH);
    }

    public static boolean validateTelephone(String telephone) {
        return validateLength(telephone, TELEPHONE_MAX_LENGTH);
    }

    private static boolean validateLength(String str, int maxLength) {
        if (null == str) {
            return true;
        }

        if (str.length() > maxLength) {
            log.warn("Value too long: {}. Max length: {}", str, maxLength);
            return false;
        }

        return true;
    }

}
