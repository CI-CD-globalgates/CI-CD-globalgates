package com.app.globalgates.controller.member;

import com.app.globalgates.aop.annotation.LogStatus;
import com.app.globalgates.aop.annotation.LogStatusWithReturn;
import com.app.globalgates.auth.CustomUserDetails;
import com.app.globalgates.auth.JwtTokenProvider;
import com.app.globalgates.dto.MemberDTO;
import com.app.globalgates.service.MemberService;
import java.io.IOException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/member/**")
@RequiredArgsConstructor
@Slf4j
public class MemberAPIController implements MemberAPIControllerDocs {
    private final MemberService memberService;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final HttpServletResponse response;

    //  회원가입
    @PostMapping("join")
    @LogStatus
    public ResponseEntity<?> join(MemberDTO memberDTO, @RequestParam(value = "file", required = false) MultipartFile file) throws IOException {
        log.info("memberDTO {}", memberDTO);
        memberService.join(memberDTO, file);
        return ResponseEntity.ok(Map.of("message", "회원가입 성공"));
    }

    @PostMapping("oauth/join")
    @LogStatusWithReturn
    public ResponseEntity<?> oauthJoin(
            MemberDTO memberDTO,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) throws IOException {
        // 프론트가 보내는 provider/providerId/profileURL/oauthJoin/memberName/memberEmail/memberPhone
        // + 추가정보를 받아서
        // OAuth 신규가입 전용 서비스로 넘김
        return ResponseEntity.ok(Map.of("message", "SNS 회원가입 성공"));
    }

    @GetMapping("check-email")
    @LogStatusWithReturn
    public boolean checkEmail(@RequestParam String memberEmail){
        return memberService.checkEmail(memberEmail);
    }
    @GetMapping("check-phone")
    @LogStatusWithReturn
    public boolean checkPhone(@RequestParam String memberPhone){
        return memberService.checkPhone(memberPhone);
    }
    @GetMapping("check-handle")
    @LogStatusWithReturn
    public boolean checkHandle(@RequestParam String memberHandle){
        // 아이디 모달에서 blur 시 중복검사를 호출한다.
        return memberService.checkHandle(memberHandle);
    }

    @GetMapping("check-company-name")
    @LogStatusWithReturn
    public boolean checkCompanyName(@RequestParam String companyName){
        return memberService.checkCompanyName(companyName);
    }

    @GetMapping("check-business-number")
    @LogStatusWithReturn
    public boolean checkBusinessNumber(@RequestParam String businessNumber){
        return memberService.checkBusinessNumber(businessNumber);
    }

    @PostMapping("login")
    @LogStatusWithReturn
    public ResponseEntity<?> login(@RequestBody MemberDTO memberDTO){
        log.info("memberDTO: {}", memberDTO);
        try{
            Map<String, String> tokenMap = new HashMap<>();

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(memberDTO.getLoginId(),memberDTO.getMemberPassword())
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            log.info("authentication: {}", (CustomUserDetails) authentication.getPrincipal());

            String accessToken = jwtTokenProvider.createAccessToken(memberDTO.getLoginId());
            jwtTokenProvider.createRefreshToken(memberDTO.getLoginId());

            tokenMap.put("accessToken", accessToken);

            Cookie rememberLoginIdCookie = new Cookie("rememberLoginId", memberDTO.getLoginId());

            rememberLoginIdCookie.setPath("/");
            rememberLoginIdCookie.setMaxAge(60 * 60 * 24 * 30);
            response.addCookie(rememberLoginIdCookie);

            return ResponseEntity.ok(tokenMap);
        }catch (Exception e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "로그인 실패"));
        }
    }

    // 일반 로그인은 그대로 두고, 재활성화는 로그인 실패 후 별도 흐름으로만 진입시킨다.
    // 여기서는 inactive 계정인지와 비밀번호 일치 여부만 확인해 확인 모달용 최소 정보만 내려준다.
    @PostMapping("reactivation/prepare")
    @LogStatusWithReturn
    public ResponseEntity<?> prepareReactivation(@RequestBody MemberDTO memberDTO) {
        try {
            MemberDTO member = memberService.getInactiveMemberForReactivation(
                    memberDTO.getLoginId(),
                    memberDTO.getMemberPassword()
            );

            boolean useEmail = memberDTO.getLoginId() != null && memberDTO.getLoginId().contains("@");

            return ResponseEntity.ok(Map.of(
                    "useEmail", useEmail,
                    "maskedTarget", memberService.getMaskedReactivationTarget(memberDTO.getLoginId(), member)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 인증코드 확인이 끝난 뒤 inactive 상태만 active로 복구하고,
    // 기존 로그인과 같은 인증 매니저 흐름으로 access/refresh 토큰 발급까지 마무리한다.
    @PostMapping("reactivation/complete")
    @LogStatusWithReturn
    public ResponseEntity<?> completeReactivation(@RequestBody MemberDTO memberDTO) {
        try {
            memberService.reactivateMember(
                    memberDTO.getLoginId(),
                    memberDTO.getMemberPassword()
            );

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            memberDTO.getLoginId(),
                            memberDTO.getMemberPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtTokenProvider.createAccessToken(memberDTO.getLoginId());
            jwtTokenProvider.createRefreshToken(memberDTO.getLoginId());

            Cookie rememberLoginIdCookie = new Cookie("rememberLoginId", memberDTO.getLoginId());
            rememberLoginIdCookie.setPath("/");
            rememberLoginIdCookie.setMaxAge(60 * 60 * 24 * 30);
            response.addCookie(rememberLoginIdCookie);

            return ResponseEntity.ok(Map.of("accessToken", accessToken));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("info")
    @LogStatusWithReturn
    public MemberDTO getUserInfo(HttpServletRequest request) {
        String token = jwtTokenProvider.parseTokenFromHeader(request);
        String userName = jwtTokenProvider.getUsername(token);
        MemberDTO memberDTO = memberService.getMember(userName);

        return memberDTO;
    }

    //  프로필 수정
    @PostMapping("profile/update")
    @LogStatusWithReturn
    public ResponseEntity<?> updateProfile(
            MemberDTO memberDTO,
            @RequestParam(value = "profileImage", required = false) MultipartFile profileImage,
            @RequestParam(value = "bannerImage", required = false) MultipartFile bannerImage,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) throws IOException {
        memberService.updateProfile(
                userDetails.getUsername(),
                userDetails.getId(),
                memberDTO,
                profileImage,
                bannerImage
        );
        return ResponseEntity.ok(Map.of("message", "프로필 수정 성공"));
    }
}
