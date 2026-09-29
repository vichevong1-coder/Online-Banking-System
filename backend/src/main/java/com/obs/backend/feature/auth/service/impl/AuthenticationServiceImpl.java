package com.obs.backend.feature.auth.service.impl;

import com.obs.backend.feature.auth.dto.AuthTokenResponse;
import com.obs.backend.feature.auth.dto.ForgotPasswordRequest;
import com.obs.backend.feature.auth.dto.LoginRequest;
import com.obs.backend.feature.auth.dto.LoginResponse;
import com.obs.backend.feature.auth.dto.RefreshRequest;
import com.obs.backend.feature.auth.dto.RefreshResponse;
import com.obs.backend.feature.auth.dto.ResetPasswordRequest;
import com.obs.backend.feature.auth.dto.TwoFactorResendRequest;
import com.obs.backend.feature.auth.dto.TwoFactorVerifyRequest;
import com.obs.backend.feature.auth.entity.OtpPurpose;
import com.obs.backend.feature.auth.exception.InvalidChallengeTokenException;
import com.obs.backend.feature.auth.exception.InvalidCredentialsException;
import com.obs.backend.feature.auth.exception.InvalidLoginIdentifierException;
import com.obs.backend.feature.auth.exception.InvalidOtpException;
import com.obs.backend.feature.auth.exception.InvalidRefreshTokenException;
import com.obs.backend.feature.auth.exception.PhoneNotVerifiedException;
import com.obs.backend.feature.auth.service.AuthenticationService;
import com.obs.backend.feature.auth.service.LoginAttemptService;
import com.obs.backend.feature.auth.service.OtpService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.AccountStatusPolicy;
import com.obs.backend.security.Role;
import com.obs.backend.security.jwt.JwtService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountStatusPolicy accountStatusPolicy;
    private final OtpService otpService;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;
    private final com.obs.backend.security.jwt.RevokedTokenRepository revokedTokenRepository;

    public AuthenticationServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccountStatusPolicy accountStatusPolicy,
            OtpService otpService,
            JwtService jwtService,
            LoginAttemptService loginAttemptService,
            com.obs.backend.security.jwt.RevokedTokenRepository revokedTokenRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountStatusPolicy = accountStatusPolicy;
        this.otpService = otpService;
        this.jwtService = jwtService;
        this.loginAttemptService = loginAttemptService;
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String identifier = StringUtils.hasText(request.email())
                ? request.email().trim()
                : (StringUtils.hasText(request.phone()) ? request.phone().trim() : "unknown");

        try {
            User user = findByIdentifier(request.email(), request.phone());

            if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
                loginAttemptService.recordAttempt(identifier, false, null, null);
                throw new InvalidCredentialsException();
            }
            accountStatusPolicy.checkLoginAllowed(user.getStatus());
            if (user.getRole() == Role.CUSTOMER && !user.isPhoneVerified()) {
                loginAttemptService.recordAttempt(identifier, false, null, null);
                throw new PhoneNotVerifiedException();
            }

            otpService.generateAndSend(user, OtpPurpose.LOGIN);
            String challengeToken = jwtService.generateChallengeToken(user.getId().toString());
            return new LoginResponse(challengeToken);
        } catch (RuntimeException e) {
            loginAttemptService.recordAttempt(identifier, false, null, null);
            throw e;
        }
    }

    @Override
    @Transactional
    public AuthTokenResponse verifyTwoFactor(TwoFactorVerifyRequest request) {
        User user = resolveChallengeUser(request.challengeToken());

        otpService.verify(user, OtpPurpose.LOGIN, request.code());
        String jti = jwtService.parseClaims(request.challengeToken()).getId();
        if (jti != null) {
            revokedTokenRepository.save(new com.obs.backend.security.jwt.RevokedToken(jti));
        }
        return issueTokens(user);
    }

    @Override
    @Transactional
    public LoginResponse resendTwoFactor(TwoFactorResendRequest request) {
        User user = resolveChallengeUser(request.challengeToken());

        otpService.generateAndSend(user, OtpPurpose.LOGIN);
        String challengeToken = jwtService.generateChallengeToken(user.getId().toString());
        return new LoginResponse(challengeToken);
    }

    @Override
    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        io.jsonwebtoken.Claims claims;
        try {
            claims = jwtService.parseClaims(request.refreshToken());
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
            throw new InvalidRefreshTokenException();
        }
        
        String jti = claims.getId();
        if (jti != null && revokedTokenRepository.existsById(jti)) {
            throw new InvalidRefreshTokenException();
        }
        
        UUID userId = jwtService
                .resolveRefreshSubject(request.refreshToken())
                .map(UUID::fromString)
                .orElseThrow(InvalidRefreshTokenException::new);
                
        revokedTokenRepository.findById("USER-" + userId).ifPresent(revoked -> {
            if (claims.getIssuedAt().toInstant().isBefore(revoked.getRevokedAt())) {
                throw new InvalidRefreshTokenException();
            }
        });
        
        User user = userRepository.findById(userId).orElseThrow(InvalidRefreshTokenException::new);

        accountStatusPolicy.checkLoginAllowed(user.getStatus());
        String accessToken = jwtService.generateAccessToken(user.getId().toString(), Set.of(user.getRole()));
        return new RefreshResponse(accessToken, "Bearer");
    }

    private User resolveChallengeUser(String challengeToken) {
        UUID userId = jwtService
                .resolveChallengeSubject(challengeToken)
                .map(UUID::fromString)
                .orElseThrow(InvalidChallengeTokenException::new);
        return userRepository.findById(userId).orElseThrow(InvalidChallengeTokenException::new);
    }

    private User findByIdentifier(String email, String phone) {
        String normalizedEmail = StringUtils.hasText(email) ? email.trim() : null;
        String normalizedPhone = StringUtils.hasText(phone) ? phone.trim() : null;
        if ((normalizedEmail == null) == (normalizedPhone == null)) {
            throw new InvalidLoginIdentifierException();
        }

        Optional<User> user =
                normalizedEmail != null
                        ? userRepository.findByEmail(normalizedEmail)
                        : userRepository.findByPhone(normalizedPhone);
        return user.orElseThrow(InvalidCredentialsException::new);
    }

    private AuthTokenResponse issueTokens(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId().toString(), Set.of(user.getRole()));
        String refreshToken = jwtService.generateRefreshToken(user.getId().toString());
        return new AuthTokenResponse(
                accessToken,
                refreshToken,
                "Bearer",
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole());
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        String id = request.identifier().trim();

        // Deliberately silent on a miss: the controller returns 204 either way, so this
        // unauthenticated endpoint cannot be used to test whether a phone number or email
        // banks here. A 404 for "no such customer" and a 204 for a real one is an
        // enumeration oracle, and this is reachable without a token.
        userRepository.findByPhone(id)
                .or(() -> userRepository.findByEmail(id))
                .ifPresentOrElse(
                        user -> otpService.generateAndSend(user, OtpPurpose.PASSWORD_RESET),
                        () -> log.debug("Password reset requested for an identifier with no account"));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String id = request.identifier().trim();

        // InvalidOtpException rather than UserNotFoundException, for the same reason
        // forgotPassword stays silent: an unknown identifier has to be indistinguishable
        // from a known one with a wrong code. Otherwise the oracle closed above just moves
        // to this endpoint — 404 here, 400 INVALID_OR_EXPIRED_OTP there.
        User user = userRepository.findByPhone(id)
                .or(() -> userRepository.findByEmail(id))
                .orElseThrow(InvalidOtpException::new);

        otpService.verify(user, OtpPurpose.PASSWORD_RESET, request.code());
        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void logout(com.obs.backend.feature.auth.dto.LogoutRequest request) {
        try {
            String jti = jwtService.parseClaims(request.refreshToken()).getId();
            if (jti != null) {
                revokedTokenRepository.save(new com.obs.backend.security.jwt.RevokedToken(jti));
            }
        } catch (Exception e) {
            // Ignore invalid tokens on logout
        }
    }
}
