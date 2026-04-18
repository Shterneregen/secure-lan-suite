package com.shterneregen.securelan.crypto;

import com.shterneregen.securelan.crypto.keystore.KeyStoreService;
import com.shterneregen.securelan.crypto.keystore.impl.DefaultKeyStoreService;
import com.shterneregen.securelan.crypto.service.AesGcmCryptoService;
import com.shterneregen.securelan.crypto.service.FileCryptoService;
import com.shterneregen.securelan.crypto.service.HashService;
import com.shterneregen.securelan.crypto.service.KeyEncodingService;
import com.shterneregen.securelan.crypto.service.KeyGenerationService;
import com.shterneregen.securelan.crypto.service.RsaCryptoService;
import com.shterneregen.securelan.crypto.service.SignatureService;
import com.shterneregen.securelan.crypto.service.impl.DefaultAesGcmCryptoService;
import com.shterneregen.securelan.crypto.service.impl.DefaultFileCryptoService;
import com.shterneregen.securelan.crypto.service.impl.DefaultHashService;
import com.shterneregen.securelan.crypto.service.impl.DefaultKeyEncodingService;
import com.shterneregen.securelan.crypto.service.impl.DefaultKeyGenerationService;
import com.shterneregen.securelan.crypto.service.impl.DefaultRsaCryptoService;
import com.shterneregen.securelan.crypto.service.impl.DefaultSignatureService;
import com.shterneregen.securelan.crypto.workflow.HybridFileCryptoWorkflow;
import com.shterneregen.securelan.crypto.workflow.PasswordFileCryptoWorkflow;
import com.shterneregen.securelan.crypto.workflow.impl.DefaultHybridFileCryptoWorkflow;
import com.shterneregen.securelan.crypto.workflow.impl.DefaultPasswordFileCryptoWorkflow;

public final class CryptoServices {
    private final KeyGenerationService keyGenerationService;
    private final KeyEncodingService keyEncodingService;
    private final AesGcmCryptoService aesGcmCryptoService;
    private final RsaCryptoService rsaCryptoService;
    private final HashService hashService;
    private final SignatureService signatureService;
    private final HybridFileCryptoWorkflow hybridFileCryptoWorkflow;
    private final PasswordFileCryptoWorkflow passwordFileCryptoWorkflow;
    private final FileCryptoService fileCryptoService;
    private final KeyStoreService keyStoreService;

    private CryptoServices() {
        this.keyGenerationService = new DefaultKeyGenerationService();
        this.keyEncodingService = new DefaultKeyEncodingService();
        this.aesGcmCryptoService = new DefaultAesGcmCryptoService();
        this.rsaCryptoService = new DefaultRsaCryptoService();
        this.hashService = new DefaultHashService();
        this.signatureService = new DefaultSignatureService();
        this.hybridFileCryptoWorkflow = new DefaultHybridFileCryptoWorkflow(
                aesGcmCryptoService,
                rsaCryptoService,
                keyGenerationService,
                keyEncodingService
        );
        this.passwordFileCryptoWorkflow = new DefaultPasswordFileCryptoWorkflow();
        this.fileCryptoService = new DefaultFileCryptoService(hybridFileCryptoWorkflow, passwordFileCryptoWorkflow);
        this.keyStoreService = new DefaultKeyStoreService();
    }

    public static CryptoServices createDefault() {
        return new CryptoServices();
    }

    public KeyGenerationService keyGenerationService() {
        return keyGenerationService;
    }

    public KeyEncodingService keyEncodingService() {
        return keyEncodingService;
    }

    public AesGcmCryptoService aesGcmCryptoService() {
        return aesGcmCryptoService;
    }

    public RsaCryptoService rsaCryptoService() {
        return rsaCryptoService;
    }

    public HashService hashService() {
        return hashService;
    }

    public SignatureService signatureService() {
        return signatureService;
    }

    public HybridFileCryptoWorkflow hybridFileCryptoWorkflow() {
        return hybridFileCryptoWorkflow;
    }

    public PasswordFileCryptoWorkflow passwordFileCryptoWorkflow() {
        return passwordFileCryptoWorkflow;
    }

    public FileCryptoService fileCryptoService() {
        return fileCryptoService;
    }

    public KeyStoreService keyStoreService() {
        return keyStoreService;
    }
}
