package com.shterneregen.securelan.crypto.workflow;

import com.shterneregen.securelan.crypto.model.PasswordEncryptedData;

public interface PasswordFileCryptoWorkflow {
    PasswordEncryptedData encrypt(byte[] fileBytes, char[] password);

    byte[] decrypt(PasswordEncryptedData encryptedData, char[] password);
}
