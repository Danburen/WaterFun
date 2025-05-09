package org.waterwood.waterfunservice.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.waterwood.waterfunservice.DTO.LoginRequest;
import org.waterwood.waterfunservice.DTO.enums.ErrorCode;
import org.waterwood.waterfunservice.service.authServices.CaptchaService;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private CaptchaService captchaService;

    /** redis + cookie(HttpOnly) save captcha
     * Generate the captcha
     */
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response) throws IOException{
        CaptchaService.LineCaptchaResult result = captchaService.generateVerifyCode();
        Cookie cookie = new Cookie("CAPTCHA_KEY",result.uuid());
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(120);
        response.addCookie(cookie);
        // set the header of response
        response.setContentType("image/jpeg");
        response.setHeader("Pragma", "No-cache");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setDateHeader("Expires", 0);
        // write img stream to response stream
        result.captcha().write(response.getOutputStream());
    }

    @GetMapping("/verify-code")
    public void getVerifyCode(HttpServletResponse response) throws IOException {

    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest,HttpServletRequest request) {;
        if (loginRequest.getUsername() == null) {
            return ErrorCode.USERNAME_EMPTY.toResponseEntity();
        }else if (loginRequest.getPassword() == null) {
            return ErrorCode.PASSWORD_EMPTY.toResponseEntity();
        }else if(loginRequest.getCaptcha() == null || loginRequest.getCaptcha().isEmpty()) {
            return ErrorCode.CAPTCHA_EMPTY.toResponseEntity();
        }
        // get captcha key(uuid) from cookie from frontend
        String uuid = Arrays.stream(request.getCookies())
                .filter(c->"CAPTCHA_KEY".equals(c.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .orElse(null);
        if (uuid == null) {
            return ResponseEntity.badRequest().body("can't get uuid");
//            return ErrorCode.CAPTCHA_EXPIRED.toResponseEntity();
        }
        // get captcha code from redis
        String correctCode = captchaService.getCode(uuid);
        if(correctCode == null){
            return ResponseEntity.badRequest().body("redis can't get captcha key");
            //return ErrorCode.CAPTCHA_EXPIRED.toResponseEntity();
        }
        // verify the captcha code from user and redis
        if(!correctCode.equals(loginRequest.getCaptcha())){
            return ErrorCode.CAPTCHA_INCORRECT.toResponseEntity();
        }
        if (!"admin".equals(loginRequest.getUsername()) ||
                !"123456".equals(loginRequest.getPassword())) {
            return ErrorCode.USERNAME_OR_PASSWORD_INCORRECT.toResponseEntity();
        }
        captchaService.removeCode(uuid);
        return ResponseEntity.ok("Successfully Login!");
    }
}
