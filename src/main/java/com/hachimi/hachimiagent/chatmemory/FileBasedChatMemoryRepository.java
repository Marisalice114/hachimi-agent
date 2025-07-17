package com.hachimi.hachimiagent.chatmemory;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import lombok.extern.slf4j.Slf4j;
import org.objenesis.strategy.StdInstantiatorStrategy;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * 基于文件的聊天记忆存储库实现
 * 参照 InMemoryChatMemoryRepository 的设计，将数据持久化到文件系统
 */
@Slf4j
public class FileBasedChatMemoryRepository implements ChatMemoryRepository {

    // 存储位置
    private final String storagePath;

    // 文件扩展名
    private static final String FILE_EXTENSION = ".chat";

    // Kryo 序列化工具
    private static final Kryo kryo = new Kryo();

    // 内存缓存，提高读取性能
    private final ConcurrentHashMap<String, List<Message>> cache = new ConcurrentHashMap<>();

    // 读写锁，保证线程安全
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // 是否启用缓存
    private final boolean enableCache;

    // 静态初始化 Kryo
    static {
        kryo.setRegistrationRequired(false); // 开启动态注册
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy()); // 设置实例化策略
    }

    /**
     * 构造函数，默认启用缓存
     */
    public FileBasedChatMemoryRepository(String storagePath) {
        this(storagePath, true);
    }

    /**
     * 构造函数，可控制是否启用缓存
     */
    public FileBasedChatMemoryRepository(String storagePath, boolean enableCache) {
        this.storagePath = storagePath;
        this.enableCache = enableCache;

        // 确保存储目录存在
        File storageDir = new File(storagePath);
        if (!storageDir.exists()) {
            if (storageDir.mkdirs()) {
                log.info("Created chat memory storage directory: {}", storagePath);
            } else {
                throw new RuntimeException("Failed to create storage directory: " + storagePath);
            }
        }

        // 如果启用缓存，预加载所有对话到内存
        if (enableCache) {
            preloadCache();
        }

        log.info("FileBasedChatMemoryRepository initialized with storage path: {}, cache enabled: {}",
                storagePath, enableCache);
    }

    @Override
    public List<String> findConversationIds() {
        lock.readLock().lock();
        try {
            if (enableCache) {
                // 从缓存获取
                return new ArrayList<>(cache.keySet());
            } else {
                // 直接从文件系统扫描
                return scanConversationIdsFromFileSystem();
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        lock.readLock().lock();
        try {
            if (enableCache) {
                // 从缓存获取
                List<Message> messages = cache.get(conversationId);
                return messages != null ? new ArrayList<>(messages) : new ArrayList<>();
            } else {
                // 直接从文件读取
                return loadMessagesFromFile(conversationId);
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Conversation ID cannot be null or empty");
        }

        lock.writeLock().lock();
        try {
            // 保存到文件
            saveMessagesToFile(conversationId, messages);

            // 更新缓存
            if (enableCache) {
                if (messages == null || messages.isEmpty()) {
                    cache.remove(conversationId);
                } else {
                    cache.put(conversationId, new ArrayList<>(messages));
                }
            }

            log.debug("Saved {} messages for conversation: {}",
                    messages != null ? messages.size() : 0, conversationId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }

        lock.writeLock().lock();
        try {
            // 删除文件
            File file = new File(storagePath, conversationId + FILE_EXTENSION);
            if (file.exists()) {
                if (file.delete()) {
                    log.debug("Deleted conversation file: {}", conversationId);
                } else {
                    log.warn("Failed to delete conversation file: {}", conversationId);
                }
            }

            // 从缓存移除
            if (enableCache) {
                cache.remove(conversationId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 预加载所有对话到缓存
     */
    private void preloadCache() {
        try {
            List<String> conversationIds = scanConversationIdsFromFileSystem();
            for (String conversationId : conversationIds) {
                List<Message> messages = loadMessagesFromFile(conversationId);
                if (!messages.isEmpty()) {
                    cache.put(conversationId, messages);
                }
            }
            log.info("Preloaded {} conversations into cache", cache.size());
        } catch (Exception e) {
            log.error("Failed to preload cache", e);
        }
    }

    /**
     * 从文件系统扫描所有对话ID
     */
    private List<String> scanConversationIdsFromFileSystem() {
        try (Stream<Path> paths = Files.walk(Paths.get(storagePath))) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(FILE_EXTENSION))
                    .map(path -> {
                        String fileName = path.getFileName().toString();
                        return fileName.substring(0, fileName.length() - FILE_EXTENSION.length());
                    })
                    .toList();
        } catch (IOException e) {
            log.error("Failed to scan conversation IDs from file system", e);
            return new ArrayList<>();
        }
    }

    /**
     * 从文件加载消息
     */
    private List<Message> loadMessagesFromFile(String conversationId) {
        File file = new File(storagePath, conversationId + FILE_EXTENSION);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Input input = new Input(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            List<Message> messages = kryo.readObject(input, ArrayList.class);
            return messages != null ? messages : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to load messages from file for conversation: {}", conversationId, e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存消息到文件
     */
    private void saveMessagesToFile(String conversationId, List<Message> messages) {
        File file = new File(storagePath, conversationId + FILE_EXTENSION);

        if (messages == null || messages.isEmpty()) {
            // 如果消息为空，删除文件
            if (file.exists() && !file.delete()) {
                log.warn("Failed to delete empty conversation file: {}", conversationId);
            }
            return;
        }

        try (Output output = new Output(new FileOutputStream(file))) {
            kryo.writeObject(output, messages);
        } catch (Exception e) {
            log.error("Failed to save messages to file for conversation: {}", conversationId, e);
            throw new RuntimeException("Failed to save conversation: " + conversationId, e);
        }
    }

    // ========== 扩展功能方法 ==========

    /**
     * 清空所有对话数据
     */
    public void clearAll() {
        lock.writeLock().lock();
        try {
            // 清空缓存
            cache.clear();

            // 删除所有文件
            try (Stream<Path> paths = Files.walk(Paths.get(storagePath))) {
                paths.filter(Files::isRegularFile)
                      .filter(path -> path.toString().endsWith(FILE_EXTENSION))
                      .forEach(path -> {
                          try {
                              Files.delete(path);
                          } catch (IOException e) {
                              log.warn("Failed to delete file: {}", path, e);
                          }
                      });
            } catch (IOException e) {
                log.error("Failed to clear all conversations", e);
            }

            log.info("Cleared all conversation data");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        if (!enableCache) {
            return "Cache disabled";
        }

        lock.readLock().lock();
        try {
            int totalConversations = cache.size();
            int totalMessages = cache.values().stream()
                    .mapToInt(List::size)
                    .sum();

            return String.format("Cache: %d conversations, %d messages",
                    totalConversations, totalMessages);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 强制刷新缓存
     */
    public void refreshCache() {
        if (!enableCache) {
            return;
        }

        lock.writeLock().lock();
        try {
            cache.clear();
            preloadCache();
            log.info("Cache refreshed");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取存储路径
     */
    public String getStoragePath() {
        return storagePath;
    }

    /**
     * 是否启用了缓存
     */
    public boolean isCacheEnabled() {
        return enableCache;
    }
}
