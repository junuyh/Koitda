package com.koitda.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 정산 계좌·사업자번호 등 민감 컬럼의 AES-GCM 암복호화(ERD: 개인정보·정산 계좌 컬럼 암호화).
 * 키는 환경변수(KOITDA_FIELD_ENC_KEY)로 주입하며 저장소에 커밋하지 않는다.
 * Hibernate 가 사용하지만 Spring 빈으로 등록해 키를 주입한다(Boot 의 빈 컨테이너 통합).
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

	private static final int IV_LENGTH = 12;
	private static final int TAG_BITS = 128;

	private final SecretKeySpec key;
	private final SecureRandom random = new SecureRandom();

	public EncryptedStringConverter(
			@Value("${koitda.security.field-encryption-key:koitda-dev-field-key-change-me!}") String rawKey) {
		// AES-256 = 32바이트. 데모 편의상 길이를 32로 맞춘다(운영은 정확한 32바이트 키 사용).
		byte[] key32 = Arrays.copyOf(rawKey.getBytes(StandardCharsets.UTF_8), 32);
		this.key = new SecretKeySpec(key32, "AES");
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		if (attribute == null) {
			return null;
		}
		try {
			byte[] iv = new byte[IV_LENGTH];
			random.nextBytes(iv);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] cipherText = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
			byte[] out = ByteBuffer.allocate(iv.length + cipherText.length).put(iv).put(cipherText).array();
			return Base64.getEncoder().encodeToString(out);
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("필드 암호화 실패", e);
		}
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		if (dbData == null) {
			return null;
		}
		try {
			ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(dbData));
			byte[] iv = new byte[IV_LENGTH];
			buffer.get(iv);
			byte[] cipherText = new byte[buffer.remaining()];
			buffer.get(cipherText);
			Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException e) {
			throw new IllegalStateException("필드 복호화 실패", e);
		}
	}
}
