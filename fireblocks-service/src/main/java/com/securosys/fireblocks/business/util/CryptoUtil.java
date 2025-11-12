// SPDX-FileCopyrightText: Copyright 2025 Securosys SA
// SPDX-License-Identifier: Apache-2.0

package com.securosys.fireblocks.business.util;

import com.securosys.fireblocks.business.exceptions.BusinessException;
import com.securosys.fireblocks.business.exceptions.BusinessReason;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * This class offers various helper methods for using crypto operations in Java. Generally this class should only offer methods
 * that are not specific to the JCE. But it may offer methods that uses JCE specific objects like a BLS public key which is
 * a public key object. In this case the method does not separate the different public keys but works for all public keys in
 * general.
 */
@Component
@RequiredArgsConstructor
public class CryptoUtil {

	private static final String X509_CERTIFICATE_TYPE = "X.509";

	private final ResourceLoader resourceLoader;

	/**
	 * Reads a PEM-encoded X509 certificate from a file and parses it to an X509 certificate object
	 * @param certificatePem The PEM-encoded X509 certificate file
	 * @return the x509 certificate object
	 */
	public static X509Certificate parseX509CertificateFromFile(File certificatePem) {
		byte[] certificate = readPemEncodedFile(certificatePem);
		InputStream certificateInputStream = new ByteArrayInputStream(certificate);
		try {
			CertificateFactory factory = CertificateFactory.getInstance(X509_CERTIFICATE_TYPE);
			return (X509Certificate) factory.generateCertificate(certificateInputStream);
		}
		catch (CertificateException e) {
			throw new BusinessException("Could not parse X509 certificate", BusinessReason.ERROR_PARSING_CERTIFICATE, e);
		}
	}

	/**
	 * Loads a certificate from a specified file location
	 * @param certificateLocation The location of the PEM encoded certificate
	 * @return The certificate object
	 */
	public X509Certificate loadCertificate(String certificateLocation){
		File certificatePem = readFile(certificateLocation);
		try {
			byte[] certificate = readPemEncodedFile(certificatePem);
			CertificateFactory cf = CertificateFactory.getInstance(X509_CERTIFICATE_TYPE);
			InputStream certificateInputStream = new ByteArrayInputStream(certificate);
			return (X509Certificate) cf.generateCertificate(certificateInputStream);
		}
		catch (CertificateException e) {
			String msg = String.format("Could not parse certificate '%s'", certificatePem.getName());
			throw new BusinessException(msg, BusinessReason.ERROR_INVALID_CRYPTO_FILE, e);
		}
	}

	/**
	 * Reads PEM-encoded content from a file
	 * @param pemFile the PEM-encoded file to read from
	 * @return content of the file as a byte array
	 */
	public static byte[] readPemEncodedFile(File pemFile) {
		try (FileReader keyReader = new FileReader(pemFile);
			 PemReader pemReader = new PemReader(keyReader)) {
			PemObject pemObject = pemReader.readPemObject();
			return pemObject.getContent();
		}
		catch (IOException e) {
			throw new BusinessException("Could not read pem file", BusinessReason.ERROR_READING_PEM, e);
		}
	}

	private File readFile(String location) {
		try {
			Resource resource = resourceLoader.getResource(location);
			if(resource.exists()) {
				return resource.getFile();
			}
			else {
				String msg = String.format("Could not load file from location '%s' as it does not exist", location);
				throw new BusinessException(msg, BusinessReason.ERROR_INVALID_CONFIG_INPUT);
			}

		}
		catch (IOException e) {
			String msg = String.format("Could not load file from location '%s'", location);
			throw new BusinessException(msg, BusinessReason.ERROR_INVALID_CONFIG_INPUT, e);
		}
	}

	public PublicKey getPublicKeyFromBase64(String publicKeyPath) {
		String base64DerEncodedPublicKey = "";
		try {
			base64DerEncodedPublicKey = readPublicKey(publicKeyPath);
			byte[] publicKeyRaw = Base64.getDecoder().decode(base64DerEncodedPublicKey);

			try (ASN1InputStream asn1 = new ASN1InputStream(publicKeyRaw)) {
				SubjectPublicKeyInfo pki = SubjectPublicKeyInfo.getInstance(asn1.readObject());
				String algOid = getAlgorithmForOid(pki.getAlgorithm().getAlgorithm().getId());
				if (algOid == null) {
					throw new BusinessException("Unsupported algorithm OID in public key: " + base64DerEncodedPublicKey,
							BusinessReason.ERROR_INPUT_VALIDATION_FAILED);
				}

				X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyRaw);
				Security.addProvider(new BouncyCastleProvider());
				KeyFactory keyFactory = KeyFactory.getInstance(algOid, "BC");
				return keyFactory.generatePublic(keySpec);
			}
		} catch (InvalidKeySpecException e) {
			throw new BusinessException("Invalid key spec: " + e.getMessage(), BusinessReason.ERROR_INPUT_VALIDATION_FAILED, e);
		} catch (Exception e) {
			throw new BusinessException("Could not create public key from base64-string", BusinessReason.ERROR_INPUT_VALIDATION_FAILED, e);
		}
	}

	public String readPublicKey(String publicKeyPath) throws IOException, URISyntaxException {
		byte[] keyBytes = readFileBytes(publicKeyPath);
		return new String(keyBytes, StandardCharsets.UTF_8)
				.replaceAll("-----BEGIN (.*)PUBLIC KEY-----", "")
				.replaceAll("-----END (.*)PUBLIC KEY-----", "")
				.replaceAll("\\s", "");
	}

	public static byte[] readFileBytes(String filePath) throws IOException, URISyntaxException {
		if (filePath.startsWith("classpath:")) {
			String resourcePath = filePath.substring("classpath:".length());
			try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath)) {
				if (inputStream == null) {
					throw new BusinessException("Resource not found: " + filePath, BusinessReason.ERROR_INVALID_CRYPTO_FILE);
				}
				return inputStream.readAllBytes();
			}
		} else if (filePath.startsWith("file:")) {
			Path path = Paths.get(new URI(filePath));
			return Files.readAllBytes(path);
		} else {
			Path path = Paths.get(filePath);
			return Files.readAllBytes(path);
		}
	}

	public static String getAlgorithmForOid(String algOid) {
		String alg = null;
		if (algOid == null) {
			return null;
		}
		else if (algOid.startsWith("1.2.840.113549.1.1")) {
			// this is RSA, but the OID is not used as a key factory id, so we have to map
			alg = "RSA";
		}
		else if (algOid.startsWith("1.2.840.10045.2.1")) {
			// this is EC
			alg = "EC";
		}
		else if(algOid.startsWith("1.3.101.112")) {
			alg = "Ed25519";
		}
		else if(algOid.startsWith("1.3.6.1.4.1.44668.5.3.1.1")) {
			alg = "BLS";
		}
		else if (algOid.startsWith("1.2.840.10040.4.1")) {
			// DSA
			alg = "DSA";
		}
		else if (algOid.equals("1.0.18033.3.2.1")) {
			alg = "AES";
		}
		else if (algOid.equals("1.0.18033.3.2.2")) {
			alg = "Camellia";
		}
		else if (algOid.equals("1.2.840.113549.1.9.16.3.18")) {
			alg = "ChaCha20";
		}
		else if (algOid.equals("1.0.18033.3.1.1")) {
			alg = "TDEA";
		}
        return alg;
	}

}
