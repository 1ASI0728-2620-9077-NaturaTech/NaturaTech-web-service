package com.plantsync.platform.iam.infrastructure.hashing.bcrypt;

import com.plantsync.platform.iam.application.internal.outboundservices.hashing.HashingService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * This interface is a marker interface for the Bcrypt hashing service.
 * It extends the {@link HashingService} and {@link PasswordEncoder} interfaces.
 */
public interface BCryptHashingService extends HashingService, PasswordEncoder {
}
