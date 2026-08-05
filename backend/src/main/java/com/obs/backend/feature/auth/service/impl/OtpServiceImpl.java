package com.obs.backend.feature.auth.service.impl;

import com.obs.backend.feature.auth.entity.OtpCode;
import com.obs.backend.feature.auth.entity.OtpPurpose;
import com.obs.backend.feature.auth.exception.InvalidOtpException;
import com.obs.backend.feature.auth.exception.UserNotFoundException;
import com.obs.backend.feature.auth.repository.OtpCodeRepository;
import com.obs.backend.feature.auth.service.OtpSender;
import com.obs.backend.feature.auth.service.OtpService;
import com.obs.backend.feature.user.entity.User;
import com.obs.backend.feature.user.repository.UserRepository;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OtpServiceImpl implements OtpService {

    private static final int CODE_LENGTH = 6;
    private static final int CODE_BOUND = 1_000_000;
    private static final Duration CODE_TTL = Duration.ofMinutes(5);

    private final OtpCodeRepository otpCodeRepository;
    private final UserRepository userRepository;
    private final OtpSender otpSender;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpServiceImpl(
            OtpCodeRepository otpCodeRepository,
            UserRepository userRepository,
            OtpSender otpSender,
            PasswordEncoder passwordEncoder) {
        this.otpCodeRepository = otpCodeRepository;
        this.userRepository = userRepository;
        this.otpSender = otpSender;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void generateAndSend(User user, OtpPurpose purpose) {
        otpCodeRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .ifPresent(existing -> {
                    existing.markConsumed();
                    otpCodeRepository.save(existing);
                });

        String code = generateCode();
        OtpCode otpCode =
                new OtpCode(user.getId(), purpose, passwordEncoder.encode(code), Instant.now().plus(CODE_TTL));
        otpCodeRepository.save(otpCode);
        otpSender.send(user.getPhone(), code);
    }

    @Override
    @Transactional
    public void verify(User user, OtpPurpose purpose, String code) {
        OtpCode otpCode = otpCodeRepository
                .findFirstByUserIdAndPurposeAndConsumedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .filter(candidate -> candidate.getExpiresAt().isAfter(Instant.now()))
                .filter(candidate -> passwordEncoder.matches(code, candidate.getCodeHash()))
                .orElseThrow(InvalidOtpException::new);

        otpCode.markConsumed();
        otpCodeRepository.save(otpCode);
    }

    @Override
    @Transactional
    public void verifyRegistration(String phone, String code) {
        User user = userRepository.findByPhone(phone).orElseThrow(UserNotFoundException::new);
        verify(user, OtpPurpose.REGISTRATION, code);
        user.markPhoneVerified();
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void resendRegistration(String phone) {
        User user = userRepository.findByPhone(phone).orElseThrow(UserNotFoundException::new);
        generateAndSend(user, OtpPurpose.REGISTRATION);
    }

    private String generateCode() {
        return String.format("%0" + CODE_LENGTH + "d", secureRandom.nextInt(CODE_BOUND));
    }
}
