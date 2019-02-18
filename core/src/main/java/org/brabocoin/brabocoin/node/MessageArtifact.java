package org.brabocoin.brabocoin.node;

import com.google.protobuf.Message;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

public class MessageArtifact implements Comparable<MessageArtifact> {

    private final Message message;
    private final LocalDateTime time;

    public MessageArtifact(Message message) {
        this.message = message;
        this.time = LocalDateTime.now();
    }
    public LocalDateTime getTime() {
        return time;
    }

    public Message getMessage() {
        return message;
    }

    @Override
    public int compareTo(@NotNull MessageArtifact o) {
        return this.getTime().compareTo(o.getTime());
    }
}
