package com.example.demo.controller;

import com.example.demo.dto.TmsraSearchRequest;
import com.example.demo.service.TmsraService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tmsra")
public class TmsraController {

    private static final Logger log = LoggerFactory.getLogger(TmsraController.class);

    private final TmsraService tmsraService;

    public TmsraController(TmsraService tmsraService) {
        this.tmsraService = tmsraService;
    }

    /**
     * Đăng nhập TMSRA → trả về accessToken.
     */
    @PostMapping("/login")
    public Map<String, Object> loginAndGetAccessToken(
            @RequestBody(required = false) AuthRequest request) {
        if (request == null || request.normalizedUsername() == null
                || request.password() == null || request.password().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Thiếu username hoặc password");
        }
        return tmsraService.getAccessTokenInfo(request.normalizedUsername(), request.password());
    }


    @PostMapping({"/search-certificate", "/search"})
    public Map<String, Object> searchCertificateInfo(
            @RequestBody(required = false) TmsraSearchRequest request,
            HttpServletRequest httpRequest) {

        TmsraSearchRequest safeRequest = (request != null) ? request : TmsraSearchRequest.empty();

        // Lấy accessToken từ header Authorization của client gửi lên
        String accessToken = httpRequest.getHeader("Authorization");
        // Nếu header trống thì để null, service sẽ tự lấy
        if (accessToken != null && accessToken.isBlank()) {
            accessToken = null;
        }

        try {
            return tmsraService.searchCertificateInfo(safeRequest, accessToken);
        } catch (ResponseStatusException ex) {
            log.warn("TMSRA searchCertificate failed: status={}, message={}", ex.getStatusCode(), ex.getReason());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error while searching TMSRA certificates", ex);
            throw ex;
        }
    }

    /**
     * Lấy danh sách trạng thái chứng thư.
     */
    @GetMapping("/states")
    public Map<String, Object> getCertificateStates(
            @RequestParam(defaultValue = "0") String language,
            @RequestParam(required = false) String certificateStateCode) {
        return tmsraService.getCertificateStates(language, certificateStateCode);
    }
}
