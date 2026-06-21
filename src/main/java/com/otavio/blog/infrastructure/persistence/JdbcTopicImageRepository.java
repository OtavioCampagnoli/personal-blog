package com.otavio.blog.infrastructure.persistence;

import com.otavio.blog.domain.image.Image;
import com.otavio.blog.domain.topic.Topic;
import com.otavio.blog.domain.topic.TopicImage;
import com.otavio.blog.domain.topic.TopicImageRepository;
import com.otavio.blog.shared.domain.Id;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcTopicImageRepository implements TopicImageRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTopicImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public TopicImage save(TopicImage ti) {
        String sql = """
            INSERT INTO topic_images (id, topic_id, image_id, position, caption, added_at)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            ti.getId().getValue(),
            ti.getTopicId().getValue(),
            ti.getImageId().getValue(),
            ti.getPosition(),
            ti.getCaption(),
            Timestamp.from(ti.getAddedAt())
        );
        return ti;
    }

    @Override
    public List<TopicImage> findByTopicIdOrderByPosition(Id<Topic> topicId) {
        String sql = "SELECT * FROM topic_images WHERE topic_id = ? ORDER BY position ASC";
        return jdbcTemplate.query(sql, new TopicImageRowMapper(), topicId.getValue());
    }

    @Override
    public int countByTopicId(Id<Topic> topicId) {
        String sql = "SELECT COUNT(*) FROM topic_images WHERE topic_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, topicId.getValue());
        return count != null ? count : 0;
    }

    @Override
    public boolean existsByTopicIdAndImageId(Id<Topic> topicId, Id<Image> imageId) {
        String sql = "SELECT COUNT(*) FROM topic_images WHERE topic_id = ? AND image_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
            topicId.getValue(), imageId.getValue());
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public void incrementPositionsFrom(Id<Topic> topicId, int fromPosition) {
        String sql = """
            UPDATE topic_images
            SET position = position + 1
            WHERE topic_id = ? AND position >= ?
            """;
        jdbcTemplate.update(sql, topicId.getValue(), fromPosition);
    }

    @Override
    @Transactional
    public void delete(Id<TopicImage> id) {
        jdbcTemplate.update("DELETE FROM topic_images WHERE id = ?", id.getValue());
    }

    private static class TopicImageRowMapper implements RowMapper<TopicImage> {
        @Override
        public TopicImage mapRow(ResultSet rs, int rowNum) throws SQLException {
            TopicImage ti = new TopicImage(Id.of(rs.getObject("id", UUID.class)));
            ti.setTopicId(Id.of(rs.getObject("topic_id", UUID.class)));
            ti.setImageId(Id.of(rs.getObject("image_id", UUID.class)));
            ti.setPosition(rs.getInt("position"));
            ti.setCaption(rs.getString("caption"));
            ti.setAddedAt(rs.getTimestamp("added_at").toInstant());
            return ti;
        }
    }
}
