package com.example.demo.service;

import com.example.demo.config.TmsraProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.net.ssl.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Map;

@Service
public class TmsraService {

    private static final Logger log = LoggerFactory.getLogger(TmsraService.class);

    private final TmsraProperties props;
    private final ObjectMapper objectMapper;

    public TmsraService(TmsraProperties props) {
        this.props = props;
        this.objectMapper = new ObjectMapper();
        if (props.isInsecureSsl()) {
            disableSslVerification();
        }
    }

    // ─── Login: lấy accessToken ─────────────────────────────────────────────────

    /**
     * Gọi API getAccessTokenForTMSRA.
     * TMSRA yêu cầu field "userName" và "passWord" (chữ hoa N và W).
     */
    public Map<String, Object> getAccessTokenInfo(String username, String password) {
        String url = props.getBaseUrl() + "/getAccessTokenForTMSRA";

        Map<String, String> payload = Map.of(
                "userName", username != null ? username : props.getUsername(),
                "passWord", password  != null ? password  : props.getPassword()
        );

        log.info("Calling TMSRA getAccessTokenForTMSRA for user: {}", username);

        try {
            String requestBody  = objectMapper.writeValueAsString(payload);
            String responseBody = doPost(url, requestBody, null, null);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            Object responseCode = result.get("responseCode");
            if (responseCode != null && !responseCode.equals(0)) {
                String msg = (String) result.getOrDefault("responseMessage", "TMSRA xác thực thất bại");
                log.warn("TMSRA login failed: responseCode={}, message={}", responseCode, msg);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, msg);
            }

            log.info("TMSRA login success for user: {}", username);
            return result;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("TMSRA getAccessToken error: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không thể kết nối đến TMSRA: " + ex.getMessage());
        }
    }

    // ─── Tìm kiếm chứng thư: getCertificateInfoForTMSRA

    public Map<String, Object> searchCertificateInfo(
            com.example.demo.dto.TmsraSearchRequest request,
            String accessToken) {

        String url = props.getBaseUrl() + "/getCertificateInfoForTMSRA";

        // Nếu không truyền accessToken thì tự lấy bằng thông tin config
        String token = (accessToken != null && !accessToken.isBlank())
                ? accessToken
                : (String) getAccessTokenInfo(props.getUsername(), props.getPassword()).get("accessToken");

        if (token == null || token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Không lấy được accessToken");
        }

        // Build payload
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        String lang = request.language() != null ? request.language() : (props.getLanguage() != null ? props.getLanguage() : "0");
        payload.put("language", lang);

        addIfPresent(payload, "taxCode",                     request.taxCode());
        addIfPresent(payload, "pid",                         request.pid());
        addIfPresent(payload, "budgetCode",                  request.budgetCode());
        addIfPresent(payload, "decision",                    request.decision());
        addIfPresent(payload, "citizenId",                   request.citizenId());
        addIfPresent(payload, "cccd",                        request.cccd());
        addIfPresent(payload, "passport",                    request.passport());
        addIfPresent(payload, "socialInsuranceCode",         request.socialInsuranceCode());
        addIfPresent(payload, "unitCode",                    request.unitCode());
        addIfPresent(payload, "personalTaxCode",             request.personalTaxCode());
        addIfPresent(payload, "personalSocialInsuranceCode", request.personalSocialInsuranceCode());
        addIfPresent(payload, "certificateSN",               request.certificateSN());
        addIfPresent(payload, "certificateStateCode",        request.certificateStateCode());
        addIfPresent(payload, "activationCode",              request.activationCode());
        addIfPresent(payload, "expandFutureParamXML",        request.expandFutureParamXML());

        log.info("TMSRA searchCertificateInfo payload keys: {}", payload.keySet());

        try {
            String requestBody  = objectMapper.writeValueAsString(payload);
            // Header: userName = tên config, Authorization = accessToken
            String responseBody = doPost(url, requestBody, props.getUsername(), token);

            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);

            log.info("TMSRA searchCertificateInfo responseCode: {}", result.get("responseCode"));
            return result;

        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("TMSRA searchCertificateInfo error: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Lỗi tìm kiếm chứng thư: " + ex.getMessage());
        }
    }

    // Danh sách trạng thái chứng thư
    public Map<String, Object> getCertificateStates(String language, String certificateStateCode) {
        String url = props.getBaseUrl() + "/getCertificateState";

        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("language", language != null ? language : props.getLanguage());
        if (certificateStateCode != null && !certificateStateCode.isBlank()) {
            payload.put("certificateStateCode", certificateStateCode);
        }

        try {
            String requestBody  = objectMapper.writeValueAsString(payload);
            String responseBody = doPost(url, requestBody, null, null);
            @SuppressWarnings("unchecked")
            Map<String, Object> result = objectMapper.readValue(responseBody, Map.class);
            return result;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("TMSRA getCertificateState error: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không thể lấy danh sách trạng thái: " + ex.getMessage());
        }
    }


    /**
     * @param userName    Nếu khác null → thêm header "userName"
     * @param bearerToken Nếu khác null → thêm header "Authorization"
     */
    private String doPost(String urlStr, String jsonBody,
                          String userName, String bearerToken) throws Exception {
        URL url  = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Accept",       "application/json");

        if (userName != null && !userName.isBlank()) {
            conn.setRequestProperty("userName", userName);
        }
        if (bearerToken != null && !bearerToken.isBlank()) {
            conn.setRequestProperty("Authorization", bearerToken);
        }

        conn.setConnectTimeout(props.getTimeoutSeconds() * 1000);
        conn.setReadTimeout(props.getTimeoutSeconds() * 1000);
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        java.io.InputStream is = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (is == null) {
            throw new ResponseStatusException(HttpStatus.valueOf(status),
                    "TMSRA trả về HTTP" + status + "không có body");
        }

        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        log.debug("TMSRA response [{}]: {}", status, body.length() > 500 ? body.substring(0, 500) + "…" : body);

        if (status < 200 || status >= 300) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "TMSRA HTTP" + status + ": " + body);
        }
        return body;
    }

    private void addIfPresent(java.util.Map<String, Object> map, String key, String value) {
        if (value != null && !value.isBlank()) {
            map.put(key, value);
        }
    }

    private static void disableSslVerification() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers()                              { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] c, String a)              {}
                        public void checkServerTrusted(X509Certificate[] c, String a)              {}
                    }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception ex) {
            LoggerFactory.getLogger(TmsraService.class)
                    .warn("Could not disable SSL verification: {}", ex.getMessage());
        }
    }
}