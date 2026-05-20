package com.acueducto.veredal.waterrights.application.port;

import java.util.UUID;

public record PropertySnapshot(UUID id, String status, UUID ownerId) {
}
