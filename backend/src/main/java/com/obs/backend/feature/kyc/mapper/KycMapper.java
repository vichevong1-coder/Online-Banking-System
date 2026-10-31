package com.obs.backend.feature.kyc.mapper;

import com.obs.backend.feature.kyc.dto.KycApplicationResponse;
import com.obs.backend.feature.kyc.dto.KycDocumentInfo;
import com.obs.backend.feature.kyc.entity.KycDocument;
import com.obs.backend.feature.user.entity.User;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class KycMapper {

    public KycApplicationResponse toApplication(User user, List<KycDocument> documents) {
        List<KycDocumentInfo> infos = documents.stream()
                .sorted(Comparator.comparing(KycDocument::getType))
                .map(d -> new KycDocumentInfo(d.getType(), d.getSizeBytes(), d.getSha256(), d.getCreatedAt()))
                .toList();
        return new KycApplicationResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getPhone(),
                user.getNidNumber(),
                user.getNidExpiryDate(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getStatus(),
                user.getRejectionReason(),
                user.getCreatedAt(),
                infos);
    }
}
