package com.obs.backend.feature.auth.service.impl;

import com.obs.backend.feature.auth.dto.AuthTokenResponse;
import com.obs.backend.feature.auth.dto.LoginRequest;
import com.obs.backend.feature.auth.dto.LoginResponse;
import com.obs.backend.feature.auth.dto.RefreshRequest;
import com.obs.backend.feature.auth.dto.RefreshResponse;
import com.obs.backend.feature.auth.dto.TwoFactorResendRequest;
import com.obs.backend.feature.auth.dto.TwoFactorVerifyRequest;
import com.obs.backend.feature.auth.entity.OtpPurpose;
import com.obs.backend.feature.auth.exception.InvalidChallengeTokenException;
import com.obs.backend.feature.auth.exception.InvalidCredentialsException;
import com.obs.backend.feature.auth.exception.InvalidLoginIdentifierException;
import com.obs.backend.feature.auth.exception.InvalidRefreshTokenException;
import com.obs.backend.feature.auth.exception.PhoneNotVerifiedException;
import com.obs.backend.feature.auth.service.AuthenticationService;
import com.obs.backend.feature.auth.service.OtpService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import com.obs.backend.security.AccountStatusPolicy;
import com.obs.backend.security.Role;
import com.obs.backend.security.jwt.JwtService;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountStatusPolicy accountStatusPolicy;
    private final OtpService otpService;
    private final JwtService jwtService;

    public AuthenticationServiceImpl(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AccountStatusPolicy accountStatusPolicy,
            OtpService otpService,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountStatusPolicy = accountStatusPolicy;
        this.otpService = otpService;
        this.jwtService = jwtService;
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = findByIdentifier(request.email(), request.phone());

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        accountStatusPolicy.checkLoginAllowed(user.getStatus());
        if (user.getRole() == Role.CUSTOMER && !user.isPhoneVerified()) {
            throw new PhoneNotVerifiedException();
        }

        otpService.generateAndSend(user, OtpPurpose.LOGIN);
        String challengeToken = jwtService.generateChallengeToken(user.getId().toString());
        return new LoginResponse(challengeToken);
    }

    @Override
    @Transactional
    public AuthTokenResponse verifyTwoFactor(TwoFactorVerifyRequest request) {
        User user = resolveChallengeUser(request.challengeToken());

        otpService.verify(user, OtpPurpose.LOGIN, request.code());
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
        UUID userId = jwtService
                .resolveRefreshSubject(request.refreshToken())
                .map(UUID::fromString)
                .orElseThrow(InvalidRefreshTokenException::new);
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
}
