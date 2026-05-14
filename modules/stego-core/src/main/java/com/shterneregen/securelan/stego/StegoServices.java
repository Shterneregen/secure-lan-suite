package com.shterneregen.securelan.stego;

import com.shterneregen.securelan.crypto.CryptoServices;
import com.shterneregen.securelan.stego.service.SteganographyService;
import com.shterneregen.securelan.stego.service.impl.BmpSteganographyService;

import java.util.Objects;

public final class StegoServices {
    private final SteganographyService steganographyService;

    private StegoServices(CryptoServices cryptoServices) {
        Objects.requireNonNull(cryptoServices, "cryptoServices");
        this.steganographyService = new BmpSteganographyService(cryptoServices.passwordFileCryptoWorkflow());
    }

    public static StegoServices createDefault() {
        return new StegoServices(CryptoServices.createDefault());
    }

    public static StegoServices create(CryptoServices cryptoServices) {
        return new StegoServices(cryptoServices);
    }

    public SteganographyService steganographyService() {
        return steganographyService;
    }
}
