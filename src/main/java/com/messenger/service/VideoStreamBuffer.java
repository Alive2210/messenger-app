package com.messenger.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * Буфер для кэширования видео потока
 * Хранит последние фреймы для воспроизведения при обрыве связи
 */
@Slf4j
@Component
public class VideoStreamBuffer {

    // Размер буфера (количество фреймов)
    private static final int BUFFER_SIZE = 60; // ~2 секунды при 30 FPS
    
    // Максимальный размер буфера для одного участника (в байтах)
    private static final long MAX_BUFFER_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB
    
    // Хранилище буферов по ID конференции и участнику
    private final Map<String, ConcurrentLinkedQueue<VideoFrame>> buffers = new ConcurrentHashMap<>();
    
    // Метаданные потоков
    private final Map<String, StreamMetadata> metadata = new ConcurrentHashMap<>();
    
    // Cleanup scheduler
    private final ScheduledExecutorService cleanupScheduler = Executors.newSingleThreadScheduledExecutor();

    public VideoStreamBuffer() {
        // Запускаем периодическую очистку старых буферов
        cleanupScheduler.scheduleAtFixedRate(this::cleanupOldBuffers, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Добавляет фрейм в буфер
     */
    public void addFrame(String conferenceId, String participantId, byte[] frameData, long timestamp) {
        String key = buildKey(conferenceId, participantId);
        
        ConcurrentLinkedQueue<VideoFrame> buffer = buffers.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());
        StreamMetadata meta = metadata.computeIfAbsent(key, k -> new StreamMetadata());
        
        // Проверяем размер буфера
        if (meta.getCurrentSize() + frameData.length > MAX_BUFFER_SIZE_BYTES) {
            // Удаляем старые фреймы
            removeOldFrames(buffer, meta, frameData.length);
        }
        
        // Добавляем новый фрейм
        VideoFrame frame = new VideoFrame();
        frame.setData(frameData);
        frame.setTimestamp(timestamp);
        frame.setSequenceNumber(meta.getNextSequenceNumber());
        
        buffer.offer(frame);
        meta.addFrame(frameData.length);
        
        // Ограничиваем количество фреймов
        while (buffer.size() > BUFFER_SIZE) {
            VideoFrame removed = buffer.poll();
            if (removed != null) {
                meta.removeFrame(removed.getData().length);
            }
        }
        
        log.trace("📹 Добавлен фрейм {} в буфер {} (размер: {})", 
                frame.getSequenceNumber(), key, buffer.size());
    }

    /**
     * Получает фреймы из буфера начиная с указанной последовательности
     */
    public byte[][] getFrames(String conferenceId, String participantId, long fromSequence) {
        String key = buildKey(conferenceId, participantId);
        ConcurrentLinkedQueue<VideoFrame> buffer = buffers.get(key);
        
        if (buffer == null || buffer.isEmpty()) {
            return new byte[0][];
        }
        
        return buffer.stream()
                .filter(f -> f.getSequenceNumber() >= fromSequence)
                .map(VideoFrame::getData)
                .toArray(byte[][]::new);
    }

    /**
     * Получает последние N фреймов для воспроизведения
     */
    public byte[][] getLastFrames(String conferenceId, String participantId, int count) {
        String key = buildKey(conferenceId, participantId);
        ConcurrentLinkedQueue<VideoFrame> buffer = buffers.get(key);
        
        if (buffer == null || buffer.isEmpty()) {
            return new byte[0][];
        }
        
        return buffer.stream()
                .skip(Math.max(0, buffer.size() - count))
                .map(VideoFrame::getData)
                .toArray(byte[][]::new);
    }

    /**
     * Воспроизводит буфер с начала (для восстановления после обрыва)
     */
    public byte[][] replayBuffer(String conferenceId, String participantId) {
        String key = buildKey(conferenceId, participantId);
        ConcurrentLinkedQueue<VideoFrame> buffer = buffers.get(key);
        
        if (buffer == null || buffer.isEmpty()) {
            log.debug("📼 Буфер пуст для {}, воспроизведение невозможно", key);
            return new byte[0][];
        }
        
        log.info("📼 Воспроизведение буфера {} ({} фреймов)", key, buffer.size());
        
        return buffer.stream()
                .map(VideoFrame::getData)
                .toArray(byte[][]::new);
    }

    /**
     * Очищает буфер для конференции
     */
    public void clearBuffer(String conferenceId, String participantId) {
        String key = buildKey(conferenceId, participantId);
        buffers.remove(key);
        metadata.remove(key);
        log.debug("🗑️ Очищен буфер {}", key);
    }

    /**
     * Очищает все буферы для конференции
     */
    public void clearConferenceBuffers(String conferenceId) {
        String prefix = conferenceId + ":";
        buffers.keySet().removeIf(key -> key.startsWith(prefix));
        metadata.keySet().removeIf(key -> key.startsWith(prefix));
        log.info("🗑️ Очищены все буферы для конференции {}", conferenceId);
    }

    /**
     * Получает статус буфера
     */
    public BufferStatus getStatus(String conferenceId, String participantId) {
        String key = buildKey(conferenceId, participantId);
        StreamMetadata meta = metadata.get(key);
        ConcurrentLinkedQueue<VideoFrame> buffer = buffers.get(key);
        
        if (meta == null || buffer == null) {
            return null;
        }
        
        BufferStatus status = new BufferStatus();
        status.setConferenceId(conferenceId);
        status.setParticipantId(participantId);
        status.setFrameCount(buffer.size());
        status.setTotalSizeBytes(meta.getCurrentSize());
        status.setLastSequenceNumber(meta.getLastSequenceNumber());
        
        return status;
    }

    /**
     * Удаляет старые фреймы для освобождения места
     */
    private void removeOldFrames(ConcurrentLinkedQueue<VideoFrame> buffer, 
                                  StreamMetadata meta, long neededSpace) {
        long freedSpace = 0;
        while (freedSpace < neededSpace && !buffer.isEmpty()) {
            VideoFrame removed = buffer.poll();
            if (removed != null) {
                freedSpace += removed.getData().length;
                meta.removeFrame(removed.getData().length);
            }
        }
    }

    /**
     * Очистка старых неиспользуемых буферов
     */
    private void cleanupOldBuffers() {
        long currentTime = System.currentTimeMillis();
        
        metadata.entrySet().removeIf(entry -> {
            boolean isOld = (currentTime - entry.getValue().getLastAccessTime()) > 300000; // 5 минут
            if (isOld) {
                buffers.remove(entry.getKey());
                log.debug("🧹 Очищен старый буфер {}", entry.getKey());
            }
            return isOld;
        });
    }

    private String buildKey(String conferenceId, String participantId) {
        return conferenceId + ":" + participantId;
    }

    /**
     * Класс для хранения видео фрейма
     */
    public static class VideoFrame {
        private byte[] data;
        private long timestamp;
        private long sequenceNumber;

        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public long getSequenceNumber() { return sequenceNumber; }
        public void setSequenceNumber(long sequenceNumber) { this.sequenceNumber = sequenceNumber; }
    }

    /**
     * Метаданные потока
     */
    private static class StreamMetadata {
        private long currentSize = 0;
        private long lastSequenceNumber = 0;
        private long lastAccessTime = System.currentTimeMillis();

        public long getNextSequenceNumber() {
            return ++lastSequenceNumber;
        }

        public void addFrame(long size) {
            currentSize += size;
            lastAccessTime = System.currentTimeMillis();
        }

        public void removeFrame(long size) {
            currentSize -= size;
            lastAccessTime = System.currentTimeMillis();
        }

        public long getCurrentSize() { return currentSize; }
        public long getLastSequenceNumber() { return lastSequenceNumber; }
        public long getLastAccessTime() { return lastAccessTime; }
    }

    /**
     * Статус буфера для API
     */
    public static class BufferStatus {
        private String conferenceId;
        private String participantId;
        private int frameCount;
        private long totalSizeBytes;
        private long lastSequenceNumber;

        public String getConferenceId() { return conferenceId; }
        public void setConferenceId(String conferenceId) { this.conferenceId = conferenceId; }
        public String getParticipantId() { return participantId; }
        public void setParticipantId(String participantId) { this.participantId = participantId; }
        public int getFrameCount() { return frameCount; }
        public void setFrameCount(int frameCount) { this.frameCount = frameCount; }
        public long getTotalSizeBytes() { return totalSizeBytes; }
        public void setTotalSizeBytes(long totalSizeBytes) { this.totalSizeBytes = totalSizeBytes; }
        public long getLastSequenceNumber() { return lastSequenceNumber; }
        public void setLastSequenceNumber(long lastSequenceNumber) { this.lastSequenceNumber = lastSequenceNumber; }
    }
}
