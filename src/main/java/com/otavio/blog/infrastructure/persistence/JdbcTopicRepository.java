package com.otavio.blog.infrastructure.persistence;

import com.otavio.blog.domain.post.Post;
import com.otavio.blog.domain.topic.Topic;
import com.otavio.blog.domain.topic.TopicRepository;
import com.otavio.blog.shared.domain.Id;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcTopicRepository implements TopicRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcTopicRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public Topic save(Topic topic) {
        if (existsById(topic.getId())) {
            return update(topic);
        }
        return insert(topic);
    }

    private Topic insert(Topic topic) {
        String sql = """
            INSERT INTO topics (id, post_id, title, content, order_number, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        jdbcTemplate.update(sql,
            topic.getId().getValue(),
            topic.getPostId().getValue(),
            topic.getTitle(),
            topic.getContent(),
            topic.getOrderNumber(),
            Timestamp.from(topic.getCreatedAt()),
            Timestamp.from(topic.getUpdatedAt())
        );

        return topic;
    }

    private Topic update(Topic topic) {
        String sql = """
            UPDATE topics
            SET title = ?, content = ?, order_number = ?, updated_at = ?
            WHERE id = ?
            """;

        jdbcTemplate.update(sql,
            topic.getTitle(),
            topic.getContent(),
            topic.getOrderNumber(),
            Timestamp.from(topic.getUpdatedAt()),
            topic.getId().getValue()
        );

        return topic;
    }

    @Override
    public Optional<Topic> findById(Id<Topic> id) {
        String sql = "SELECT * FROM topics WHERE id = ?";
        try {
            Topic topic = jdbcTemplate.queryForObject(sql, new TopicRowMapper(), id.getValue());
            return Optional.ofNullable(topic);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<Topic> findByPostIdOrderByOrderNumber(Id<Post> postId) {
        String sql = "SELECT * FROM topics WHERE post_id = ? ORDER BY order_number ASC";
        return jdbcTemplate.query(sql, new TopicRowMapper(), postId.getValue());
    }

    @Override
    public int countByPostId(Id<Post> postId) {
        String sql = "SELECT COUNT(*) FROM topics WHERE post_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, postId.getValue());
        return count != null ? count : 0;
    }

    @Override
    @Transactional
    public void incrementOrdersFrom(Id<Post> postId, int fromOrder) {
        String sql = """
            UPDATE topics
            SET order_number = order_number + 1, updated_at = CURRENT_TIMESTAMP
            WHERE post_id = ? AND order_number >= ?
            """;
        jdbcTemplate.update(sql, postId.getValue(), fromOrder);
    }

    @Override
    @Transactional
    public void delete(Id<Topic> id) {
        jdbcTemplate.update("DELETE FROM topics WHERE id = ?", id.getValue());
    }

    private boolean existsById(Id<Topic> id) {
        String sql = "SELECT COUNT(*) FROM topics WHERE id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, id.getValue());
        return count != null && count > 0;
    }

    private static class TopicRowMapper implements RowMapper<Topic> {
        @Override
        public Topic mapRow(ResultSet rs, int rowNum) throws SQLException {
            Topic topic = new Topic(Id.of(rs.getObject("id", UUID.class)));
            topic.setPostId(Id.of(rs.getObject("post_id", UUID.class)));
            topic.setTitle(rs.getString("title"));
            topic.setContent(rs.getString("content"));
            topic.setOrderNumber(rs.getInt("order_number"));
            topic.setCreatedAt(rs.getTimestamp("created_at").toInstant());
            topic.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
            return topic;
        }
    }
}
