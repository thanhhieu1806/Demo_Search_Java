package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record TmsraSearchRequest(
        String language,
        @JsonAlias({"taxcode", "tax_code"}) String taxCode,
        String pid,
        String budgetCode,
        String decision,
        @JsonAlias({"cccd", "citizenID", "citizenIDNo"}) String citizenId,
        @JsonAlias({"soCccd", "soCCCD"}) String cccd,
        String passport,
        String socialInsuranceCode,
        String unitCode,
        String personalTaxCode,
        String personalSocialInsuranceCode,
        @JsonAlias({"certificateSn", "serialNumber", "certificateSerialNumber"}) String certificateSN,
        String certificateStateCode,
        String activationCode,
        String expandFutureParamXML
) {
    public static TmsraSearchRequest empty() {
        return new TmsraSearchRequest(
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null
        );
    }
}