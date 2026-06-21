package com.otavio.blog.infrastructure.persistence;

import com.otavio.blog.domain.author.Author;
import com.otavio.blog.domain.image.Image;
import com.otavio.blog.domain.image.ImageRepository;
import com.otavio.blog.shared.domain.Id;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcImageRepository implements ImageRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcImageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public Image save(Image image) {
        String sql = """
            INSERT INTO images (id, author_id, s3_key, url, alt_text, content_type,
                                size_bytes, width, height, uploaded_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        jdbcTemplate.update(sql,
            image.getId().getValue(),
            image.getAuthorId().getValue(),
            image.getS3Key(),
            image.getUrl(),
            image.getAltText(),
            image.getContentType(),
            image.getSizeBytes(),
            image.getWidth(),
            image.getHeight(),
            Timestamp.from(image.getUploadedAt())
        );
        return image;
    }

    @Override
    public Optional<Image> findById(Id<Image> id) {
        String sql = "SELECT * FROM images WHERE id = ?";
        try {
            Image image = jdbcTemplate.queryForObject(sql, new ImageRowMapper(), id.getValue());
            return Optional.ofNullable(image);
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void delete(Id<Image> id) {
        jdbcTemplate.update("DELETE FROM images WHERE id = ?", id.getValue());
    }

    private static class ImageRowMapper implements RowMapper<Image> {
        @Override
        public Image mapRow(ResultSet rs, int rowNum) throws SQLException {
            Image img = new Image(Id.of(rs.getObject("id", UUID.class)));
            img.setAuthorId(Id.of(rs.getObject("author_id", UUID.class)));
            img.setS3Key(rs.getString("s3_key"));
            img.setUrl(rs.getString("url"));
            img.setAltText(rs.getString("alt_text"));
            img.setContentType(rs.getString("content_type"));
            img.setSizeBytes(rs.getLong("size_bytes"));
            img.setWidth(rs.getInt("width"));
            img.setHeight(rs.getInt("height"));
            img.setUploadedAt(rs.getTimestamp("uploaded_at").toInstant());
            return img;
        }
    }
}
