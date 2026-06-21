package com.otavio.blog.domain.topic;

import com.otavio.blog.domain.image.Image;
import com.otavio.blog.shared.domain.BaseEntity;
import com.otavio.blog.shared.domain.Id;
import com.otavio.blog.shared.domain.Notification;

import java.time.Instant;

public class TopicImage extends BaseEntity<TopicImage> {

    private Id<Topic> topicId;
    private Id<Image> imageId;
    private int       position;
    private String    caption;
    private Instant   addedAt;

    public TopicImage() { super(); }
    public TopicImage(Id<TopicImage> id) { super(id); }

    public static TopicImage create(Id<Topic> topicId, Id<Image> imageId,
                                    int position, String caption) {
        TopicImage ti = new TopicImage();
        ti.topicId  = topicId;
        ti.imageId  = imageId;
        ti.position = position;
        ti.caption  = caption;
        ti.addedAt  = Instant.now();
        return ti;
    }

    @Override
    public void validate(Notification notification) {
        if (position < 1) {
            notification.addError("position", "POSITION_INVALID", "Position must be >= 1");
        }
        if (caption != null && caption.length() > 300) {
            notification.addError("caption", "CAPTION_TOO_LONG",
                "Caption cannot exceed 300 characters");
        }
    }

    // Getters
    public Id<Topic> getTopicId()  { return topicId; }
    public Id<Image> getImageId()  { return imageId; }
    public int       getPosition() { return position; }
    public String    getCaption()  { return caption; }
    public Instant   getAddedAt()  { return addedAt; }

    // Setters para reconstrução do banco
    public void setTopicId(Id<Topic> topicId)   { this.topicId = topicId; }
    public void setImageId(Id<Image> imageId)   { this.imageId = imageId; }
    public void setPosition(int position)       { this.position = position; }
    public void setCaption(String caption)      { this.caption = caption; }
    public void setAddedAt(Instant addedAt)     { this.addedAt = addedAt; }
}
