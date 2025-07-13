package com.cache.plugin.security;

import com.cache.plugin.config.TieredCacheProperties;
import com.cache.plugin.exception.CacheSerializationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 缓存加密组件
 */
public class CacheEncryption {
    
    private static final Logger logger = LoggerFactory.getLogger(CacheEncryption.class);
    
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    private final TieredCacheProperties.EncryptionProperties properties;
    private final ObjectMapper objectMapper;
    private final SecretKey secretKey;
    private final SecureRandom secureRandom;
    
    public CacheEncryption(TieredCacheProperties.EncryptionProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.secureRandom = new SecureRandom();
        this.secretKey = initializeSecretKey();
        
        if (properties.isEnabled()) {
            logger.info("Cache encryption initialized with algorithm: {}", properties.getAlgorithm());
        }
    }
    
    /**
     * 初始化密钥
     */
    private SecretKey initializeSecretKey() {
        if (!properties.isEnabled()) {
            return null;
        }
        
        try {
            String keyString = properties.getKey();
            if (keyString != null && !keyString.isEmpty()) {
                // 使用配置的密钥
                byte[] keyBytes = Base64.getDecoder().decode(keyString);
                return new SecretKeySpec(keyBytes, AES_ALGORITHM);
            } else {
                // 生成新密钥
                KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
                keyGenerator.init(256); // AES-256
                SecretKey key = keyGenerator.generateKey();
                
                // 记录生成的密钥（生产环境应该安全存储）
                String encodedKey = Base64.getEncoder().encodeToString(key.getEncoded());
                logger.warn("Generated new encryption key (should be stored securely): {}", encodedKey);
                
                return key;
            }
        } catch (Exception e) {
            logger.error("Failed to initialize encryption key", e);
            throw new CacheSerializationException("Failed to initialize encryption key", e);
        }
    }
    
    /**
     * 加密对象
     */
    public Object encrypt(Object value) {
        if (!properties.isEnabled() || value == null) {
            return value;
        }
        
        try {
            // 序列化对象为JSON
            String jsonValue = objectMapper.writeValueAsString(value);
            byte[] plaintext = jsonValue.getBytes(StandardCharsets.UTF_8);
            
            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // 初始化加密器
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            
            // 加密
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // 组合IV和密文
            byte[] encryptedData = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encryptedData, 0, iv.length);
            System.arraycopy(ciphertext, 0, encryptedData, iv.length, ciphertext.length);
            
            // Base64编码
            return Base64.getEncoder().encodeToString(encryptedData);
            
        } catch (Exception e) {
            logger.error("Failed to encrypt cache value", e);
            throw new CacheSerializationException("Failed to encrypt cache value", e);
        }
    }
    
    /**
     * 解密对象
     */
    public Object decrypt(Object encryptedValue) {
        if (!properties.isEnabled() || encryptedValue == null) {
            return encryptedValue;
        }
        
        try {
            // 如果不是加密的字符串，直接返回
            if (!(encryptedValue instanceof String)) {
                return encryptedValue;
            }
            
            String encryptedString = (String) encryptedValue;
            
            // Base64解码
            byte[] encryptedData = Base64.getDecoder().decode(encryptedString);
            
            // 提取IV和密文
            if (encryptedData.length < GCM_IV_LENGTH) {
                logger.warn("Encrypted data too short, returning as-is");
                return encryptedValue;
            }
            
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // 初始化解密器
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // 解密
            byte[] plaintext = cipher.doFinal(ciphertext);
            String jsonValue = new String(plaintext, StandardCharsets.UTF_8);
            
            // 反序列化JSON为对象
            return objectMapper.readValue(jsonValue, Object.class);
            
        } catch (Exception e) {
            logger.error("Failed to decrypt cache value, returning as-is", e);
            // 解密失败时返回原值，可能是未加密的数据
            return encryptedValue;
        }
    }
    
    /**
     * 检查是否启用加密
     */
    public boolean isEnabled() {
        return properties.isEnabled();
    }
    
    /**
     * 获取加密算法
     */
    public String getAlgorithm() {
        return properties.getAlgorithm();
    }
}